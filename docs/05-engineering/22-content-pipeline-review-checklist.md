# FP-001 — AI Assisted Content Creation

# Engineering — Content Pipeline Review Checklist

## Doküman Bilgisi

| Alan                           | Değer                                                                                                                                                                                                                                          |
| ------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Doküman                        | `docs/05-engineering/22-content-pipeline-review-checklist.md`                                                                                                                                                                                  |
| Feature                        | `FP-001 AI Assisted Content Creation`                                                                                                                                                                                                          |
| Katman                         | Engineering                                                                                                                                                                                                                                    |
| Önceki Product Dokümanı        | `docs/01-product/00-content-pipeline.md`                                                                                                                                                                                                       |
| Önceki AI Platform Dokümanları | `docs/03-ai-platform/20-content-agent-workflow.md`, `docs/03-ai-platform/21-content-agent-contract.md`                                                                                                                                         |
| Önceki Engineering Dokümanları | `docs/05-engineering/18-content-pipeline-engineering.md`, `docs/05-engineering/19-content-pipeline-api-design.md`, `docs/05-engineering/20-content-pipeline-implementation-plan.md`, `docs/05-engineering/21-content-pipeline-coding-tasks.md` |
| Durum                          | Draft                                                                                                                                                                                                                                          |
| Öncelik                        | High                                                                                                                                                                                                                                           |
| Sahiplik                       | Engineering + Lead Software Architect                                                                                                                                                                                                          |
| Sonraki Adım                   | `Task 01 — Enum Definitions`                                                                                                                                                                                                                   |

---

# 1. Genel Bakış

Bu doküman, FP-001 AI Assisted Content Creation feature’ı için kod üretimine başladıktan sonra her task sonunda kullanılacak review checklist’i tanımlar.

Amaç, IDE AI tarafından üretilen kodun:

* Product vizyonuyla uyumlu kalmasını
* AI Platform contract’larını bozmamasını
* Engineering kararlarına uymasını
* Scope dışına çıkmamasını
* Compile/test edilebilir kalmasını
* İnsan review zorunluluğunu bypass etmemesini

sağlamaktır.

Bu doküman kod değildir.

Bu dokümandan sonra `Task 01 — Enum Definitions` ile kod üretimine başlanabilir.

---

# 2. Review Prensipleri

Her task sonunda şu prensiplerle review yapılmalıdır:

1. **Task kapsamı korunmuş mu?**
   IDE AI sadece verilen task’ı mı yaptı?

2. **Kod compile oluyor mu?**
   Compile hatası varsa sonraki task’a geçilmez.

3. **Dokümanlarla uyum var mı?**
   Product, AI Platform ve Engineering kararları korunmuş mu?

4. **Business rule doğru yerde mi?**
   Controller veya frontend business rule taşımıyor mu?

5. **Status transition güvenli mi?**
   Status doğrudan update edilmiyor mu?

6. **AI output draft olarak mı kalıyor?**
   AI çıktısı otomatik final içerik yapılmıyor mu?

7. **Human review korunuyor mu?**
   İnsan onayı olmadan `READY_FOR_PUBLISH` mümkün değil mi?

8. **Scope creep var mı?**
   Feature dışı kod eklenmiş mi?

9. **Provider bağımsızlığı korunuyor mu?**
   Gerçek AI provider SDK’sı erken eklenmiş mi?

10. **Retry ve revision ayrımı korunuyor mu?**
    Teknik retry ile insan revizyonu karışmamış mı?

---

# 3. Her Task Sonu Genel Checklist

Her task sonunda aşağıdaki checklist uygulanmalıdır:

