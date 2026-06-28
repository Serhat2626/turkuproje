# FP-001 — AI Assisted Content Creation

# Engineering — Content Pipeline Implementation Plan

## Doküman Bilgisi

| Alan                           | Değer                                                                                                             |
| ------------------------------ | ----------------------------------------------------------------------------------------------------------------- |
| Doküman                        | `docs/05-engineering/20-content-pipeline-implementation-plan.md`                                                  |
| Feature                        | `FP-001 AI Assisted Content Creation`                                                                             |
| Katman                         | Engineering                                                                                                       |
| Önceki Product Dokümanı        | `docs/01-product/00-content-pipeline.md`                                                                          |
| Önceki AI Platform Dokümanları | `docs/03-ai-platform/20-content-agent-workflow.md`, `docs/03-ai-platform/21-content-agent-contract.md`            |
| Önceki Engineering Dokümanları | `docs/05-engineering/18-content-pipeline-engineering.md`, `docs/05-engineering/19-content-pipeline-api-design.md` |
| Durum                          | Draft                                                                                                             |
| Öncelik                        | High                                                                                                              |
| Sahiplik                       | Engineering + Lead Software Architect                                                                             |
| Sonraki Doküman                | `docs/05-engineering/21-content-pipeline-coding-tasks.md`                                                         |

---

# 1. Genel Bakış

Bu doküman, FP-001 AI Assisted Content Creation feature’ının kod üretimine geçmeden önceki Engineering implementation plan’ını tanımlar.

Bu dokümanın amacı, IDE AI’a rastgele ve kontrolsüz kod yazdırmak yerine feature’ı küçük, test edilebilir ve geri alınabilir parçalara bölmektir.

Bu doküman kod değildir.

Bu doküman tamamlandıktan sonra bir sonraki dokümanda IDE AI’a verilecek net coding task’lar hazırlanacaktır.

---

# 2. Implementation Stratejisi

FP-001 için implementation stratejisi:

```text id="e23g58"
Önce domain ve persistence
Sonra application service
Sonra mock AI boundary
Sonra REST API
Sonra Admin Panel
Sonra test ve review
Sonra gerçek AI provider entegrasyonu
```

İlk hedef, gerçek AI kalitesinden önce uçtan uca çalışan workflow’dur.

Yani MVP önceliği:

```text id="9s9ic8"
Knowledge Item oluştur
  → Content Project oluştur
  → Mock AI draft üret
  → Draft review et
  → Ready For Publish yap
```

Gerçek provider entegrasyonu ilk kod sprintinin zorunlu parçası değildir.

---

# 3. Temel Engineering Kararları

## 3.1 Mock AI Gateway ile Başlama

İlk implementation gerçek AI provider ile başlamamalıdır.

Neden:

* Workflow önce oturmalı
* Status geçişleri test edilmeli
* Admin Panel uçtan uca çalışmalı
* Provider kaynaklı timeout / auth / rate limit gibi problemler MVP akışını yavaşlatmamalı
* Contract doğru mu önce görülmeli

İlk fazda `MockAiContentAgentGateway` kullanılmalıdır.

Mock gateway:

* Gelen request’e göre deterministic draft üretir
* Timeout simülasyonu yapılabilir
* Empty response simülasyonu yapılabilir
* Invalid response simülasyonu yapılabilir
* Retry testlerini kolaylaştırır

---

## 3.2 Status Transition Öncelikli Geliştirme

Bu feature’ın en kritik noktası AI çağrısı değil, doğru workflow yönetimidir.

Bu nedenle önce şu kurallar kodda garanti altına alınmalıdır:

* `READY_FOR_PUBLISH` sadece `APPROVED` sonrası olabilir.
* `APPROVED` sadece human review sonrası olabilir.
* `FAILED` durumundan retry yapılabilir.
* `REVISION_REQUESTED` teknik retry değildir.
* `REJECTED` sonrası otomatik retry yapılmaz.
* `ARCHIVED` Knowledge Item’dan project oluşturulmaz.

---

