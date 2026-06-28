# FP-001 — AI Assisted Content Creation

# Engineering — Content Pipeline Engineering Design

## Doküman Bilgisi

| Alan                           | Değer                                                                                                  |
| ------------------------------ |--------------------------------------------------------------------------------------------------------|
| Doküman                        | `docs/05-engineering/18-content-pipeline-engineering.md`                                               |
| Feature                        | `FP-001 AI Assisted Content Creation`                                                                  |
| Katman                         | Engineering                                                                                            |
| Önceki Product Dokümanı        | `docs/01-product/00-content-pipeline.md`                                                               |
| Önceki AI Platform Dokümanları | `docs/03-ai-platform/20-content-agent-workflow.md`, `docs/03-ai-platform/21-content-agent-contract.md` |
| Durum                          | Draft                                                                                                  |
| Öncelik                        | High                                                                                                   |
| Sahiplik                       | Engineering + Lead Software Architect                                                                  |
| Sonraki Doküman                | `docs/05-engineering/19-content-pipeline-api-design.md`                                                |

---

# 1. Genel Bakış

Bu doküman, FP-001 AI Assisted Content Creation feature’ının Engineering seviyesindeki ilk tasarım dokümanıdır.

Product ve AI Platform dokümanlarında tanımlanan akış:

```text
Knowledge Item
  → Content Project
  → AI Generated Content Draft
  → Human Review
  → Ready For Publish Content
```

Bu Engineering dokümanı, bu akışın backend ve admin panel tarafında nasıl modülleneceğini, hangi domain kavramlarının oluşacağını, hangi application service sorumluluklarının bulunacağını, status geçişlerinin nasıl kontrol edileceğini ve AI Platform entegrasyon sınırının nerede duracağını tanımlar.

Bu dokümanda hâlâ kod yazılmaz.

Bu doküman tamamlandıktan sonra bir sonraki Engineering dokümanında API tasarımı netleştirilecektir.

---

# 2. Engineering Vizyonu

Anadolu Atlası uzun vadede AI destekli, yarı otonom bir kültürel içerik üretim platformu olacaktır.

Bu nedenle Engineering tasarımı yalnızca “bir endpoint AI çağırıyor ve metin dönüyor” şeklinde yapılmamalıdır.

Doğru Engineering yaklaşımı:

* Product kavramlarını domain model içinde korumak
* AI çıktısını final içerikten ayırmak
* İnsan review adımını zorunlu business rule olarak tanımlamak
* Status transition’ları kontrolsüz bırakmamak
* AI provider detaylarını domain ve application layer’dan izole etmek
* Retry ve hata yönetimini izlenebilir yapmak
* İleride multi-agent, source validation, storyboard, image ve audio üretimine genişleyebilecek temel kurmak

Bu feature, ilerideki tüm içerik üretim zincirinin çekirdeği olarak ele alınmalıdır.

---

# 3. Engineering Scope

## 3.1 Bu Doküman Kapsamında Olanlar

Bu dokümanda aşağıdaki konular ele alınır:

* Backend modül sınırları
* Domain model taslağı
* Entity adayları
* Enum adayları
* Application service sorumlulukları
* AI Platform integration boundary
* Status transition yönetimi
* Error handling yaklaşımı
* Retry mekanizması
* Admin Panel ekran akışı
* Persistence yaklaşımı
* Geliştirme sırası
* Kod üretimine hazırlık

---

## 3.2 Bu Doküman Kapsamında Olmayanlar

Bu dokümanda aşağıdakiler yapılmaz:

* Java kodu yazımı
* Entity class oluşturma
* DTO class oluşturma
* Controller yazımı
* Repository yazımı
* Migration dosyası yazımı
* Frontend component yazımı
* API endpoint detaylarının finalize edilmesi
* Prompt implementation
* AI provider seçimi
* Authentication / authorization detay implementasyonu
* Public kullanıcıya yayınlama

Bunlar sonraki Engineering ve kod aşamalarında yapılacaktır.

---

# 4. Ana Backend Modülleri

FP-001 için önerilen backend modül ayrımı aşağıdaki gibidir:

```text
content
  ├── knowledge-item
  ├── content-project
  ├── content-draft
  ├── review
  └── ai-integration
```

Bu ayrım fiziksel package yapısı olmak zorunda değildir.

Engineering açısından önemli olan sorumlulukların karışmamasıdır.

---

# 5. Domain Kavramları

## 5.1 Knowledge Item

Kültürel bilginin ham ve yönetilebilir birimidir.

Engineering karşılığı olarak ayrı bir domain entity olmalıdır.

Sorumlulukları:

* Kültürel konu başlığını taşımak
* Açıklama, kategori ve lokasyon bilgisini tutmak
* Üretime hazır olup olmadığını belirtmek
* Content Project oluşturma için kaynak olmak

Knowledge Item final içerik değildir.

---

## 5.2 Content Project

Bir Knowledge Item üzerinden belirli bir amaçla içerik üretim sürecidir.

Sorumlulukları:

* Hangi Knowledge Item’dan üretildiğini bilmek
* İçerik amacını tutmak
* Hedef kitleyi tutmak
* Ton bilgisini tutmak
* AI üretim ve review sürecinin ana statüsünü taşımak
* Retry sayısını ve hata durumunu izlemek
* Draft’larla ilişki kurmak

Content Project, FP-001’in ana workflow aggregate adayıdır.

---

## 5.3 Content Draft

AI veya insan tarafından oluşturulmuş içerik taslağıdır.

Sorumlulukları:

* Başlık, gövde, özet ve uyarıları tutmak
* Hangi Content Project’e bağlı olduğunu bilmek
* Üretici bilgisini tutmak
* Draft statüsünü taşımak
* İnsan review sonucuna göre güncellenmek

Content Draft final içerik değildir.

---

## 5.4 Review Note

İnsan editörün draft hakkında verdiği kararı ve notu temsil eder.

İlk MVP’de ayrı entity olmak zorunda değildir.

Ancak uzun vadede review geçmişi için ayrı model olarak düşünülmelidir.

MVP’de Content Project veya Content Draft üzerinde son review notu tutulabilir.

---

## 5.5 AI Request Record

AI agent çağrısının izlenebilir kaydıdır.

İlk MVP’de ayrı entity olarak tutulması önerilir.

Sorumlulukları:

* Request ID
* Project referansı
* Attempt number
* AI request durumu
* Hata tipi
* Hata mesajı
* Provider metadata
* Oluşturulma zamanı
* Tamamlanma zamanı

AI Request Record sayesinde retry, debugging ve gelecekte analytics mümkün olur.

---

# 6. Entity Adayları

İlk Engineering tasarımında aşağıdaki entity adayları vardır:

```text
KnowledgeItem
ContentProject
ContentDraft
AiGenerationRequest
```

Opsiyonel / sonraki faz entity adayları:

```text
ContentReview
ContentVersion
PublishedContent
AiProviderExecution
AiValidationResult
```

MVP için önerilen minimum set:

```text
KnowledgeItem
ContentProject
ContentDraft
AiGenerationRequest
```

---

# 7. Enum Adayları

## 7.1 KnowledgeItemStatus

```text
DRAFT
READY
ARCHIVED
```

## 7.2 ContentProjectStatus

```text
CREATED
AI_REQUESTED
AI_PROCESSING
DRAFT_READY
IN_REVIEW
REVISION_REQUESTED
APPROVED
REJECTED
READY_FOR_PUBLISH
FAILED
CANCELLED
```

## 7.3 ContentDraftStatus

```text
GENERATED
EDITED
REVISION_NEEDED
APPROVED
REJECTED
```

## 7.4 ContentPurpose

```text
INFORMATIONAL_TEXT
CHILD_FRIENDLY_TEXT
TOURISTIC_GUIDE_TEXT
SOCIAL_MEDIA_POST
MOBILE_APP_CARD
AUDIO_GUIDE_SCRIPT
```

MVP minimum:

```text
INFORMATIONAL_TEXT
CHILD_FRIENDLY_TEXT
MOBILE_APP_CARD
```

## 7.5 TargetAudience

```text
GENERAL_USER
CHILD
TOURIST
STUDENT
RESEARCHER
MOBILE_APP_USER
```

MVP minimum:

```text
GENERAL_USER
CHILD
MOBILE_APP_USER
```

## 7.6 ContentTone

```text
INFORMATIVE
SIMPLE
STORYTELLING
NON_ACADEMIC
CHILD_FRIENDLY
TOURISTIC
SHORT_AND_CLEAR
```

MVP minimum:

```text
INFORMATIVE
SIMPLE
CHILD_FRIENDLY
SHORT_AND_CLEAR
```

## 7.7 AiGenerationStatus

```text
REQUESTED
PROCESSING
COMPLETED
FAILED
CANCELLED
```

## 7.8 AiErrorType

```text
AI_PROVIDER_TIMEOUT
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_RATE_LIMIT
AI_PROVIDER_AUTH_ERROR
AI_PROVIDER_CONFIG_ERROR
AI_PROVIDER_INTERNAL_ERROR
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
AI_RESPONSE_LANGUAGE_MISMATCH
AI_RESPONSE_UNSAFE_CONTENT
AI_RESPONSE_PURPOSE_MISMATCH
PROJECT_CANCELLED
KNOWLEDGE_ITEM_ARCHIVED
HUMAN_REJECTED
UNKNOWN_AI_ERROR
```