```text id="g50zfr"
[ ] Kod compile oluyor.
[ ] Test varsa geçiyor.
[ ] Sadece task kapsamındaki dosyalar değişti.
[ ] Mevcut mimari/package standardı bozulmadı.
[ ] Product dokümanına aykırı karar eklenmedi.
[ ] AI Platform workflow dokümanına aykırı karar eklenmedi.
[ ] AI Platform contract dokümanına aykırı karar eklenmedi.
[ ] Engineering design dokümanına aykırı karar eklenmedi.
[ ] API design dokümanına aykırı endpoint eklenmedi.
[ ] Implementation plan sırası bozulmadı.
[ ] Controller business rule içermiyor.
[ ] Frontend business rule’un sahibi gibi davranmıyor.
[ ] Status doğrudan update edilmiyor.
[ ] Human review bypass edilmedi.
[ ] AI output otomatik approved yapılmadı.
[ ] AI output otomatik ready for publish yapılmadı.
[ ] Retry ve revision kavramları karıştırılmadı.
[ ] Gerçek AI provider entegrasyonu erken eklenmedi.
[ ] Raw provider payload API/UI’a sızdırılmadı.
[ ] Scope dışı feature eklenmedi.
[ ] Değişen dosyalar raporlandı.
```

---

# 4. Task 01 Review — Enum Definitions

Kontrol edilecekler:

```text id="d93670"
[ ] Sadece enum dosyaları oluşturuldu.
[ ] Entity oluşturulmadı.
[ ] Repository oluşturulmadı.
[ ] Service oluşturulmadı.
[ ] Controller oluşturulmadı.
[ ] DTO oluşturulmadı.
[ ] Migration oluşturulmadı.
[ ] Frontend kodu oluşturulmadı.
[ ] Gerçek AI provider kodu oluşturulmadı.
[ ] Enum değerleri dokümandaki değerlerle birebir uyumlu.
[ ] Ekstra enum değeri eklenmedi.
[ ] Enum isimleri doğru.
[ ] Kod compile oluyor.
```

Beklenen enum’lar:

```text id="06tbkp"
KnowledgeItemStatus
ContentProjectStatus
ContentDraftStatus
ContentPurpose
TargetAudience
ContentTone
AiGenerationStatus
AiErrorType
GeneratedBy
```

---

# 5. Task 02 Review — Entities and Migrations

Kontrol edilecekler:

```text id="e8ex72"
[ ] Sadece entity ve migration dosyaları oluşturuldu.
[ ] Repository oluşturulmadı.
[ ] Service oluşturulmadı.
[ ] Controller oluşturulmadı.
[ ] API DTO oluşturulmadı.
[ ] Frontend kodu oluşturulmadı.
[ ] KnowledgeItem entity minimum alanları içeriyor.
[ ] ContentProject entity minimum alanları içeriyor.
[ ] ContentDraft entity minimum alanları içeriyor.
[ ] AiGenerationRequest entity minimum alanları içeriyor.
[ ] Enum mapping doğru.
[ ] Timestamp standardı mevcut projeye uygun.
[ ] ID standardı mevcut projeye uygun.
[ ] İlişkiler sade tutuldu.
[ ] Migration tablo adları doğru.
[ ] Migration kolon adları tutarlı.
[ ] Kod compile oluyor.
[ ] Migration çalıştırılabilir görünüyor.
```

Riskli durumlar:

```text id="rbwhhu"
[ ] Entity içine business method doldurulmuş mu?
[ ] AI provider alanları fazla detaylı eklenmiş mi?
[ ] Raw provider response zorunlu hale getirilmiş mi?
[ ] PublishedContent gibi scope dışı entity eklenmiş mi?
```

---

# 6. Task 03 Review — Repositories

Kontrol edilecekler:

```text id="qrwm0a"
[ ] Sadece repository dosyaları oluşturuldu.
[ ] Service oluşturulmadı.
[ ] Controller oluşturulmadı.
[ ] DTO oluşturulmadı.
[ ] Frontend kodu oluşturulmadı.
[ ] KnowledgeItemRepository var.
[ ] ContentProjectRepository var.
[ ] ContentDraftRepository var.
[ ] AiGenerationRequestRepository var.
[ ] Query method’ları MVP için yeterli.
[ ] Gereksiz karmaşık query eklenmedi.
[ ] Kod compile oluyor.
```

Özellikle:

```text id="xz9ye9"
[ ] latest draft sorgusu destekleniyor.
[ ] project AI request history sorgusu destekleniyor.
[ ] attempt number hesaplamaya yardımcı sorgu var.
```

---

# 7. Task 04 Review — Knowledge Item Application Service

Kontrol edilecekler:

```text id="pmmhmq"
[ ] Service sadece Knowledge Item lifecycle’ı yönetiyor.
[ ] Content Project logic eklenmedi.
[ ] AI logic eklenmedi.
[ ] Controller eklenmedi.
[ ] Frontend kodu eklenmedi.
[ ] create işlemi DRAFT statüsüyle başlıyor.
[ ] title zorunlu.
[ ] DRAFT → READY geçişi çalışıyor.
[ ] DRAFT → ARCHIVED geçişi çalışıyor.
[ ] READY → ARCHIVED geçişi çalışıyor.
[ ] ARCHIVED → READY engelleniyor.
[ ] ARCHIVED item update engelleniyor.
[ ] Business rule service içinde.
[ ] Kod compile oluyor.
```

---

# 8. Task 05 Review — Knowledge Item API

Kontrol edilecekler:

```text id="eugeq1"
[ ] Sadece Knowledge Item API eklendi.
[ ] Content Project API eklenmedi.
[ ] AI API eklenmedi.
[ ] Frontend kodu eklenmedi.
[ ] Controller ince.
[ ] Controller business rule içermiyor.
[ ] Controller KnowledgeItemApplicationService çağırıyor.
[ ] Request DTO içinde status alanı yok.
[ ] mark-ready action endpoint olarak var.
[ ] archive action endpoint olarak var.
[ ] Generic status update endpoint yok.
[ ] Error response standardına uyumlu.
[ ] Kod compile oluyor.
```

Endpoint kontrolü:

```text id="lxne7z"
[ ] POST /api/admin/knowledge-items
[ ] GET /api/admin/knowledge-items
[ ] GET /api/admin/knowledge-items/{knowledgeItemId}
[ ] PUT /api/admin/knowledge-items/{knowledgeItemId}
[ ] POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
[ ] POST /api/admin/knowledge-items/{knowledgeItemId}/archive
```

---

# 9. Task 06 Review — Content Project Application Service

Kontrol edilecekler:

```text id="ne0kq4"
[ ] Service Content Project lifecycle’ını yönetiyor.
[ ] AI generation logic eklenmedi.
[ ] Controller eklenmedi.
[ ] Frontend kodu eklenmedi.
[ ] Project sadece READY Knowledge Item’dan oluşturuluyor.
[ ] ARCHIVED Knowledge Item’dan project oluşturulamıyor.
[ ] Yeni project CREATED başlıyor.
[ ] DRAFT_READY → IN_REVIEW geçişi kontrollü.
[ ] IN_REVIEW → APPROVED geçişi kontrollü.
[ ] IN_REVIEW → REVISION_REQUESTED geçişi kontrollü.
[ ] IN_REVIEW → REJECTED geçişi kontrollü.
[ ] APPROVED → READY_FOR_PUBLISH geçişi kontrollü.
[ ] İnsan review olmadan READY_FOR_PUBLISH yapılamıyor.
[ ] CREATED → READY_FOR_PUBLISH engelleniyor.
[ ] FAILED → READY_FOR_PUBLISH engelleniyor.
[ ] REJECTED → READY_FOR_PUBLISH engelleniyor.
[ ] Kod compile oluyor.
```

---

# 10. Task 07 Review — AI Contract DTOs and Gateway Interface

Kontrol edilecekler:

```text id="k90ut5"
[ ] Sadece AI contract DTO’ları oluşturuldu.
[ ] Sadece gateway interface oluşturuldu.
[ ] Mock gateway henüz eklenmedi.
[ ] Real provider entegrasyonu eklenmedi.
[ ] Controller eklenmedi.
[ ] Service implementation eklenmedi.
[ ] Frontend kodu eklenmedi.
[ ] DTO’lar docs/03-ai-platform/21-content-agent-contract.md ile uyumlu.
[ ] API DTO ile AI contract DTO ayrımı korunuyor.
[ ] Provider bağımsızlığı korunuyor.
[ ] Kod compile oluyor.
```

Beklenen DTO/interface:

```text id="ya6n71"
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
AiContentAgentGateway
```

---

# 11. Task 08 Review — Mock AI Gateway

Kontrol edilecekler:

```text id="luc3o9"
[ ] MockAiContentAgentGateway oluşturuldu.
[ ] AiContentAgentGateway interface’ini kullanıyor.
[ ] Real provider entegrasyonu yok.
[ ] Provider SDK dependency eklenmedi.
[ ] Success response deterministic.
[ ] TIMEOUT simülasyonu destekleniyor.
[ ] EMPTY_RESPONSE simülasyonu destekleniyor.
[ ] Ek hata modları varsa contract ile uyumlu.
[ ] Mock response ContentAgentResponse ile uyumlu.
[ ] Mock error ContentAgentError ile uyumlu.
[ ] Kod compile oluyor.
```

Riskli durum:

```text id="4bq8ar"
[ ] Mock gateway project status değiştirmeye çalışıyor mu?
[ ] Mock gateway database’e erişiyor mu?
[ ] Mock gateway business rule uyguluyor mu?
```

Bu durumlar varsa task kabul edilmez.

---

# 12. Task 09 Review — AI Generation Application Service

Kontrol edilecekler:

```text id="qkcfqy"
[ ] AI generation service oluşturuldu.
[ ] Gateway interface üzerinden çalışıyor.
[ ] Real provider SDK kullanılmıyor.
[ ] Generate sadece CREATED veya REVISION_REQUESTED için.
[ ] Retry sadece FAILED için.
[ ] Retry max 3 uygulanıyor.
[ ] Her retry yeni AiGenerationRequest oluşturuyor.
[ ] AI_REQUESTED ve AI_PROCESSING statüleri doğru set ediliyor.
[ ] Success sonrası ContentDraft oluşturuluyor.
[ ] Success sonrası Project DRAFT_READY oluyor.
[ ] Failure sonrası Project FAILED oluyor.
[ ] Validation başarısızsa draft oluşturulmuyor.
[ ] Error type project üzerinde saklanıyor.
[ ] AiGenerationRequest response/error bilgisi güncelleniyor.
[ ] Revision retry count ile karıştırılmıyor.
[ ] Kod compile oluyor.
```

Özellikle kontrol:

```text id="fas5u3"
[ ] AI output otomatik APPROVED yapılmıyor.
[ ] AI output otomatik READY_FOR_PUBLISH yapılmıyor.
[ ] Human review bypass edilmiyor.
```

---

# 13. Task 10 Review — Content Project API Actions

Kontrol edilecekler:

```text id="yu1uvm"
[ ] Content Project API endpoint’leri eklendi.
[ ] Controller business rule içermiyor.
[ ] Controller application service çağırıyor.
[ ] Request DTO’larda status alanı yok.
[ ] Direct status update endpoint yok.
[ ] generate-ai-draft action endpoint var.
[ ] retry-ai-draft action endpoint var.
[ ] start-review action endpoint var.
[ ] approve action endpoint var.
[ ] request-revision action endpoint var.
[ ] reject action endpoint var.
[ ] mark-ready-for-publish action endpoint var.
[ ] cancel action endpoint var.
[ ] Raw AI/provider payload response’a sızmıyor.
[ ] Error response standardına uyuluyor.
[ ] Kod compile oluyor.
```

Endpoint kontrolü:

```text id="fp5zuj"
[ ] POST /api/admin/content-projects
[ ] GET /api/admin/content-projects
[ ] GET /api/admin/content-projects/{contentProjectId}
[ ] POST /api/admin/content-projects/{contentProjectId}/generate-ai-draft
[ ] POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
[ ] POST /api/admin/content-projects/{contentProjectId}/start-review
[ ] POST /api/admin/content-projects/{contentProjectId}/approve
[ ] POST /api/admin/content-projects/{contentProjectId}/request-revision
[ ] POST /api/admin/content-projects/{contentProjectId}/reject
[ ] POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish
[ ] POST /api/admin/content-projects/{contentProjectId}/cancel
```

---

# 14. Task 11 Review — Content Draft API

Kontrol edilecekler:

```text id="k94xxu"
[ ] Draft detail endpoint var.
[ ] Draft update endpoint var.
[ ] Controller business rule içermiyor.
[ ] Draft update service üzerinden yapılıyor.
[ ] Draft sadece GENERATED veya REVISION_NEEDED iken update ediliyor.
[ ] Manuel update sonrası draft EDITED oluyor.
[ ] Draft update project’i APPROVED yapmıyor.
[ ] Draft update project’i READY_FOR_PUBLISH yapmıyor.
[ ] Kod compile oluyor.
```