## 3.3 Controller İnce, Application Service Güçlü

Controller’lar yalnızca:

* Request almalı
* Application service çağırmalı
* Response döndürmeli

Controller’lar:

* Status transition kararı vermemeli
* Retry limiti kontrol etmemeli
* AI validation yapmamalı
* Business rule içermemeli

Business rule application layer’da olmalıdır.

---

# 4. Implementation Fazları

## Faz 1 — Domain Enum ve Temel Model

Amaç:

Product ve Engineering dokümanlarında belirlenen statü ve tipleri kod seviyesine taşımak.

Kapsam:

* `KnowledgeItemStatus`
* `ContentProjectStatus`
* `ContentDraftStatus`
* `ContentPurpose`
* `TargetAudience`
* `ContentTone`
* `AiGenerationStatus`
* `AiErrorType`
* `GeneratedBy`

Başarı kriteri:

* Enum’lar oluşturulmuş olmalı.
* İsimler dokümanlarla uyumlu olmalı.
* MVP dışı enum değerleri şimdiden eklenebilir ancak kullanılmayan değerler business flow’u bozmaz.

---

## Faz 2 — Entity ve Persistence

Amaç:

MVP workflow için gerekli verileri saklayacak entity’leri oluşturmak.

Kapsam:

* `KnowledgeItem`
* `ContentProject`
* `ContentDraft`
* `AiGenerationRequest`

Minimum persistence alanları önceki Engineering dokümanına uygun olmalıdır.

Başarı kriteri:

* Entity’ler oluşturulmuş olmalı.
* Migration dosyaları hazırlanmış olmalı.
* Repository’ler oluşturulmuş olmalı.
* İlişkiler sade tutulmalı.
* İlk fazda gereksiz complex mapping yapılmamalı.

---

## Faz 3 — Application Service: Knowledge Item

Amaç:

Knowledge Item yönetimini backend application layer’a almak.

Kapsam:

* Create
* Update
* List
* Detail
* Mark Ready
* Archive

Business rules:

* Title zorunlu
* İlk statü `DRAFT`
* `DRAFT → READY`
* `DRAFT/READY → ARCHIVED`
* `ARCHIVED → READY` yasak

Başarı kriteri:

* Knowledge Item lifecycle çalışmalı.
* Geçersiz statü geçişleri engellenmeli.
* Unit test veya application service test yazılmalı.

---

## Faz 4 — Application Service: Content Project

Amaç:

Content Project oluşturma ve workflow status yönetimini kurmak.

Kapsam:

* Create Content Project
* Get detail
* List opsiyonel
* Start review
* Approve
* Request revision
* Reject
* Mark ready for publish
* Cancel

Business rules:

* Project sadece `READY` Knowledge Item’dan oluşturulur.
* Archived item’dan project oluşturulmaz.
* İlk project statüsü `CREATED`
* Approve sadece `IN_REVIEW`
* Ready for publish sadece `APPROVED`
* Reject sadece review sürecinde
* Revision insan notu gerektirir

Başarı kriteri:

* AI entegrasyonu olmadan project lifecycle test edilebilir olmalı.
* Status transition’lar application service içinde kontrol edilmeli.

---

## Faz 5 — AI Platform Boundary ve Mock Gateway

Amaç:

Gerçek AI provider’a bağlanmadan AI generation workflow’unu kurmak.

Kapsam:

* `ContentAgentRequest` kod karşılığı
* `ContentAgentResponse` kod karşılığı
* `ContentAgentError` kod karşılığı
* `ContentAgentValidationResult` kod karşılığı
* `AiContentAgentGateway` interface
* `MockAiContentAgentGateway`

Mock gateway davranışları:

* Başarılı draft response
* Timeout hata response
* Empty response
* Too short response
* Invalid format simülasyonu

Başarı kriteri:

* Application service AI gateway interface üzerinden çalışmalı.
* Domain/application gerçek provider SDK bilmemeli.
* Mock gateway ile success/failure testleri yapılabilmeli.

---