---

# 8. Aggregate ve Sahiplik Kararı

## 8.1 Ana Aggregate

FP-001 için ana aggregate:

```text
ContentProject
```

Çünkü workflow status, AI generation, draft üretimi, review ve ready for publish kararları Content Project çevresinde döner.

## 8.2 Knowledge Item Ayrı Aggregate Olmalı mı?

Evet.

Knowledge Item bağımsız yönetilebilir bir kültürel bilgi kaynağıdır.

Bir Knowledge Item’dan birden fazla Content Project üretilebilir.

Bu nedenle Knowledge Item, Content Project içine gömülmemelidir.

## 8.3 Content Draft Ayrı Aggregate Olmalı mı?

İlk MVP’de Content Draft, Content Project’e bağlı entity olarak ele alınabilir.

Ancak uzun vadede versiyonlama ve review history ihtiyacı artacağı için kendi repository’si olabilir.

MVP için öneri:

* ContentDraft ayrı entity
* ContentProject ile ilişkilendirilmiş
* Lifecycle ContentProject application service tarafından yönetilir

---

# 9. Application Service Sorumlulukları

## 9.1 KnowledgeItemApplicationService

Sorumlulukları:

* Knowledge Item oluşturmak
* Knowledge Item güncellemek
* Knowledge Item listelemek
* Knowledge Item detayını getirmek
* Knowledge Item statüsünü değiştirmek
* Archive işlemini yönetmek

Yapmaması gerekenler:

* AI çağrısı yapmak
* Content Draft oluşturmak
* Review kararı vermek

---

## 9.2 ContentProjectApplicationService

Sorumlulukları:

* Content Project oluşturmak
* Project detayını getirmek
* Project listelemek
* Project status transition’larını yönetmek
* Review başlatmak
* Onay / red / revizyon kararlarını işlemek
* Ready For Publish kararını vermek
* Cancel işlemini yapmak

Yapmaması gerekenler:

* AI provider’a doğrudan bağlanmak
* Prompt oluşturmak
* Provider response parse etmek

---

## 9.3 ContentDraftApplicationService

Sorumlulukları:

* Draft oluşturmak
* Draft detayını getirmek
* Draft güncellemek
* Draft statüsünü yönetmek
* AI output’tan draft oluşturma sürecine yardımcı olmak

İlk MVP’de bu sorumluluklar ContentProjectApplicationService içinde kalabilir.

Ancak karmaşıklık artarsa ayrı service’e taşınmalıdır.

---

## 9.4 AiContentGenerationApplicationService

Sorumlulukları:

* Content Project için AI generation başlatmak
* ContentAgentRequest hazırlamak
* AI Platform boundary’yi çağırmak
* AI response validation sonucunu işlemek
* DraftConversionPayload’dan Content Draft oluşturulmasını tetiklemek
* AI hata durumlarını normalize etmek
* Retry sayısını yönetmek
* Project statüsünü AI akışına göre güncellemek

Yapmaması gerekenler:

* Product review kararı vermek
* Provider SDK detaylarını bilmek
* Raw provider formatına bağımlı olmak
* İnsan onayını bypass etmek

---

# 10. AI Platform Integration Boundary

Engineering tarafında AI Platform’a erişim doğrudan provider SDK ile yapılmamalıdır.

Önerilen conceptual boundary:

```text
AiContentGenerationApplicationService
  → AiContentAgentGateway
  → AiProviderAdapter
  → External AI Provider
```

## 10.1 AiContentAgentGateway

Gateway, Engineering ile AI Platform arasındaki sınırdır.

Sorumlulukları:

* ContentAgentRequest almak
* ContentAgentResponse veya ContentAgentError döndürmek
* Provider detaylarını saklamak
* AI Platform contract’ına uygun çalışmak

## 10.2 AiProviderAdapter

Provider’a özel implementasyon katmanıdır.

Sorumlulukları:

* Dış provider çağrısı yapmak
* Timeout, rate limit, auth gibi hataları yakalamak
* Provider response’u normalize etmek

## 10.3 Domain’in Bilmemesi Gerekenler

Domain layer şunları bilmemelidir:

* Hangi AI provider kullanılıyor
* Hangi model kullanılıyor
* Prompt nasıl hazırlanıyor
* Token sayısı
* Provider response raw formatı
* API key veya provider config

---

# 11. Status Transition Yönetimi

Status geçişleri controller veya frontend tarafından serbestçe yapılmamalıdır.