Endpoint kontrolü:

```text id="bkg9xh"
[ ] GET /api/admin/content-drafts/{contentDraftId}
[ ] PUT /api/admin/content-drafts/{contentDraftId}
```

---

# 15. Task 12 Review — Backend Tests

Kontrol edilecekler:

```text id="e5flw9"
[ ] Knowledge Item lifecycle testleri var.
[ ] Content Project lifecycle testleri var.
[ ] AI generation success testi var.
[ ] AI generation failure testi var.
[ ] Empty response testi var.
[ ] Retry success testi var.
[ ] Retry max 3 testi var.
[ ] Revision flow testi var.
[ ] Human rejection testi var.
[ ] Invalid transition testleri var.
[ ] Testlerde real AI provider kullanılmıyor.
[ ] Mock gateway kullanılıyor.
[ ] Testler geçiyor.
```

---

# 16. Task 13 Review — Admin Panel Knowledge Item Screens

Kontrol edilecekler:

```text id="d42ic8"
[ ] Knowledge Item List ekranı var.
[ ] Create/Edit form var.
[ ] Detail ekranı var.
[ ] Create API’ye bağlı.
[ ] Update API’ye bağlı.
[ ] Mark Ready action API’ye bağlı.
[ ] Archive action API’ye bağlı.
[ ] Kullanıcı doğrudan status seçemiyor.
[ ] Action butonları status’a göre gösteriliyor.
[ ] ARCHIVED item için uygun aksiyonlar kapalı.
[ ] Frontend build oluyor.
```

---

# 17. Task 14 Review — Admin Panel Content Project Screens

Kontrol edilecekler:

```text id="fg0w04"
[ ] Content Project create flow var.
[ ] Project sadece READY Knowledge Item’dan oluşturuluyor.
[ ] Content Project Detail ekranı var.
[ ] Linked Knowledge Item gösteriliyor.
[ ] Content purpose gösteriliyor.
[ ] Target audience gösteriliyor.
[ ] Tone gösteriliyor.
[ ] Language gösteriliyor.
[ ] Project status gösteriliyor.
[ ] Latest draft varsa gösteriliyor.
[ ] Last AI error varsa gösteriliyor.
[ ] Kullanıcı doğrudan status değiştiremiyor.
[ ] Frontend build oluyor.
```

---

# 18. Task 15 Review — Admin Panel AI Draft and Review Actions

Kontrol edilecekler:

```text id="kx4pej"
[ ] Generate AI Draft butonu var.
[ ] Generate sadece uygun statülerde görünüyor.
[ ] Retry butonu var.
[ ] Retry sadece FAILED ve retryable hata varsa görünüyor.
[ ] Start Review action var.
[ ] Approve action var.
[ ] Request Revision action var.
[ ] Reject action var.
[ ] Mark Ready For Publish action var.
[ ] Cancel action var.
[ ] Update Draft action var.
[ ] Revision note zorunlu.
[ ] Reject note zorunlu.
[ ] AI warnings editöre görünüyor.
[ ] Last error kullanıcı dostu gösteriliyor.
[ ] Draft update otomatik approve yapmıyor.
[ ] Kullanıcı doğrudan status değiştiremiyor.
[ ] Frontend build oluyor.
```

---

# 19. Task 16 Review — End-to-End Manual Test

Kontrol edilecekler:

```text id="z3x4sk"
[ ] Happy path tamamlandı.
[ ] Retry path tamamlandı.
[ ] Revision path tamamlandı.
[ ] Rejection path tamamlandı.
[ ] Invalid transition path tamamlandı.
[ ] Hatalı senaryolar raporlandı.
[ ] Yeni feature eklenmedi.
```

Happy path:

```text id="348o0l"
Knowledge Item oluştur
→ READY yap
→ Content Project oluştur
→ AI Draft üret
→ Draft gör
→ Review başlat
→ Approve et
→ Ready For Publish yap
```

---