## Faz 6 — AI Generation Application Service

Amaç:

Content Project üzerinden AI draft generation ve retry workflow’unu yönetmek.

Kapsam:

* Generate AI draft
* Retry AI draft
* Revision sonrası generate
* AI response validation
* Draft conversion
* AiGenerationRequest kayıt yönetimi
* Retry count yönetimi
* AI error handling

Business rules:

* Generate sadece `CREATED` veya `REVISION_REQUESTED` için
* Retry sadece `FAILED` için
* Retry max `3`
* Non-retryable hata retry edilemez
* Validation başarısızsa draft oluşmaz
* Başarılı response sonrası draft oluşur
* Project `DRAFT_READY` olur

Başarı kriteri:

* Mock gateway ile uçtan uca draft üretimi çalışmalı.
* AI hatasında project `FAILED` olmalı.
* Retry başarılıysa yeni draft oluşmalı.
* Retry limit aşımı engellenmeli.

---

## Faz 7 — REST API

Amaç:

Admin Panel’in kullanacağı endpoint’leri açmak.

Kapsam:

* Knowledge Item endpoints
* Content Project endpoints
* AI generation action endpoint
* Retry endpoint
* Review action endpoints
* Content Draft detail/update endpoints

Başarı kriteri:

* Endpoint’ler `docs/05-engineering/19-content-pipeline-api-design.md` ile uyumlu olmalı.
* Request DTO’larda status set alanı olmamalı.
* Action endpoint’ler application service’e gitmeli.
* Error response standardı uygulanmalı.

---

## Faz 8 — Admin Panel Minimum Flow

Amaç:

Admin Panel’den uçtan uca MVP akışı çalıştırmak.

Minimum ekranlar:

* Knowledge Item List
* Knowledge Item Detail / Form
* Content Project Detail

Opsiyonel ekran:

* Content Project List

Minimum aksiyonlar:

* Knowledge Item oluştur
* Mark ready
* Content Project oluştur
* Generate AI draft
* Draft görüntüle
* Start review
* Approve
* Request revision
* Reject
* Retry
* Ready for publish

Başarı kriteri:

* Admin Panel’den happy path tamamlanmalı.
* Hata ve retry durumları görülebilmeli.
* Status değişimleri UI’da anlaşılır olmalı.

---

## Faz 9 — Test ve Stabilizasyon

Amaç:

Feature’ın uçtan uca sağlam çalıştığını doğrulamak.

Test kapsamı:

* Unit tests
* Application service tests
* Mock AI gateway tests
* API tests
* Manual Admin Panel tests

Başarı kriteri:

* Happy path çalışmalı.
* Error path çalışmalı.
* Retry path çalışmalı.
* Revision path çalışmalı.
* Human approval olmadan ready for publish yapılamamalı.

---

## Faz 10 — Gerçek AI Provider Hazırlığı

Amaç:

Mock gateway sonrası gerçek provider entegrasyonuna hazırlanmak.

Bu faz ilk MVP için zorunlu değildir.

Kapsam:

* Provider adapter implementation
* Prompt template
* Timeout config
* Rate limit handling
* Provider error normalization
* Token/cost metadata
* Prompt/response logging kararları

Başarı kriteri:

* Gerçek provider geçişi mock gateway’i bozmadan yapılabilmeli.
* Gateway interface korunmalı.
* Provider seçimi application/domain katmanına sızmamalı.

---

# 5. Önerilen Package / Dosya Yönü

Bu doküman kesin package yapısı dayatmaz.

Ancak önerilen yön:

```text id="qkhxy5"
content
  ├── domain
  │   ├── model
  │   ├── enums
  │   └── repository
  ├── application
  │   ├── knowledgeitem
  │   ├── project
  │   ├── draft
  │   └── ai
  ├── infrastructure
  │   └── ai
  └── api
      ├── request
      ├── response
      └── controller
```

Eğer mevcut proje package standardı farklıysa, mevcut standarda uyulmalıdır.

Yeni feature mevcut mimariyi bozacak şekilde ayrı bir dünya kurmamalıdır.