Status transition’lar application service içinde kontrollü olmalıdır.

## 11.1 Knowledge Item Transition

```text
DRAFT → READY
DRAFT → ARCHIVED
READY → ARCHIVED
```

Kurallar:

* `READY` yapılabilmesi için title zorunlu olmalıdır.
* `ARCHIVED` item’dan yeni Content Project başlatılmamalıdır.

## 11.2 Content Project Transition

Ana akış:

```text
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Alternatif akışlar:

```text
AI_REQUESTED → FAILED
AI_PROCESSING → FAILED
DRAFT_READY → REVISION_REQUESTED
IN_REVIEW → REVISION_REQUESTED
IN_REVIEW → REJECTED
FAILED → AI_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
CREATED → CANCELLED
```

Kurallar:

* `READY_FOR_PUBLISH` sadece `APPROVED` sonrası olabilir.
* `APPROVED` sadece insan review sonrası olabilir.
* `FAILED` durumundan retry yapılabilir.
* `REJECTED` sonrası otomatik retry yapılmaz.
* `CANCELLED` project üzerinde AI generation yapılamaz.

## 11.3 Content Draft Transition

```text
GENERATED → EDITED
GENERATED → APPROVED
GENERATED → REVISION_NEEDED
GENERATED → REJECTED
EDITED → APPROVED
EDITED → REVISION_NEEDED
EDITED → REJECTED
```

Kurallar:

* Draft onayı Project review akışı ile uyumlu olmalıdır.
* Draft silinmemeli, gerekiyorsa yeni draft oluşturulmalıdır.
* İlk MVP’de versiyonlama basit tutulabilir.

---

# 12. Persistence Yaklaşımı

## 12.1 KnowledgeItem Persistence

Saklanması gereken minimum alanlar:

```text
id
title
description
category
city
district
locationText
status
createdAt
updatedAt
```

## 12.2 ContentProject Persistence

Saklanması gereken minimum alanlar:

```text
id
knowledgeItemId
contentPurpose
targetAudience
tone
language
status
retryCount
lastErrorType
lastErrorMessage
createdAt
updatedAt
```

## 12.3 ContentDraft Persistence

Saklanması gereken minimum alanlar:

```text
id
contentProjectId
title
body
summary
status
generatedBy
warningsJson
createdAt
updatedAt
```

## 12.4 AiGenerationRequest Persistence

Saklanması gereken minimum alanlar:

```text
id
contentProjectId
attemptNumber
status
requestPayloadJson
responsePayloadJson
errorType
errorMessage
providerName
modelName
durationMs
createdAt
completedAt
```

Not:

* MVP’de JSON alanları basit tutulabilir.
* İleride payload’lar ayrıca normalize edilebilir.
* Raw provider response saklanacaksa kişisel veri, maliyet ve güvenlik açısından ayrıca değerlendirilmelidir.

---

# 13. REST API Tasarım Yönü

API detayları sonraki dokümanda netleştirilecektir.

Bu dokümanda yalnızca kaynak yönü belirlenir.

Önerilen resource grupları:

```text
/admin/knowledge-items
/admin/content-projects
/admin/content-drafts
/admin/ai-generation-requests
```

Önerilen aksiyonlar:

```text
Create Knowledge Item
Mark Knowledge Item Ready
Archive Knowledge Item
Create Content Project
Request AI Draft Generation
Retry AI Draft Generation
Start Review
Approve Draft
Request Revision
Reject Draft
Mark Ready For Publish
Cancel Project
```

API tasarımında action endpoint’ler status transition’ları application service üzerinden tetiklemelidir.

Frontend doğrudan status set etmemelidir.

---

# 14. Admin Panel Akışı

## 14.1 Knowledge Item Listesi

Gerekli fonksiyonlar:

* Listeleme
* Filtreleme
* Yeni oluşturma
* Detay görüntüleme
* Ready yapma
* Arşivleme
* Content Project oluşturma

## 14.2 Content Project Listesi

Gerekli fonksiyonlar:

* Project listesi
* Status filtresi
* Knowledge Item’a göre filtre
* AI hata durumlarını görme
* Draft hazır olanları görme

## 14.3 Content Project Detayı

Gerekli fonksiyonlar:

* Project bilgilerini görme
* Bağlı Knowledge Item bilgisini görme
* AI generation başlatma
* AI generation retry
* Draft içeriğini görüntüleme
* Review başlatma
* Onaylama
* Revizyon isteme
* Reddetme
* Ready For Publish yapma
* Hata detayını görme

## 14.4 Admin Panel MVP

İlk MVP için minimum ekranlar:

```text
Knowledge Item List
Knowledge Item Detail
Content Project Detail
```

Content Project listesi ilk fazda opsiyonel olabilir ama önerilir.

---

# 15. Error Handling Yaklaşımı

## 15.1 Business Errors

Örnekler:

```text
KNOWLEDGE_ITEM_NOT_READY
KNOWLEDGE_ITEM_ARCHIVED
INVALID_PROJECT_STATUS
DRAFT_NOT_FOUND
PROJECT_ALREADY_CANCELLED
PROJECT_ALREADY_APPROVED
RETRY_LIMIT_EXCEEDED
```

## 15.2 AI Errors

AI errors, AI Platform contract’taki `AiErrorType` ile uyumlu olmalıdır.

Örnekler:

```text
AI_PROVIDER_TIMEOUT
EMPTY_AI_RESPONSE
INVALID_AI_RESPONSE_FORMAT
AI_RESPONSE_TOO_SHORT
```

## 15.3 UI’da Gösterim

Admin Panel’de teknik stack trace gösterilmemelidir.

Gösterilecek bilgiler:

* Hata tipi
* Kullanıcı dostu mesaj
* Retry yapılabilir mi
* Son deneme zamanı
* Deneme sayısı

---

# 16. Retry Mekanizması

Retry logic application service içinde olmalıdır.

MVP kuralları:

* Maksimum retry sayısı `3`
* Retry sadece `FAILED` project için yapılabilir
* Retry sadece retryable error tiplerinde yapılabilir
* Retry yeni AI request oluşturmalıdır
* Retry count artırılmalıdır
* Retry sonrası project `AI_REQUESTED` olmalıdır
* Retry limit aşılırsa hata döndürülmelidir

Revizyon akışı retry değildir.

Revizyon akışı:

```text
REVISION_REQUESTED → AI_REQUESTED
```

Retry akışı:

```text
FAILED → AI_REQUESTED
```

Bu iki akış kodda kavramsal olarak ayrılmalıdır.

---

# 17. Transaction Boundary

Önerilen transaction sınırları:

## 17.1 Knowledge Item Oluşturma

Tek transaction:

```text
Create KnowledgeItem
```

## 17.2 Content Project Oluşturma

Tek transaction:

```text
Validate KnowledgeItem
Create ContentProject
```

## 17.3 AI Generation Başlatma

İki aşamalı düşünülmelidir:

Aşama 1:

```text
Validate Project
Create AiGenerationRequest
Set Project AI_REQUESTED / AI_PROCESSING
Commit
```

Aşama 2:

```text
Call AI Platform
Validate Response
Create Draft or Mark Failed
Update Project
Commit
```

İlk MVP’de senkron çalıştırılabilir.

Ancak uzun vadede async job yapısına uygun tasarlanmalıdır.

---

# 18. Senkron / Asenkron Kararı

MVP’de AI generation senkron başlatılabilir.

Ancak tasarım asenkron yapıya geçişe engel olmamalıdır.

## 18.1 MVP Senkron Akış

Admin butona basar:

```text
Request AI Draft
  → Backend AI çağrısı yapar
  → Response bekler
  → Draft oluşturur
  → UI sonucu gösterir