# 20. Durdurma Kriterleri

Aşağıdaki durumlardan biri görülürse sonraki task’a geçilmemelidir:

```text id="6mtwl7"
[ ] Kod compile olmuyor.
[ ] Testler fail ediyor.
[ ] Task kapsamı dışı büyük dosya değişiklikleri var.
[ ] Mevcut mimari bozulmuş.
[ ] Generic status update endpoint eklenmiş.
[ ] Controller business rule içeriyor.
[ ] AI output otomatik approved yapılıyor.
[ ] AI output otomatik ready for publish yapılıyor.
[ ] Human review bypass edilmiş.
[ ] Retry max 3 uygulanmıyor.
[ ] Retry ve revision aynı akışa karışmış.
[ ] Real AI provider SDK erken eklenmiş.
[ ] Public publishing eklenmiş.
[ ] Mobile API eklenmiş.
[ ] Image/video/audio/storyboard feature eklenmiş.
[ ] Raw provider payload UI/API’ye sızdırılmış.
```

---

# 21. Task Sonu Rapor Formatı

Her kod task’ı sonunda IDE AI’dan şu formatta rapor istenmelidir:

```text id="hjcc54"
Task:
Status:
Changed files:
Added files:
Removed files:
Compile result:
Test result:
Scope notes:
Known issues:
Next recommended task:
```

Örnek:

```text id="hm92fs"
Task: FP-001 Task 01 — Enum Definitions
Status: Completed
Changed files: none
Added files:
- src/main/java/.../KnowledgeItemStatus.java
- src/main/java/.../ContentProjectStatus.java
Compile result: Success
Test result: Not run
Scope notes: Only enum files were added.
Known issues: None
Next recommended task: Task 02 — Entities and Migrations
```

---

# 22. Kod Review Kararları

Her task sonunda üç karardan biri verilir:

## 22.1 Accepted

Task tamamdır. Sonraki task’a geçilebilir.

Kriter:

```text id="klkcrh"
Compile başarılı
Scope uygun
Kritik mimari ihlal yok
```

## 22.2 Needs Fix

Task genel olarak doğru ama küçük düzeltme gerekir.

Kriter:

```text id="90dmuj"
Compile başarılı veya kolay düzeltilebilir
Scope büyük ölçüde uygun
Kritik mimari ihlal yok
```

## 22.3 Rejected

Task kabul edilmez. Geri alınmalı veya yeniden yaptırılmalıdır.

Kriter:

```text id="xwrv0v"
Compile başarısız
Scope dışı büyük değişiklik
Human review bypass
Provider bağımlılığı
Status güvenliği ihlali
```

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 kod üretim sürecinde her task sonunda kullanılacak review mekanizmasını netleştirmektir.

Bu doküman ile:

* Genel review prensipleri belirlenir.
* Task bazlı checklist’ler tanımlanır.
* Durdurma kriterleri yazılır.
* Task sonu rapor formatı belirlenir.
* Accepted / Needs Fix / Rejected karar modeli tanımlanır.
* Kod üretiminin dokümanlara bağlı kalması sağlanır.

---

# 👤 Aktörler

## Backend Developer

Kod task’larını uygular veya IDE AI’a uygulatır.

Review sorumlulukları:

* Compile/test kontrolü
* Değişen dosyaların incelenmesi
* Business rule lokasyonunun kontrolü
* Status transition güvenliğinin kontrolü

## Frontend Developer

Admin Panel task’larını uygular veya IDE AI’a uygulatır.

Review sorumlulukları:

* UI action visibility kontrolü
* Direct status update yapılmadığını kontrol etmek
* API error handling kontrolü
* Build kontrolü

## IDE AI

Task’ları uygular ve task sonunda rapor verir.

Sorumlulukları:

* Sadece task kapsamını yapmak
* Değişen dosyaları raporlamak
* Compile/test sonucunu belirtmek
* Bilinen sorunları açıkça yazmak

## Lead Software Architect

Final review kararını verir.

Sorumlulukları:

* Product → AI Platform → Engineering → Kod sırasını korumak
* Scope creep’i engellemek
* Mimari sınırları korumak
* Gerektiğinde task’ı durdurmak veya geri aldırmak