---

# 6. Migration Sırası

Migration sırası önerisi:

```text id="a9658h"
1. knowledge_items
2. content_projects
3. content_drafts
4. ai_generation_requests
```

## 6.1 knowledge_items

Minimum kolonlar:

```text id="v0a4q3"
id
title
description
category
city
district
location_text
notes
status
created_at
updated_at
```

## 6.2 content_projects

Minimum kolonlar:

```text id="lfn8cz"
id
knowledge_item_id
content_purpose
target_audience
tone
language
format_hint
length_hint
status
retry_count
last_error_type
last_error_message
created_at
updated_at
```

## 6.3 content_drafts

Minimum kolonlar:

```text id="ruqt4j"
id
content_project_id
title
body
summary
status
generated_by
warnings_json
created_at
updated_at
```

## 6.4 ai_generation_requests

Minimum kolonlar:

```text id="rkg3fe"
id
content_project_id
attempt_number
status
request_payload_json
response_payload_json
error_type
error_message
provider_name
model_name
duration_ms
created_at
completed_at
```

---

# 7. DTO / Contract Sırası

Kod aşamasında DTO’lar şu sırayla hazırlanmalıdır:

## 7.1 API Request DTO’ları

```text id="5hl13r"
CreateKnowledgeItemRequest
UpdateKnowledgeItemRequest
CreateContentProjectRequest
GenerateAiDraftRequest
RetryAiDraftRequest
ApproveDraftRequest
RequestRevisionRequest
RejectDraftRequest
CancelContentProjectRequest
UpdateContentDraftRequest
```

## 7.2 API Response DTO’ları

```text id="0a2d2s"
KnowledgeItemResponse
KnowledgeItemSummaryResponse
ContentProjectResponse
ContentProjectSummaryResponse
ContentDraftResponse
AiGenerationRequestSummaryResponse
ActionStatusResponse
ErrorResponse
```

## 7.3 AI Contract DTO’ları

```text id="euxltw"
ContentAgentRequest
ContentAgentKnowledgeItemPayload
ContentAgentResponse
ContentAgentError
ContentAgentValidationResult
HumanFeedbackPayload
PreviousDraftPayload
ContentAgentRetryMetadata
DraftConversionPayload
ProviderExecutionMetadata
```

Not:

* API DTO ve AI contract DTO aynı şey değildir.
* Admin API response’ları provider contract’ına bağımlı olmamalıdır.
* AI contract DTO’ları internal boundary için kullanılmalıdır.

---

# 8. Mock AI Gateway Stratejisi

Mock gateway deterministic olmalıdır.

Aynı request aynı türde response üretmelidir.

## 8.1 Başarılı Response

Örnek davranış:

```text id="jxgb5g"
knowledgeItem.title = "Ankara Kalesi"
contentPurpose = INFORMATIONAL_TEXT

→ "Ankara Kalesi hakkında bilgilendirici mock içerik..."
```

## 8.2 Hata Simülasyonu

MVP testleri için mock gateway şu hata modlarını destekleyebilir:

```text id="jpk577"
SUCCESS
TIMEOUT
EMPTY_RESPONSE
TOO_SHORT_RESPONSE
INVALID_FORMAT
UNSAFE_CONTENT
```

Bu hata modu test config veya request içindeki test-only instruction ile tetiklenebilir.

Production davranışında test-only instruction kullanılmamalıdır.

---

# 9. Validation Planı

AI response validation ilk fazda sade olmalıdır.

MVP validation kuralları:

```text id="qf9ku2"
TITLE_REQUIRED
BODY_REQUIRED
BODY_MIN_LENGTH_100
RESPONSE_PARSEABLE
NO_PROVIDER_ERROR
```

Validation başarısızsa:

* Draft oluşturulmaz
* AiGenerationRequest `FAILED` olur
* ContentProject `FAILED` olur
* Error type kaydedilir
* Retry mümkünse retry endpoint aktif olur

Validation service ayrı bir küçük component olarak yazılabilir:

```text id="cu2zmb"
ContentAgentResponseValidator
```

---

# 10. Error Handling Planı

## 10.1 Business Exception

Business rule ihlalleri için ortak exception yaklaşımı kullanılmalıdır.

Örnek kodlar:

```text id="3rm0ll"
KNOWLEDGE_ITEM_NOT_FOUND
KNOWLEDGE_ITEM_NOT_READY
KNOWLEDGE_ITEM_ARCHIVED
CONTENT_PROJECT_NOT_FOUND
CONTENT_DRAFT_NOT_FOUND
INVALID_PROJECT_STATUS
INVALID_DRAFT_STATUS
RETRY_LIMIT_EXCEEDED
NON_RETRYABLE_AI_ERROR
APPROVED_DRAFT_REQUIRED
VALIDATION_ERROR
```

## 10.2 AI Exception / Error Result

AI tarafında exception fırlatmak yerine internal result modeli tercih edilebilir.

Örnek conceptual sonuç:

```text id="x9o4fm"
Success(ContentAgentResponse)
Failure(ContentAgentError)
```

Engineering implementation’da bu yaklaşım mevcut proje standardına göre uyarlanabilir.

## 10.3 Global Error Handler

API error response standardı merkezi handler ile sağlanmalıdır.

Amaç:

* Tutarlı error response
* Stack trace sızmasını engelleme
* Kullanıcı dostu mesaj
* HTTP status mapping

---

# 11. Admin Panel Implementation Sırası

## 11.1 Knowledge Item

Önce:

* Liste
* Oluşturma formu
* Detay
* Mark ready
* Archive

## 11.2 Content Project

Sonra:

* Knowledge Item detayından project oluşturma
* Project detay ekranı
* Status gösterimi

## 11.3 AI Draft

Sonra:

* Generate AI draft butonu
* Loading durumu
* Draft görüntüleme
* Hata gösterimi
* Retry butonu

## 11.4 Review

Sonra:

* Start review
* Approve
* Request revision
* Reject
* Review note input
* Ready for publish

## 11.5 UX Notları

* Kullanıcı doğrudan status seçmemeli.
* Sadece mevcut status’a uygun action butonları görünmeli.
* Hata varsa retryable olup olmadığı gösterilmeli.
* AI warning’leri editöre görünür olmalı.
* Draft final içerik gibi sunulmamalı.

---

# 12. Kod Üretim Parçalama Stratejisi

IDE AI’a tüm feature tek seferde yazdırılmamalıdır.

Kod üretimi küçük task’lara bölünmelidir.

Önerilen task grupları:

```text id="lqu8za"
Task 1: Enum definitions
Task 2: Entities and migrations
Task 3: Repositories
Task 4: KnowledgeItemApplicationService
Task 5: Knowledge Item API
Task 6: ContentProjectApplicationService
Task 7: AI contract DTOs and gateway interface
Task 8: Mock AI gateway
Task 9: AI generation service
Task 10: Content Project API actions
Task 11: Content Draft API
Task 12: Tests
Task 13: Admin Panel Knowledge Item screens
Task 14: Admin Panel Project screens
Task 15: Admin Panel AI draft/review actions
```

Her task sonunda:

* Kod compile olmalı.
* Test varsa çalışmalı.
* Scope dışına çıkılmamalı.
* Review checklist uygulanmalı.

---

# 13. Kod Başlama Kriterleri

Kod üretimine başlamadan önce şu kriterler sağlanmış olmalıdır:

* Product dokümanı tamamlandı.
* AI Platform workflow dokümanı tamamlandı.
* AI Platform contract dokümanı tamamlandı.
* Engineering design dokümanı tamamlandı.
* API design dokümanı tamamlandı.
* Implementation plan tamamlandı.
* Package ve migration yaklaşımı net.
* Mock AI gateway ile başlama kararı net.
* MVP kapsamı net.
* Gerçek AI provider entegrasyonunun ilk fazda opsiyonel olduğu kabul edildi.