```

Avantaj:

* Basit implementation
* Hızlı MVP
* Daha az altyapı

Risk:

* Timeout riski
* Uzun bekleme
* UI deneyimi zayıflayabilir

## 18.2 Gelecek Asenkron Akış

Admin butona basar:

```text
Request AI Draft
  → Backend request oluşturur
  → Job queue tetiklenir
  → Worker AI çağrısı yapar
  → Draft oluşturulur
  → UI polling veya notification ile güncellenir
```

MVP’de sync kabul edilir ama entity/status tasarımı async’e uyumlu olmalıdır.

---

# 19. Kod Üretim Sırası

Kod yazımına geçildiğinde önerilen sıra:

## Aşama 1 — Domain ve Persistence

1. Enum’lar
2. Entity’ler
3. Repository’ler
4. Migration dosyaları

## Aşama 2 — Application Layer

1. KnowledgeItemApplicationService
2. ContentProjectApplicationService
3. AiContentGenerationApplicationService
4. Status transition validation

## Aşama 3 — AI Boundary

1. ContentAgentRequest conceptual DTO karşılığı
2. ContentAgentResponse conceptual DTO karşılığı
3. AiContentAgentGateway interface
4. Mock AI provider adapter
5. Response validation

## Aşama 4 — REST API

1. Knowledge Item endpoints
2. Content Project endpoints
3. AI generation action endpoint
4. Review action endpoints

## Aşama 5 — Admin Panel

1. Knowledge Item list/detail
2. Content Project create/detail
3. AI generation button
4. Draft view
5. Review actions
6. Error/retry UI

## Aşama 6 — Test ve Review

1. Unit tests
2. Application service tests
3. Status transition tests
4. AI gateway mock tests
5. Manual admin panel flow test

---

# 20. Test Stratejisi

## 20.1 Unit Test

Öncelikli testler:

* Status transition validation
* Retry limit kontrolü
* Knowledge Item readiness kontrolü
* AI response validation
* Draft conversion

## 20.2 Application Service Test

Test edilecek akışlar:

* Knowledge Item oluşturma
* Content Project oluşturma
* AI generation success
* AI generation failure
* Retry success
* Retry limit exceeded
* Review approve
* Review revision requested
* Review reject
* Ready for publish

## 20.3 Integration Test

İlk MVP’de mock AI gateway ile yapılmalıdır.

Gerçek provider testleri ayrı profile veya manuel test olarak ele alınmalıdır.

## 20.4 Admin Panel Manuel Test

Kontrol edilecek akış:

```text
Create Knowledge Item
Mark Ready
Create Content Project
Generate AI Draft
View Draft
Approve Draft
Mark Ready For Publish
```

---

# 21. Mimari Riskler

## 21.1 AI Provider’a Erken Bağımlılık

Risk:

* Kod doğrudan provider SDK’ya bağımlı hale gelir.

Önlem:

* Gateway / adapter boundary kullanılmalı.

## 21.2 AI Output’un Final İçerik Gibi Kullanılması

Risk:

* İnsan review adımı atlanır.

Önlem:

* Draft ve final status ayrımı korunmalı.
* `READY_FOR_PUBLISH` sadece insan onayı sonrası olmalı.

## 21.3 Status Karmaşası

Risk:

* Frontend doğrudan status set eder.

Önlem:

* Action endpoint ve application service transition kullanılmalı.

## 21.4 Retry ve Revizyonun Karışması

Risk:

* İnsan revizyonu teknik retry gibi ele alınır.

Önlem:

* Retry ve revision ayrı command olarak tasarlanmalı.

## 21.5 Fazla Büyük İlk Implementation

Risk:

* MVP gecikir.

Önlem:

* İlk fazda mock AI provider ile uçtan uca akış tamamlanmalı.
* Gelişmiş agent, fact-checking ve versioning sonraya bırakılmalı.

---

# 22. MVP Engineering Kararı

İlk çalışan FP-001 Engineering MVP şunları içermelidir:

* Knowledge Item CRUD minimum
* Knowledge Item ready/archive
* Content Project create/detail
* AI draft generation action
* Mock veya basit AI gateway
* Content Draft create/view
* Review approve/revision/reject
* Ready For Publish transition
* Retry count ve hata gösterimi
* Admin Panel’den uçtan uca akış

MVP’de gerçek provider entegrasyonu opsiyonel olabilir.

Öncelik:

```text
Önce workflow çalışsın.
Sonra gerçek AI kalitesi artırılsın.
```

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 AI Assisted Content Creation feature’ının Engineering seviyesindeki ana tasarımını netleştirmektir.

Bu doküman ile:

* Product ve AI Platform kararları Engineering’e taşınır.
* Backend domain sınırları belirlenir.
* Ana entity ve enum adayları tanımlanır.
* Application service sorumlulukları ayrıştırılır.
* AI integration boundary netleştirilir.
* Status transition yönetimi tanımlanır.
* Retry ve error handling yaklaşımı belirlenir.
* Admin Panel MVP akışı ortaya konur.
* Kod üretim sırası hazırlanır.

---

# 👤 Aktörler

## Admin

Engineering akışında Admin:

* Knowledge Item oluşturur.
* Content Project başlatır.
* AI generation tetikler.
* Retry yapar.
* İçeriği ready for publish durumuna getirir.

## Editor

Engineering akışında Editor:

* Draft inceler.
* Revizyon ister.
* Draft onaylar.
* Draft reddeder.

## Backend System

Engineering akışında Backend System:

* Domain kurallarını uygular.
* Status transition’ları yönetir.
* AI boundary’yi çağırır.
* Draft oluşturur.
* Retry limitini kontrol eder.
* Hataları normalize eder.

## AI Content Agent

Engineering akışında AI Content Agent:

* Backend tarafından hazırlanan request context’i işler.
* Normalize response döndürür.
* Hata durumunda contract’a uygun error üretir.

## Admin Panel

Engineering akışında Admin Panel:

* Kullanıcı aksiyonlarını backend API’ye iletir.
* Status ve hata durumlarını gösterir.
* Doğrudan business rule uygulamaz.
* Doğrudan status set etmez.

---

# 📄 İş Akışı

## Uçtan Uca MVP İş Akışı

1. Admin, Admin Panel’den Knowledge Item oluşturur.
2. Backend `KnowledgeItem` kaydını `DRAFT` statüsünde oluşturur.
3. Admin Knowledge Item’ı `READY` yapar.
4. Admin bu Knowledge Item üzerinden Content Project oluşturur.
5. Backend `ContentProject` kaydını `CREATED` statüsünde oluşturur.
6. Admin AI draft generation başlatır.
7. Backend project durumunu doğrular.
8. Backend `AiGenerationRequest` oluşturur.
9. Backend AI Platform boundary’ye `ContentAgentRequest` gönderir.
10. AI Content Agent response üretir.
11. Backend response’u validation’dan geçirir.
12. Validation başarılıysa `ContentDraft` oluşturulur.
13. Project `DRAFT_READY` olur.
14. Admin veya Editor review başlatır.
15. Project `IN_REVIEW` olur.
16. Editor draft’ı onaylar, reddeder veya revizyon ister.
17. Onaylanırsa Project `APPROVED` olur.
18. Admin Project’i `READY_FOR_PUBLISH` yapar.
19. Süreç tamamlanır.

---

# 🤖 AI Agent Görevleri

Engineering açısından AI Content Agent’ın görevleri:

* `ContentAgentRequest` contract’ını tüketmek
* AI response’u normalize edilmiş yapıda döndürmek
* Hata durumlarını contract’a uygun bildirmek
* Human feedback varsa revizyon üretmek
* Provider detaylarını domain katmanına sızdırmamak
* Draft oluşturma kararını backend’e bırakmak

AI Content Agent’ın yapmaması gerekenler:

* Veritabanına doğrudan kayıt atmak
* Project status değiştirmek
* Human review kararını vermek
* Final publish kararı vermek
* Backend business rule uygulamak

---

# 👨 İnsan Müdahalesi

İnsan müdahalesi gereken Engineering noktaları:

* Knowledge Item’ın `READY` yapılması
* Draft’ın review edilmesi
* Revizyon notunun girilmesi
* AI warning’lerinin kontrol edilmesi
* Red veya onay kararının verilmesi
* Retry limitinden sonra manuel karar verilmesi
* `READY_FOR_PUBLISH` kararının verilmesi

Kod tarafında insan müdahalesini bypass eden otomatik publish veya otomatik approve davranışı olmamalıdır.

---

# 📊 Status Geçişleri

## Knowledge Item

```text
DRAFT
  → READY
  → ARCHIVED