---

# 📄 İş Akışı

Review iş akışı:

1. IDE AI ilgili task’ı tamamlar.
2. IDE AI task sonu raporunu verir.
3. Compile kontrol edilir.
4. Test varsa çalıştırılır.
5. Değişen dosyalar incelenir.
6. Genel checklist uygulanır.
7. Task özel checklist uygulanır.
8. Durdurma kriterleri kontrol edilir.
9. `Accepted`, `Needs Fix` veya `Rejected` kararı verilir.
10. Accepted ise sonraki task’a geçilir.
11. Needs Fix ise sadece düzeltme yaptırılır.
12. Rejected ise task geri alınır veya yeniden yaptırılır.

---

# 🤖 AI Agent Görevleri

Bu review dokümanında AI Content Agent production görevinde değildir.

Kod üretim sürecinde AI agent önce mock olarak temsil edilir.

Review sırasında kontrol edilecek AI agent sınırları:

* Mock agent database’e erişmemeli.
* Mock agent project status değiştirmemeli.
* Mock agent final approve kararı vermemeli.
* Mock agent provider SDK kullanmamalı.
* Mock agent sadece contract’a uygun response/error üretmeli.

Gerçek AI provider entegrasyonu bu feature’ın ilk kod sprintinde zorunlu değildir.

---

# 👨 İnsan Müdahalesi

İnsan müdahalesi review sürecinde zorunludur.

IDE AI’ın ürettiği kod otomatik kabul edilmez.

İnsan review şu noktalarda özellikle gereklidir:

* Scope kontrolü
* Status geçişleri
* Retry / revision ayrımı
* Human review zorunluluğu
* AI provider boundary
* Admin Panel action visibility
* Error handling
* Test kapsamı

---

# 📊 Status Geçişleri

Kod review sırasında aşağıdaki status geçişleri korunmalıdır.

## Knowledge Item

Geçerli:

```text id="fsue32"
DRAFT → READY
DRAFT → ARCHIVED
READY → ARCHIVED
```

Geçersiz:

```text id="dv44ln"
ARCHIVED → READY
ARCHIVED → DRAFT
```

## Content Project

Geçerli ana akış:

```text id="yss4d2"
CREATED
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
  → IN_REVIEW
  → APPROVED
  → READY_FOR_PUBLISH
```

Geçerli alternatifler:

```text id="5n2bl9"
AI_PROCESSING → FAILED
FAILED → AI_REQUESTED
DRAFT_READY → REVISION_REQUESTED
IN_REVIEW → REVISION_REQUESTED
REVISION_REQUESTED → AI_REQUESTED
IN_REVIEW → REJECTED
CREATED → CANCELLED
```

Geçersiz:

```text id="jkcara"
CREATED → READY_FOR_PUBLISH
DRAFT_READY → READY_FOR_PUBLISH
FAILED → READY_FOR_PUBLISH
REJECTED → READY_FOR_PUBLISH
CANCELLED → AI_REQUESTED
```

## Content Draft

Geçerli:

```text id="vtq6dp"
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

Review sırasında özellikle aranacak hata senaryoları:

## Human Review Bypass

Belirti:

```text id="p6aly7"
AI response success
→ Draft auto approved
→ Project auto ready for publish
```

Karar:

```text id="80p2th"
Rejected
```

## Direct Status Update

Belirti:

```text id="pgwfgi"
PUT /content-projects/{id}
{
  "status": "APPROVED"
}
```

Karar:

```text id="vooihm"
Rejected
```

## Provider Leakage

Belirti:

```text id="6wz844"
Controller veya domain doğrudan OpenAI/Gemini SDK kullanıyor.
```

Karar:

```text id="f7492u"
Rejected
```

## Retry Limit Yok

Belirti:

```text id="hhrw99"
FAILED project sınırsız retry edilebiliyor.
```

Karar:

```text id="hj8ckk"
Rejected
```

## Revision Retry Gibi Sayılıyor

Belirti:

```text id="n7rqbv"
Human revision retry count artırıyor.
```

Karar:

```text id="7etm6c"
Needs Fix veya Rejected
```

## Scope Creep

Belirti:

```text id="70yvbs"
Image generation, video, mobile API, public publishing, fact-checking gibi özellikler eklenmiş.
```

Karar:

```text id="lwbaks"
Rejected
```

---

# 🔄 Retry Kuralları

Review sırasında retry implementation için kontrol:

```text id="37p2zc"
[ ] Retry endpoint/action ayrı.
[ ] Retry sadece FAILED project için.
[ ] Retry max 3.
[ ] Retry count backend’de tutuluyor.
[ ] Her retry yeni AiGenerationRequest oluşturuyor.
[ ] Non-retryable hata retry edilmiyor.
[ ] Retry başarılıysa yeni draft oluşuyor.
[ ] Retry başarısızsa project FAILED kalıyor.
[ ] Revision retry sayılmıyor.
[ ] Human rejection sonrası otomatik retry yok.
```

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* Genel review prensipleri
* Task bazlı review checklist’leri
* Durdurma kriterleri
* Task sonu rapor formatı
* Review karar modeli
* Status transition review kuralları
* Retry review kuralları
* Human review bypass kontrolü
* Provider bağımsızlığı kontrolü

---

# 🤖 IDE AI Görevi

Bu dokümanı oluşturmak için IDE AI’a verilecek komut:

```text id="ozk1nm"
Create the file docs/05-engineering/22-content-pipeline-review-checklist.md with the provided content.
Do not renumber existing docs/05-engineering files.
Do not generate production code.
Do not create entities, DTOs, controllers, services, repositories, migrations, frontend pages, or API endpoints while creating this document.
Only add this Engineering review checklist documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Genel review checklist yazıldı.
* Task 01–16 için özel checklist yazıldı.
* Durdurma kriterleri tanımlandı.
* Task sonu rapor formatı tanımlandı.
* Accepted / Needs Fix / Rejected karar modeli tanımlandı.
* Status transition review kuralları yazıldı.
* Retry review kuralları yazıldı.
* Human review bypass kontrolü yazıldı.
* Provider leakage kontrolü yazıldı.
* Kod üretimine geçmeden önce review mekanizması hazırlandı.

---

# 🔍 Kod İnceleme Kontrol Listesi

Kod review sırasında uygulanacak en kısa kritik checklist:

```text id="zcclbe"
[ ] Compile başarılı mı?
[ ] Testler başarılı mı?
[ ] Task scope’u korunmuş mu?
[ ] Human review bypass edilmemiş mi?
[ ] Status doğrudan update edilmiyor mu?
[ ] Retry max 3 uygulanıyor mu?
[ ] Retry ve revision ayrı mı?
[ ] AI provider bağımsızlığı korunuyor mu?
[ ] Controller business rule içermiyor mu?
[ ] Scope dışı feature yok mu?
```

---

# ➡ Sonraki Doküman / Sonraki Adım

Bu dokümandan sonra yeni doküman gerekmiyor.

Sonraki adım kod üretimidir:

```text id="h1ff9r"
Task 01 — Enum Definitions
```

İlk kod task komutu:

```text id="sfh39o"
Start FP-001 Task 01 — Enum Definitions.

Follow docs/01-product/00-content-pipeline.md,
docs/03-ai-platform/20-content-agent-workflow.md,
docs/03-ai-platform/21-content-agent-contract.md,
docs/05-engineering/18-content-pipeline-engineering.md,
docs/05-engineering/19-content-pipeline-api-design.md,
docs/05-engineering/20-content-pipeline-implementation-plan.md,
docs/05-engineering/21-content-pipeline-coding-tasks.md,
and docs/05-engineering/22-content-pipeline-review-checklist.md.

Implement only the enum definitions required for FP-001:
- KnowledgeItemStatus
- ContentProjectStatus
- ContentDraftStatus
- ContentPurpose
- TargetAudience
- ContentTone
- AiGenerationStatus
- AiErrorType
- GeneratedBy

Do not create entities, repositories, services, controllers, migrations, DTOs, frontend code, or AI provider integration in this task.

Use existing project package conventions.
Ensure the project compiles after this task.
Stop after Task 01 and report changed files using the task report format from docs/05-engineering/22-content-pipeline-review-checklist.md.
```