---

# 14. İlk Kod Sprint Hedefi

İlk kod sprint sonunda hedef:

```text id="3q6d42"
Backend tarafında:
Knowledge Item oluştur
Knowledge Item ready yap
Content Project oluştur
Mock AI draft üret
Draft görüntüle
Review approve yap
Ready for publish yap
```

Admin Panel tarafı bu sprintin sonunda yetişmezse sorun değildir.

Öncelik backend workflow’dur.

---

# 15. İkinci Kod Sprint Hedefi

İkinci kod sprint hedefi:

```text id="gy88o5"
Admin Panel üzerinden:
Knowledge Item oluştur
Project oluştur
AI draft üret
Draft review et
Ready for publish yap
```

Ayrıca:

* Hata gösterimi
* Retry butonu
* Revision note input
* Draft update

eklenebilir.

---

# 16. Üçüncü Kod Sprint Hedefi

Üçüncü kod sprint hedefi:

```text id="0hcmsq"
Gerçek AI provider adapter hazırlığı
Prompt template
Provider error normalization
Config üzerinden mock/real provider seçimi
AI response kalitesini iyileştirme
```

Bu sprintte halen insan review zorunluluğu korunmalıdır.

---

# 17. Definition of Done

FP-001 Engineering implementation tamamlanmış sayılması için:

* Backend happy path çalışıyor.
* Backend error path çalışıyor.
* Backend retry path çalışıyor.
* Backend revision path çalışıyor.
* Admin Panel happy path çalışıyor.
* AI response draft olarak saklanıyor.
* İnsan approval olmadan ready for publish yapılamıyor.
* Retry max 3 ile sınırlı.
* Archived Knowledge Item’dan project oluşturulamıyor.
* Mock AI gateway ile test yapılabiliyor.
* Kod review checklist geçildi.
* Dokümanlarla çelişen scope eklenmedi.

---

# 18. Scope Guardrails

Kod üretimi sırasında aşağıdakiler yapılmamalıdır:

* Public content publishing
* Mobile API
* Multi-language production workflow
* Image generation
* Video generation
* Storyboard generation
* Audio file generation
* Fact-checking agent
* Research agent
* Prompt management UI
* Provider cost dashboard
* Full audit log
* Complex versioning
* Autonomous publishing

Bu başlıklar gelecek feature sprintlerin konusudur.

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 AI Assisted Content Creation feature’ının kod üretimine kontrollü şekilde hazırlanmasını sağlamaktır.

Bu doküman ile:

* Implementation fazları belirlenir.
* Mock AI gateway ile başlama kararı netleştirilir.
* Backend implementation sırası tanımlanır.
* Admin Panel implementation sırası tanımlanır.
* Migration sırası belirlenir.
* DTO ve contract oluşturma sırası belirlenir.
* Test stratejisi implementation plan’a bağlanır.
* IDE AI’a küçük task’lar halinde kod yazdırma zemini hazırlanır.

---

# 👤 Aktörler

## Admin

Implementation sonunda Admin:

* Knowledge Item oluşturabilir.
* Knowledge Item’ı ready yapabilir.
* Content Project oluşturabilir.
* AI draft generation başlatabilir.
* Retry yapabilir.
* Draft review sürecini ilerletebilir.
* Ready for publish kararı verebilir.

## Editor

Implementation sonunda Editor:

* Draft görüntüleyebilir.
* Draft düzenleyebilir.
* Revizyon isteyebilir.
* Onaylayabilir.
* Reddedebilir.

## Backend Developer

Implementation plan’ı kod task’larına böler ve IDE AI çıktısını kontrol eder.

## IDE AI

Belirlenen task sınırları içinde kod üretir.

IDE AI:

* Tek seferde tüm feature’ı yazmaz.
* Dokümanda belirtilmeyen kapsamı eklemez.
* Mevcut mimari yapıyı bozmadan ilerler.

## Lead Software Architect

Doküman uyumunu, scope kontrolünü ve kod review kriterlerini takip eder.

---