```

Geçersiz örnekler:

```text
ARCHIVED → READY
ARCHIVED → DRAFT
```

## Content Project

```text
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Alternatif geçişler:

```text
AI_REQUESTED → FAILED
AI_PROCESSING → FAILED
FAILED → AI_REQUESTED
DRAFT_READY → REVISION_REQUESTED
IN_REVIEW → REVISION_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
IN_REVIEW → REJECTED
CREATED → CANCELLED
```

Geçersiz örnekler:

```text
CREATED → READY_FOR_PUBLISH
DRAFT_READY → READY_FOR_PUBLISH
FAILED → READY_FOR_PUBLISH
REJECTED → READY_FOR_PUBLISH
CANCELLED → AI_REQUESTED
```

## Content Draft

```text
GENERATED
  → EDITED
  → APPROVED
```

Alternatif geçişler:

```text
GENERATED → REVISION_NEEDED
GENERATED → REJECTED
EDITED → REVISION_NEEDED
EDITED → REJECTED
```

---

# ⚠ Hata Senaryoları

## Knowledge Item Ready Değilken Project Oluşturma

Beklenen davranış:

* Content Project oluşturulmaz.
* Business error döner.
* Hata: `KNOWLEDGE_ITEM_NOT_READY`

## Archived Knowledge Item’dan Project Oluşturma