# 📄 İş Akışı

## Implementation İş Akışı

1. Enum’lar oluşturulur.
2. Entity ve migration dosyaları oluşturulur.
3. Repository’ler eklenir.
4. Knowledge Item application service yazılır.
5. Knowledge Item API açılır.
6. Content Project application service yazılır.
7. AI contract DTO’ları hazırlanır.
8. Gateway interface tanımlanır.
9. Mock AI gateway yazılır.
10. AI generation service yazılır.
11. Content Project action API’leri açılır.
12. Content Draft API’leri açılır.
13. Backend testleri yazılır.
14. Admin Panel minimum ekranları eklenir.
15. Uçtan uca happy path test edilir.
16. Error, retry ve revision path test edilir.
17. Gerçek AI provider entegrasyonu sonraki sprintte değerlendirilir.

---

# 🤖 AI Agent Görevleri

Implementation plan kapsamında AI Content Agent’ın görevi:

* İlk fazda mock gateway ile temsil edilmek
* Contract’a uygun response üretmek
* Başarı ve hata senaryolarını test edilebilir yapmak
* Gerçek provider gelene kadar workflow’u doğrulamak

Gerçek AI agent ilk sprintte zorunlu değildir.

Mock agent’ın amacı:

* Workflow’u test etmek
* Status transition’ları doğrulamak
* Retry davranışını test etmek
* Admin Panel entegrasyonunu mümkün kılmak

---

# 👨 İnsan Müdahalesi

Implementation plan’da insan müdahalesi şu noktalarda korunmalıdır:

* Knowledge Item ready kararı
* Draft review başlatma
* Draft onaylama
* Revizyon isteme
* Draft reddetme
* Ready for publish kararı
* Retry limitinden sonra manuel değerlendirme

Kod hiçbir şekilde:

```text id="qtrhau"
AI draft üretildi → otomatik approved → otomatik ready for publish
```

akışını yapmamalıdır.

---

# 📊 Status Geçişleri

Implementation sırasında korunması gereken ana geçişler:

## Knowledge Item

```text id="sqzd4l"
DRAFT → READY
DRAFT → ARCHIVED
READY → ARCHIVED
```

## Content Project

```text id="zton6j"
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Alternatif:

```text id="n4oz92"
AI_PROCESSING → FAILED
FAILED → AI_REQUESTED
IN_REVIEW → REVISION_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
IN_REVIEW → REJECTED
CREATED → CANCELLED
```

## Content Draft

```text id="43lhp9"
GENERATED → EDITED
GENERATED → APPROVED
GENERATED → REVISION_NEEDED
GENERATED → REJECTED
EDITED → APPROVED
EDITED → REVISION_NEEDED
EDITED → REJECTED
```

---

# ⚠ Hata Senaryoları

Implementation sırasında özellikle test edilmesi gereken hata senaryoları:

## Knowledge Item Ready Değil

* Project oluşturma engellenmeli.
* Hata: `KNOWLEDGE_ITEM_NOT_READY`

## Archived Knowledge Item

* Project oluşturma engellenmeli.
* Hata: `KNOWLEDGE_ITEM_ARCHIVED`

## Invalid Project Status

* Geçersiz action engellenmeli.
* Hata: `INVALID_PROJECT_STATUS`

## AI Timeout

* Mock gateway timeout simüle etmeli.
* Project `FAILED` olmalı.
* Retry yapılabilir olmalı.

## Empty AI Response

* Draft oluşmamalı.
* Project `FAILED` olmalı.

## Retry Limit Exceeded

* 3 denemeden sonra retry engellenmeli.
* Manuel müdahale mesajı dönmeli.

## Human Rejection

* Project `REJECTED` olmalı.
* Otomatik retry yapılmamalı.

---

# 🔄 Retry Kuralları

Kod aşamasında retry için:

* Retry endpoint ayrı olacak.
* Retry sadece `FAILED` durumunda çalışacak.
* Retry count backend’de tutulacak.
* Maksimum retry sayısı `3`.
* Her retry yeni `AiGenerationRequest` oluşturacak.
* Non-retryable hata retry edilmeyecek.
* Retry başarılıysa draft oluşacak.
* Retry başarısızsa project `FAILED` kalacak.
* Revizyon retry sayılmayacak.

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* Kod üretim fazları
* Backend implementation sırası
* Migration sırası
* DTO / contract sırası
* Mock AI gateway stratejisi
* Admin Panel implementation sırası
* Test ve stabilizasyon planı
* Kod başlama kriterleri
* İlk, ikinci ve üçüncü kod sprint hedefleri
* Definition of Done
* Scope guardrails

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI yine kod yazmayacaktır.

IDE AI’ın görevi:

1. `docs/05-engineering/20-content-pipeline-implementation-plan.md` dosyasını oluşturmak.
2. Bu dokümanı verilen içerikle doldurmak.
3. Mevcut `docs/05-engineering` dosyalarını yeniden numaralandırmamak.
4. Kod üretmemek.
5. Entity, DTO, controller, service, repository, migration veya frontend page oluşturmamak.
6. Kod yazımına ancak sonraki coding tasks dokümanı tamamlandıktan sonra geçmek.
7. Implementation fazlarını değiştirmemek.
8. Mock AI gateway ile başlama kararını korumak.

IDE AI’a verilecek net komut:

```text id="vjlxe2"
Create the file docs/05-engineering/20-content-pipeline-implementation-plan.md with the provided content.
Do not renumber existing docs/05-engineering files.
Do not generate code.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API endpoints yet.
Only add this Engineering implementation plan documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Implementation fazları tanımlandı.
* Mock AI gateway ile başlama kararı netleştirildi.
* Domain, persistence, service, API, admin panel ve test sırası belirlendi.
* Migration sırası belirlendi.
* DTO ve contract oluşturma sırası belirlendi.
* Retry ve revision implementation ayrımı netleştirildi.
* Admin Panel implementation sırası tanımlandı.
* İlk üç kod sprint hedefi belirlendi.
* Kod başlama kriterleri tanımlandı.
* Definition of Done belirlendi.
* Scope guardrails yazıldı.
* Kod üretimine geçilmedi.
* Sonraki coding tasks dokümanı belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Kod aşamasında bu implementation plan’a göre şu kontroller yapılmalıdır:

* Kod belirlenen faz sırasına uygun mu?
* Mock AI gateway ile başlanmış mı?
* Gerçek provider SDK domain/application katmanına sızmamış mı?
* Status transition’lar application service içinde mi?
* Controller’lar business rule içermiyor mu?
* Retry ve revision ayrı mı?
* Retry max 3 uygulanıyor mu?
* Her retry yeni AI request oluşturuyor mu?
* AI validation başarısızsa draft oluşturulmuyor mu?
* İnsan approval olmadan ready for publish yapılamıyor mu?
* Archived Knowledge Item’dan project oluşturulamıyor mu?
* API DTO ve AI contract DTO ayrımı korunmuş mu?
* Admin Panel doğrudan status set etmiyor mu?
* Scope dışı feature eklenmemiş mi?
* Testler happy path, failure path, retry path ve revision path’i kapsıyor mu?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text id="rxrmep"
docs/05-engineering/21-content-pipeline-coding-tasks.md
```

Bu dokümanda artık IDE AI’a verilecek somut coding task’lar hazırlanacaktır.

Ele alınacak konular:

* Task 1: Enum definitions
* Task 2: Entity and migration
* Task 3: Repository
* Task 4: Knowledge Item service
* Task 5: Knowledge Item API
* Task 6: Content Project service
* Task 7: AI contract DTOs
* Task 8: Mock AI gateway
* Task 9: AI generation service
* Task 10: Content Project API actions
* Task 11: Content Draft API
* Task 12: Backend tests
* Task 13: Admin Panel screens
* Task 14: End-to-end manual test checklist

Bu doküman tamamlandıktan sonra kontrollü şekilde ilk kod task’ına başlanabilir.