Beklenen davranış:

* Content Project oluşturulmaz.
* Hata: `KNOWLEDGE_ITEM_ARCHIVED`

## Geçersiz Project Status’ta AI Generation

Beklenen davranış:

* AI çağrısı yapılmaz.
* Hata: `INVALID_PROJECT_STATUS`

## AI Provider Timeout

Beklenen davranış:

* `AiGenerationRequest` failed olur.
* Content Project `FAILED` olur.
* Hata tipi `AI_PROVIDER_TIMEOUT` olur.
* Retry yapılabilir.

## AI Response Validation Başarısız

Beklenen davranış:

* Draft oluşturulmaz.
* Project `FAILED` olur.
* Validation error saklanır.
* Retry yapılabilir.

## Retry Limit Aşımı

Beklenen davranış:

* Retry yapılmaz.
* Business error: `RETRY_LIMIT_EXCEEDED`
* Manuel müdahale gerekir.

## Human Rejection

Beklenen davranış:

* Draft `REJECTED` olur.
* Project `REJECTED` olur.
* Otomatik retry yapılmaz.

---

# 🔄 Retry Kuralları

Engineering retry kuralları:

* Retry sadece `FAILED` project için yapılabilir.
* Retry sadece retryable AI error type için yapılabilir.
* Retry count maksimum `3` olmalıdır.
* Her retry yeni `AiGenerationRequest` oluşturmalıdır.
* Retry eski request’i overwrite etmemelidir.
* Retry sonrası Project `AI_REQUESTED` olur.
* AI çağrısı başladığında Project `AI_PROCESSING` olur.
* Başarılı response sonrası Project `DRAFT_READY` olur.
* Başarısız response sonrası Project tekrar `FAILED` olur.
* Retry limit aşılırsa manuel müdahale gerekir.

Revizyon kuralları:

* Revizyon `REVISION_REQUESTED` statüsünden başlar.
* Revizyon yeni AI request oluşturur.
* Revizyon retry count ile karıştırılmamalıdır.
* Revizyon notu AI context’ine eklenmelidir.

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* Engineering domain model yönü
* Entity adayları
* Enum adayları
* Application service sınırları
* AI integration boundary
* Status transition kuralları
* Persistence yaklaşımı
* Admin Panel MVP akışı
* Error handling yaklaşımı
* Retry mekanizması
* Transaction boundary
* Sync/async kararı
* Kod üretim sırası
* Test stratejisi

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI hâlâ kod yazmayacaktır.

IDE AI’ın görevi:

1. `docs/05-engineering/18-content-pipeline-engineering.md` dosyasını oluşturmak.
2. Bu dokümanı verilen içerikle doldurmak.
3. Product ve AI Platform dokümanlarıyla çelişen karar eklememek.
4. Entity, DTO, controller, service, repository, migration veya frontend component oluşturmamak.
5. API endpoint detaylarını finalize etmemek.
6. Kod üretim sırasını dokümanda korumak.
7. Sonraki API design dokümanı tamamlanmadan implementation başlatmamak.

IDE AI’a verilecek net komut:

```text
Create the file docs/05-engineering/18-content-pipeline-engineering.md with the provided content.
Do not generate code.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API endpoints yet.
Only add this Engineering design documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Product akışı Engineering seviyesine taşındı.
* AI Platform contract’ları Engineering boundary’ye bağlandı.
* Ana domain kavramları tanımlandı.
* Entity adayları belirlendi.
* Enum adayları belirlendi.
* Application service sorumlulukları ayrıştırıldı.
* AI provider bağımlılığı izole edildi.
* Status transition kuralları tanımlandı.
* Retry ve revision ayrımı netleştirildi.
* Persistence yaklaşımı belirlendi.
* Admin Panel MVP akışı tanımlandı.
* Transaction boundary belirlendi.
* Sync/async kararı MVP için verildi.
* Kod üretim sırası tanımlandı.
* Kod yazımına geçilmedi.
* Sonraki doküman belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Kod aşamasında bu dokümana göre aşağıdakiler kontrol edilmelidir:

* Domain model Product kavramlarını doğru temsil ediyor mu?
* Knowledge Item ve Content Project ayrımı korunmuş mu?
* Content Draft final içerik gibi kullanılmıyor mu?
* AI provider domain/application içine sızmamış mı?
* Status transition’lar controller veya frontend tarafından serbest yapılmıyor mu?
* Application service business rule uyguluyor mu?
* Retry limiti uygulanıyor mu?
* Retry ve revision ayrı akışlar mı?
* AI response validation başarısızsa draft oluşturulmuyor mu?
* Human review olmadan `READY_FOR_PUBLISH` yapılamıyor mu?
* `AiGenerationRequest` geçmişi overwrite edilmiyor mu?
* Admin Panel doğrudan status set etmiyor mu?
* Mock AI gateway ile test yapılabiliyor mu?
* Kod MVP dışı kapsamı gereksiz büyütmüyor mu?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text
docs/05-engineering/19-content-pipeline-api-design.md
```

Bu dokümanda REST API tasarımı netleştirilecektir.

Ele alınacak konular:

* Knowledge Item API endpoints
* Content Project API endpoints
* Content Draft API endpoints
* AI generation action endpoint
* Review action endpoints
* Request / response DTO yönü
* Error response standardı
* Admin Panel’in kullanacağı API akışı
* API seviyesinde status transition güvenliği
* API test senaryoları

Bu API design dokümanı tamamlanmadan backend kod üretimine geçilmeyecektir.
