# FP-001 — AI Assisted Content Creation

# Engineering — Content Pipeline API Design

## Doküman Bilgisi

| Alan                           | Değer                                                                                                  |
| ------------------------------ | ------------------------------------------------------------------------------------------------------ |
| Doküman                        | `docs/05-engineering/19-content-pipeline-api-design.md`                                                |
| Feature                        | `FP-001 AI Assisted Content Creation`                                                                  |
| Katman                         | Engineering                                                                                            |
| Önceki Product Dokümanı        | `docs/01-product/00-content-pipeline.md`                                                               |
| Önceki AI Platform Dokümanları | `docs/03-ai-platform/20-content-agent-workflow.md`, `docs/03-ai-platform/21-content-agent-contract.md` |
| Önceki Engineering Dokümanı    | `docs/05-engineering/18-content-pipeline-engineering.md`                                               |
| Durum                          | Draft                                                                                                  |
| Öncelik                        | High                                                                                                   |
| Sahiplik                       | Engineering + Lead Software Architect                                                                  |
| Sonraki Doküman                | `docs/05-engineering/20-content-pipeline-implementation-plan.md`                                       |

---

# 1. Genel Bakış

Bu doküman, FP-001 AI Assisted Content Creation feature’ı için Admin Panel tarafından kullanılacak REST API tasarımını Engineering seviyesinde tanımlar.

Bu API tasarımı şu Product akışını destekler:

```text
Knowledge Item
  → Content Project
  → AI Generated Content Draft
  → Human Review
  → Ready For Publish Content
```

Bu doküman hâlâ kod üretimi değildir.

Bu dokümanda endpoint yönü, request/response beklentileri, action endpoint’ler, status transition güvenliği, hata modeli ve Admin Panel’in API kullanımı tanımlanır.

Bu doküman tamamlandıktan sonra implementation plan dokümanına geçilecektir.

---

# 2. API Tasarım Prensipleri

FP-001 API tasarımı aşağıdaki prensiplere uymalıdır:

1. **Admin odaklıdır.**
   Bu endpoint’ler public kullanıcıya değil Admin Panel’e hizmet eder.

2. **Status doğrudan set edilmez.**
   Frontend `status = APPROVED` gibi doğrudan update yapmaz. Bunun yerine action endpoint çağırır.

3. **Business rule backend’dedir.**
   Admin Panel sadece aksiyon tetikler. Geçerli/geçersiz status transition kararını backend verir.

4. **AI provider detayları API’ye sızmaz.**
   API, OpenAI/Gemini/Anthropic gibi provider detaylarına bağımlı olmamalıdır.

5. **AI çıktısı final içerik değildir.**
   API response’larında AI draft ile final publish durumu ayrılmalıdır.

6. **Retry ve revision ayrı action’lardır.**
   Teknik hata retry, insan geri bildirimi revision olarak ele alınır.

7. **Hatalar kullanıcı dostu ve izlenebilir olmalıdır.**
   Stack trace veya provider secret bilgisi dönülmez.

8. **MVP sade olmalıdır.**
   İlk hedef uçtan uca çalışan feature’dır; gereksiz endpoint zenginliği sonraya bırakılır.

---

# 3. Base Path

FP-001 Admin API kaynakları için önerilen base path:

```text
/api/admin
```

Önerilen resource grupları:

```text
/api/admin/knowledge-items
/api/admin/content-projects
/api/admin/content-drafts
/api/admin/ai-generation-requests
```

---

# 4. Ana Resource’lar

## 4.1 Knowledge Item

Kültürel bilgi kaynağıdır.

API üzerinden:

* Oluşturulur
* Listelenir
* Detayı görüntülenir
* Güncellenir
* `READY` yapılır
* Arşivlenir

## 4.2 Content Project

Bir Knowledge Item üzerinden içerik üretim sürecidir.

API üzerinden:

* Oluşturulur
* Listelenir
* Detayı görüntülenir
* AI generation başlatılır
* Retry yapılır
* Review başlatılır
* Revizyon istenir
* Onaylanır
* Reddedilir
* Ready for publish yapılır
* İptal edilir

## 4.3 Content Draft

AI veya insan tarafından oluşturulan içerik taslağıdır.

API üzerinden:

* Görüntülenir
* Güncellenebilir
* Project detayında gösterilir
* Review aksiyonlarına kaynak olur

## 4.4 AI Generation Request

AI çağrısının izlenebilir kaydıdır.

API üzerinden:

* Project detayında son hata gösterilebilir
* İstenirse request history listelenebilir
* MVP’de ayrı listeleme opsiyoneldir

---

# 5. Knowledge Item API

## 5.1 Knowledge Item Oluşturma

```http
POST /api/admin/knowledge-items
```

Amaç:

Yeni bir kültürel bilgi kaydı oluşturur.

Request:

```json
{
  "title": "Ankara Kalesi",
  "description": "Ankara'nın tarihi simgelerinden biri.",
  "category": "HISTORICAL_PLACE",
  "city": "Ankara",
  "district": "Altındağ",
  "locationText": "Ankara, Altındağ",
  "notes": "Tarihî hassasiyet korunmalı."
}
```

Response:

```json
{
  "id": "knowledge-item-id",
  "title": "Ankara Kalesi",
  "description": "Ankara'nın tarihi simgelerinden biri.",
  "category": "HISTORICAL_PLACE",
  "city": "Ankara",
  "district": "Altındağ",
  "locationText": "Ankara, Altındağ",
  "status": "DRAFT",
  "createdAt": "2026-06-28T12:00:00Z",
  "updatedAt": "2026-06-28T12:00:00Z"
}
```

Kurallar:

* `title` zorunludur.
* İlk statü `DRAFT` olmalıdır.
* Aynı başlıkla kayıt olup olmayacağı Engineering implementation sırasında netleştirilebilir.

---

## 5.2 Knowledge Item Listeleme

```http
GET /api/admin/knowledge-items
```

Query parametreleri:

```text
status
category
city
search
page
size
sort
```

Örnek:

```http
GET /api/admin/knowledge-items?status=READY&city=Ankara&page=0&size=20
```

Response:

```json
{
  "items": [
    {
      "id": "knowledge-item-id",
      "title": "Ankara Kalesi",
      "category": "HISTORICAL_PLACE",
      "city": "Ankara",
      "district": "Altındağ",
      "status": "READY",
      "createdAt": "2026-06-28T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 5.3 Knowledge Item Detay

```http
GET /api/admin/knowledge-items/{knowledgeItemId}
```

Response:

```json
{
  "id": "knowledge-item-id",
  "title": "Ankara Kalesi",
  "description": "Ankara'nın tarihi simgelerinden biri.",
  "category": "HISTORICAL_PLACE",
  "city": "Ankara",
  "district": "Altındağ",
  "locationText": "Ankara, Altındağ",
  "notes": "Tarihî hassasiyet korunmalı.",
  "status": "READY",
  "createdAt": "2026-06-28T12:00:00Z",
  "updatedAt": "2026-06-28T12:00:00Z"
}
```

---

## 5.4 Knowledge Item Güncelleme

```http
PUT /api/admin/knowledge-items/{knowledgeItemId}
```

Request:

```json
{
  "title": "Ankara Kalesi",
  "description": "Ankara'nın tarihî simgelerinden biri.",
  "category": "HISTORICAL_PLACE",
  "city": "Ankara",
  "district": "Altındağ",
  "locationText": "Ankara, Altındağ",
  "notes": "Mobil içeriklerde kısa anlatım tercih edilmeli."
}
```

Kurallar:

* Archived kayıt güncellenebilir mi kararı implementation’da verilebilir.
* MVP önerisi: `ARCHIVED` kayıt güncellenmesin.

---

## 5.5 Knowledge Item Ready Yapma

```http
POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
```

Amaç:

Knowledge Item’ı içerik üretimine hazır hale getirir.

Kurallar:

* Sadece `DRAFT` durumundan `READY` yapılabilir.
* `title` boş olamaz.
* `ARCHIVED` kayıt ready yapılamaz.

Response:

```json
{
  "id": "knowledge-item-id",
  "status": "READY"
}
```

---

## 5.6 Knowledge Item Arşivleme

```http
POST /api/admin/knowledge-items/{knowledgeItemId}/archive
```

Amaç:

Knowledge Item’ı aktif üretimden çıkarır.

Kurallar:

* `DRAFT` veya `READY` item arşivlenebilir.
* `ARCHIVED` kayıt tekrar arşivlenirse idempotent davranış tercih edilebilir.
* Archived item’dan yeni Content Project oluşturulamaz.

Response:

```json
{
  "id": "knowledge-item-id",
  "status": "ARCHIVED"
}
```

---

# 6. Content Project API

## 6.1 Content Project Oluşturma

```http
POST /api/admin/content-projects
```

Amaç:

Bir Knowledge Item üzerinden içerik üretim projesi başlatır.

Request:

```json
{
  "knowledgeItemId": "knowledge-item-id",
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr",
  "formatHint": "Kısa ve bilgilendirici metin",
  "lengthHint": "300-500 kelime"
}
```

Response:

```json
{
  "id": "content-project-id",
  "knowledgeItemId": "knowledge-item-id",
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr",
  "status": "CREATED",
  "retryCount": 0,
  "createdAt": "2026-06-28T12:00:00Z"
}
```

Kurallar:

* Knowledge Item `READY` olmalıdır.
* Knowledge Item `ARCHIVED` olmamalıdır.
* İlk project statüsü `CREATED` olmalıdır.

---

## 6.2 Content Project Listeleme

```http
GET /api/admin/content-projects
```

Query parametreleri:

```text
status
knowledgeItemId
contentPurpose
targetAudience
search
page
size
sort
```

Response:

```json
{
  "items": [
    {
      "id": "content-project-id",
      "knowledgeItemTitle": "Ankara Kalesi",
      "contentPurpose": "INFORMATIONAL_TEXT",
      "targetAudience": "GENERAL_USER",
      "tone": "INFORMATIVE",
      "language": "tr",
      "status": "DRAFT_READY",
      "retryCount": 0,
      "lastErrorType": null,
      "createdAt": "2026-06-28T12:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

MVP’de bu endpoint opsiyonel olabilir ancak Admin Panel takibi için önerilir.

---

## 6.3 Content Project Detay

```http
GET /api/admin/content-projects/{contentProjectId}
```

Response:

```json
{
  "id": "content-project-id",
  "knowledgeItem": {
    "id": "knowledge-item-id",
    "title": "Ankara Kalesi",
    "category": "HISTORICAL_PLACE",
    "city": "Ankara",
    "district": "Altındağ"
  },
  "contentPurpose": "INFORMATIONAL_TEXT",
  "targetAudience": "GENERAL_USER",
  "tone": "INFORMATIVE",
  "language": "tr",
  "status": "DRAFT_READY",
  "retryCount": 0,
  "lastErrorType": null,
  "lastErrorMessage": null,
  "latestDraft": {
    "id": "content-draft-id",
    "title": "Ankara Kalesi",
    "body": "Ankara Kalesi, başkentin tarihî dokusunu yansıtan...",
    "summary": "Ankara Kalesi hakkında kısa tanıtım.",
    "status": "GENERATED",
    "generatedBy": "AI_CONTENT_AGENT",
    "warnings": []
  },
  "createdAt": "2026-06-28T12:00:00Z",
  "updatedAt": "2026-06-28T12:05:00Z"
}
```

---

# 7. AI Generation Action API

## 7.1 AI Draft Generation Başlatma

```http
POST /api/admin/content-projects/{contentProjectId}/generate-ai-draft
```

Amaç:

Content Project için AI destekli draft üretimini başlatır.

Request:

```json
{
  "additionalInstruction": "Metni sade ve anlaşılır tut."
}
```

Request body MVP’de opsiyonel olabilir.

Response:

```json
{
  "projectId": "content-project-id",
  "status": "DRAFT_READY",
  "draft": {
    "id": "content-draft-id",
    "title": "Ankara Kalesi",
    "body": "Ankara Kalesi, Ankara'nın tarihî dokusunu yansıtan...",
    "summary": "Ankara Kalesi hakkında kısa tanıtım.",
    "status": "GENERATED",
    "warnings": []
  },
  "aiGenerationRequest": {
    "id": "ai-request-id",
    "status": "COMPLETED",
    "attemptNumber": 1
  }
}
```

Kurallar:

* Project `CREATED` veya `REVISION_REQUESTED` durumunda olabilir.
* Project `FAILED` durumundaysa bu endpoint yerine retry endpoint kullanılmalıdır.
* Project `CANCELLED`, `REJECTED`, `READY_FOR_PUBLISH` ise AI generation yapılamaz.
* AI response validation başarısız olursa draft oluşturulmaz.
* Başarılı sonuçta Project `DRAFT_READY` olur.

---

## 7.2 AI Draft Retry

```http
POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
```

Amaç:

Teknik veya validation hatası nedeniyle başarısız olmuş AI generation işlemini tekrar dener.

Request:

```json
{
  "retryReason": "Timeout sonrası tekrar deneme"
}
```

Response başarılıysa:

```json
{
  "projectId": "content-project-id",
  "status": "DRAFT_READY",
  "retryCount": 1,
  "draft": {
    "id": "content-draft-id",
    "title": "Ankara Kalesi",
    "body": "Ankara Kalesi, başkentin tarihî dokusunu yansıtan...",
    "status": "GENERATED"
  }
}
```

Response başarısızsa:

```json
{
  "projectId": "content-project-id",
  "status": "FAILED",
  "retryCount": 2,
  "error": {
    "code": "AI_PROVIDER_TIMEOUT",
    "message": "AI provider belirlenen süre içinde cevap vermedi.",
    "retryable": true
  }
}
```

Kurallar:

* Sadece `FAILED` durumundaki project için kullanılmalıdır.
* Retry count maksimum `3` olmalıdır.
* Non-retryable hata tiplerinde retry yapılmamalıdır.
* Her retry yeni `AiGenerationRequest` oluşturmalıdır.
* Eski request overwrite edilmemelidir.

---

# 8. Review Action API

## 8.1 Review Başlatma

```http
POST /api/admin/content-projects/{contentProjectId}/start-review
```

Amaç:

Draft hazır olan project’i insan review sürecine alır.

Kurallar:

* Project `DRAFT_READY` olmalıdır.
* En az bir draft bulunmalıdır.
* Project `IN_REVIEW` olur.

Response:

```json
{
  "projectId": "content-project-id",
  "status": "IN_REVIEW"
}
```

---

## 8.2 Draft Onaylama

```http
POST /api/admin/content-projects/{contentProjectId}/approve
```

Request:

```json
{
  "draftId": "content-draft-id",
  "reviewNote": "İçerik kontrol edildi ve uygun bulundu."
}
```

Response:

```json
{
  "projectId": "content-project-id",
  "status": "APPROVED",
  "draft": {
    "id": "content-draft-id",
    "status": "APPROVED"
  }
}
```

Kurallar:

* Project `IN_REVIEW` olmalıdır.
* Draft mevcut olmalıdır.
* Draft statüsü `APPROVED` olur.
* Project statüsü `APPROVED` olur.
* Bu işlem insan onayı olarak kabul edilir.

---

## 8.3 Revizyon İsteme

```http
POST /api/admin/content-projects/{contentProjectId}/request-revision
```

Request:

```json
{
  "draftId": "content-draft-id",
  "feedbackType": "MAKE_CHILD_FRIENDLY",
  "reviewNote": "Metni çocukların anlayabileceği şekilde daha sade ve merak uyandırıcı hale getir."
}
```

Response:

```json
{
  "projectId": "content-project-id",
  "status": "REVISION_REQUESTED",
  "draft": {
    "id": "content-draft-id",
    "status": "REVISION_NEEDED"
  }
}
```

Kurallar:

* Project `IN_REVIEW` veya `DRAFT_READY` olabilir.
* `reviewNote` zorunlu olmalıdır.
* Revizyon retry değildir.
* Sonraki AI generation, human feedback context’i ile yapılmalıdır.

---

## 8.4 Draft Reddetme

```http
POST /api/admin/content-projects/{contentProjectId}/reject
```

Request:

```json
{
  "draftId": "content-draft-id",
  "reviewNote": "İçerik kültürel açıdan yetersiz ve yeniden ele alınmalı."
}
```

Response:

```json
{
  "projectId": "content-project-id",
  "status": "REJECTED",
  "draft": {
    "id": "content-draft-id",
    "status": "REJECTED"
  }
}
```

Kurallar:

* Project `IN_REVIEW` olmalıdır.
* İnsan reddi sonrası otomatik retry yapılmaz.
* Yeni üretim gerekiyorsa bilinçli yeni akış tasarlanmalıdır.

---

## 8.5 Ready For Publish Yapma

```http
POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish
```

Amaç:

İnsan onayından geçmiş project’i yayına hazır hale getirir.

Response:

```json
{
  "projectId": "content-project-id",
  "status": "READY_FOR_PUBLISH"
}
```

Kurallar:

* Project `APPROVED` olmalıdır.
* Onaylanmış draft bulunmalıdır.
* AI çıktısı insan onayı olmadan bu statüye geçemez.

---

## 8.6 Project İptal Etme

```http
POST /api/admin/content-projects/{contentProjectId}/cancel
```

Request:

```json
{
  "reason": "Bu içerik çalışması artık gerekli değil."
}
```

Response:

```json
{
  "projectId": "content-project-id",
  "status": "CANCELLED"
}
```

Kurallar:

* `CREATED`, `FAILED`, `DRAFT_READY`, `REVISION_REQUESTED` durumlarında iptal edilebilir.
* `READY_FOR_PUBLISH` project iptal edilmemelidir.
* `CANCELLED` project için AI generation yapılamaz.

---

# 9. Content Draft API

## 9.1 Draft Detay

```http
GET /api/admin/content-drafts/{contentDraftId}
```

Response:

```json
{
  "id": "content-draft-id",
  "contentProjectId": "content-project-id",
  "title": "Ankara Kalesi",
  "body": "Ankara Kalesi, başkentin tarihî dokusunu yansıtan...",
  "summary": "Ankara Kalesi hakkında kısa tanıtım.",
  "status": "GENERATED",
  "generatedBy": "AI_CONTENT_AGENT",
  "warnings": [],
  "createdAt": "2026-06-28T12:05:00Z",
  "updatedAt": "2026-06-28T12:05:00Z"
}
```

---

## 9.2 Draft Güncelleme

```http
PUT /api/admin/content-drafts/{contentDraftId}
```

Amaç:

İnsan editörün draft üzerinde manuel düzenleme yapmasını sağlar.

Request:

```json
{
  "title": "Ankara Kalesi",
  "body": "Düzenlenmiş içerik metni...",
  "summary": "Düzenlenmiş kısa özet."
}
```

Response:

```json
{
  "id": "content-draft-id",
  "title": "Ankara Kalesi",
  "body": "Düzenlenmiş içerik metni...",
  "summary": "Düzenlenmiş kısa özet.",
  "status": "EDITED"
}
```

Kurallar:

* Draft `GENERATED` veya `REVISION_NEEDED` durumunda düzenlenebilir.
* Manuel düzenleme sonrası draft `EDITED` olur.
* Draft güncelleme project’i otomatik `APPROVED` yapmaz.

---

# 10. AI Generation Request API

MVP’de ayrı detay endpoint’i zorunlu değildir.

Ancak debugging için önerilir.

## 10.1 Project AI Request History

```http
GET /api/admin/content-projects/{contentProjectId}/ai-generation-requests
```

Response:

```json
{
  "items": [
    {
      "id": "ai-request-id",
      "attemptNumber": 1,
      "status": "FAILED",
      "errorType": "AI_PROVIDER_TIMEOUT",
      "errorMessage": "AI provider belirlenen süre içinde cevap vermedi.",
      "providerName": "mock-provider",
      "modelName": "mock-model",
      "durationMs": 30000,
      "createdAt": "2026-06-28T12:00:00Z",
      "completedAt": "2026-06-28T12:00:30Z"
    }
  ]
}
```

Not:

* Raw prompt ve raw response MVP’de UI’a gösterilmemelidir.
* Güvenlik ve veri boyutu nedeniyle ayrıca değerlendirilecektir.

---

# 11. Standart Error Response

Tüm API’lerde ortak hata response formatı kullanılmalıdır.

Önerilen format:

```json
{
  "code": "INVALID_PROJECT_STATUS",
  "message": "Bu statüde AI draft üretimi başlatılamaz.",
  "details": {
    "currentStatus": "READY_FOR_PUBLISH",
    "allowedStatuses": ["CREATED", "REVISION_REQUESTED"]
  },
  "timestamp": "2026-06-28T12:00:00Z",
  "path": "/api/admin/content-projects/content-project-id/generate-ai-draft"
}
```

## 11.1 Business Error Kodları

```text
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

## 11.2 AI Error Kodları

AI error kodları `docs/03-ai-platform/21-content-agent-contract.md` ile uyumlu olmalıdır:

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
UNKNOWN_AI_ERROR
```

---

# 12. HTTP Status Code Kullanımı

Önerilen HTTP status kullanımı:

| Durum                       |                                      HTTP Status |
| --------------------------- | -----------------------------------------------: |
| Başarılı create             |                                    `201 Created` |
| Başarılı read/update/action |                                         `200 OK` |
| Validation hatası           |                                `400 Bad Request` |
| Auth yok                    |                               `401 Unauthorized` |
| Yetki yok                   |                                  `403 Forbidden` |
| Kayıt bulunamadı            |                                  `404 Not Found` |
| Geçersiz status transition  |                                   `409 Conflict` |
| Retry limit aşıldı          |                                   `409 Conflict` |
| AI provider geçici hata     | `502 Bad Gateway` veya `503 Service Unavailable` |
| Beklenmeyen sunucu hatası   |                      `500 Internal Server Error` |

MVP’de AI provider hataları application error olarak `200` ile dönülmemelidir. Hata gerçekten başarısız akışsa uygun HTTP error kullanılmalıdır.

---

# 13. API Seviyesinde Status Transition Güvenliği

Frontend hiçbir zaman genel update endpoint’iyle status değiştirmemelidir.

Yanlış yaklaşım:

```http
PUT /api/admin/content-projects/{id}
{
  "status": "APPROVED"
}
```

Doğru yaklaşım:

```http
POST /api/admin/content-projects/{id}/approve
```

Böylece:

* Business rule backend’de çalışır.
* Geçersiz geçiş engellenir.
* Review note zorunlu kılınabilir.
* Audit/log eklemek kolaylaşır.
* Admin Panel basit kalır.

---

# 14. Admin Panel API Kullanım Akışı

## 14.1 Minimum MVP Akışı

```text
POST /api/admin/knowledge-items
POST /api/admin/knowledge-items/{id}/mark-ready
POST /api/admin/content-projects
POST /api/admin/content-projects/{id}/generate-ai-draft
GET  /api/admin/content-projects/{id}
POST /api/admin/content-projects/{id}/start-review
POST /api/admin/content-projects/{id}/approve
POST /api/admin/content-projects/{id}/mark-ready-for-publish
```

## 14.2 Revizyon Akışı

```text
POST /api/admin/content-projects/{id}/request-revision
POST /api/admin/content-projects/{id}/generate-ai-draft
GET  /api/admin/content-projects/{id}
```

## 14.3 Retry Akışı

```text
POST /api/admin/content-projects/{id}/retry-ai-draft
GET  /api/admin/content-projects/{id}
```

---

# 15. MVP Endpoint Listesi

İlk implementation için minimum endpoint listesi:

```text
POST /api/admin/knowledge-items
GET  /api/admin/knowledge-items
GET  /api/admin/knowledge-items/{knowledgeItemId}
PUT  /api/admin/knowledge-items/{knowledgeItemId}
POST /api/admin/knowledge-items/{knowledgeItemId}/mark-ready
POST /api/admin/knowledge-items/{knowledgeItemId}/archive

POST /api/admin/content-projects
GET  /api/admin/content-projects/{contentProjectId}
POST /api/admin/content-projects/{contentProjectId}/generate-ai-draft
POST /api/admin/content-projects/{contentProjectId}/retry-ai-draft
POST /api/admin/content-projects/{contentProjectId}/start-review
POST /api/admin/content-projects/{contentProjectId}/approve
POST /api/admin/content-projects/{contentProjectId}/request-revision
POST /api/admin/content-projects/{contentProjectId}/reject
POST /api/admin/content-projects/{contentProjectId}/mark-ready-for-publish

GET  /api/admin/content-drafts/{contentDraftId}
PUT  /api/admin/content-drafts/{contentDraftId}
```

Opsiyonel ama önerilen:

```text
GET /api/admin/content-projects
POST /api/admin/content-projects/{contentProjectId}/cancel
GET /api/admin/content-projects/{contentProjectId}/ai-generation-requests
```

---

# 16. Request Validation Kuralları

## 16.1 Knowledge Item

* `title` zorunlu
* `title` boş string olamaz
* `category` desteklenen değerlerden biri olmalı
* `city`, `district`, `locationText` opsiyonel
* `notes` uzunluk sınırı Engineering implementation’da belirlenmeli

## 16.2 Content Project

* `knowledgeItemId` zorunlu
* `contentPurpose` zorunlu
* `targetAudience` zorunlu
* `tone` zorunlu
* `language` zorunlu
* `language` MVP için `tr` olabilir
* Knowledge Item `READY` olmalı

## 16.3 Review Actions

* `draftId` zorunlu
* `reviewNote` approve için opsiyonel olabilir
* `reviewNote` revision ve reject için zorunlu olmalıdır
* `feedbackType` revision için zorunlu olmalıdır

---

# 17. Security ve Authorization Notları

MVP’de minimum rol yaklaşımı:

```text
ADMIN
EDITOR
VIEWER
```

## 17.1 ADMIN

Yapabilir:

* Tüm Knowledge Item işlemleri
* Tüm Content Project işlemleri
* AI generation
* Retry
* Ready for publish
* Cancel

## 17.2 EDITOR

Yapabilir:

* Project detay görüntüleme
* Draft görüntüleme
* Draft düzenleme
* Review başlatma
* Approve
* Request revision
* Reject

## 17.3 VIEWER

Yapabilir:

* Listeleme
* Detay görüntüleme

İlk implementation sadece `ADMIN` ile başlayabilir. Ancak API tasarımı ileride rol ayrımına engel olmamalıdır.

---

# 18. API Test Senaryoları

## 18.1 Happy Path

1. Knowledge Item oluştur.
2. Mark ready yap.
3. Content Project oluştur.
4. AI draft generation başlat.
5. Project detayında draft gör.
6. Review başlat.
7. Approve et.
8. Ready for publish yap.

Beklenen sonuç:

* Final project status `READY_FOR_PUBLISH`

## 18.2 Knowledge Item Ready Değil

1. Knowledge Item oluştur.
2. Ready yapmadan Content Project oluşturmayı dene.

Beklenen sonuç:

* `409 Conflict`
* Error code: `KNOWLEDGE_ITEM_NOT_READY`

## 18.3 Archived Knowledge Item

1. Knowledge Item oluştur.
2. Archive yap.
3. Content Project oluşturmayı dene.

Beklenen sonuç:

* `409 Conflict`
* Error code: `KNOWLEDGE_ITEM_ARCHIVED`

## 18.4 AI Failure

1. Content Project oluştur.
2. Mock AI provider timeout döndürsün.
3. AI draft generation başlat.

Beklenen sonuç:

* Project `FAILED`
* Error code: `AI_PROVIDER_TIMEOUT`
* Draft oluşmaz.

## 18.5 Retry Success

1. Project `FAILED` olsun.
2. Retry endpoint çağır.
3. Mock AI provider başarılı response döndürsün.

Beklenen sonuç:

* Project `DRAFT_READY`
* Draft oluşur.
* Retry count artar.

## 18.6 Retry Limit Exceeded

1. Project `FAILED` olsun.
2. Retry count `3` olsun.
3. Retry endpoint çağır.

Beklenen sonuç:

* `409 Conflict`
* Error code: `RETRY_LIMIT_EXCEEDED`

## 18.7 Human Revision

1. Draft üret.
2. Review başlat.
3. Request revision çağır.
4. Generate AI draft tekrar çağır.

Beklenen sonuç:

* Yeni draft oluşur.
* Human feedback AI context’ine dahil edilir.

## 18.8 Human Approval Without Review

1. Draft üret.
2. Review başlatmadan approve çağır.

Beklenen sonuç:

* `409 Conflict`
* Error code: `INVALID_PROJECT_STATUS`

---

# 19. Implementation Notları

Kod aşamasında dikkat edilecek noktalar:

* Action endpoint’ler ince controller, güçlü application service yaklaşımıyla yazılmalı.
* Controller business rule içermemeli.
* Request DTO’lar status alanı içermemeli.
* Response DTO’lar Admin Panel’in ihtiyaç duyduğu status ve aksiyon bilgisini içermeli.
* AI generation için önce mock gateway ile başlanmalı.
* Gerçek provider entegrasyonu workflow oturduktan sonra yapılmalı.
* API response’larında raw provider payload gösterilmemeli.
* Draft oluşturma sadece validation başarılıysa yapılmalı.

---

# 20. Kapsam Dışı

Bu API tasarımında şu konular kapsam dışıdır:

* Public content API
* Mobil uygulama içerik API’si
* Çoklu dil yayın API’si
* Görsel üretim API’si
* Video / storyboard API’si
* Audio guide gerçek ses dosyası üretimi
* Full audit log API’si
* Prompt management API’si
* Provider configuration API’si
* Analytics API

---

# 🎯 Amaç

Bu dokümanın amacı, FP-001 AI Assisted Content Creation feature’ı için Admin Panel ve Backend arasındaki API sözleşmesini Engineering seviyesinde netleştirmektir.

Bu doküman ile:

* Knowledge Item API yönü belirlenir.
* Content Project API yönü belirlenir.
* Content Draft API yönü belirlenir.
* AI generation action endpoint’leri tanımlanır.
* Review action endpoint’leri tanımlanır.
* Retry ve revision API seviyesinde ayrıştırılır.
* Status transition güvenliği sağlanır.
* Error response standardı belirlenir.
* Admin Panel’in kullanacağı minimum API akışı tanımlanır.

---

# 👤 Aktörler

## Admin

API üzerinden:

* Knowledge Item oluşturur.
* Knowledge Item’ı ready yapar.
* Content Project oluşturur.
* AI draft generation başlatır.
* Retry yapar.
* Ready for publish kararı verir.

## Editor

API üzerinden:

* Draft görüntüler.
* Draft düzenler.
* Review başlatır.
* Onaylar.
* Revizyon ister.
* Reddeder.

## Viewer

API üzerinden:

* Liste ve detay görüntüler.
* Aksiyon endpoint çağırmaz.

## Backend System

API çağrılarını alır, business rule uygular, status transition’ları yönetir, AI boundary ile konuşur ve response döndürür.

## Admin Panel

API tüketicisidir.

Admin Panel:

* Doğrudan status set etmez.
* Action endpoint çağırır.
* Hataları kullanıcıya anlaşılır gösterir.
* AI provider detaylarını bilmez.

---

# 📄 İş Akışı

## Happy Path API İş Akışı

1. Admin `POST /knowledge-items` çağırır.
2. Backend Knowledge Item’ı `DRAFT` olarak oluşturur.
3. Admin `POST /knowledge-items/{id}/mark-ready` çağırır.
4. Backend Knowledge Item’ı `READY` yapar.
5. Admin `POST /content-projects` çağırır.
6. Backend Content Project’i `CREATED` olarak oluşturur.
7. Admin `POST /content-projects/{id}/generate-ai-draft` çağırır.
8. Backend AI generation başlatır.
9. AI response başarılıysa draft oluşturulur.
10. Project `DRAFT_READY` olur.
11. Admin veya Editor `POST /content-projects/{id}/start-review` çağırır.
12. Project `IN_REVIEW` olur.
13. Editor `POST /content-projects/{id}/approve` çağırır.
14. Draft ve Project onaylanır.
15. Admin `POST /content-projects/{id}/mark-ready-for-publish` çağırır.
16. Project `READY_FOR_PUBLISH` olur.

---

# 🤖 AI Agent Görevleri

API seviyesinde AI Content Agent’ın görevleri:

* Backend tarafından oluşturulan request context’ini işlemek
* Draft’a dönüşebilecek normalize response üretmek
* Hata durumunda contract’a uygun hata döndürmek
* Revision sırasında human feedback’i dikkate almak
* Provider detaylarını API response’a sızdırmamak

AI Content Agent API’de doğrudan görünmez.

Admin Panel yalnızca backend API ile konuşur.

---

# 👨 İnsan Müdahalesi

API tasarımında insan müdahalesi şu aksiyonlarla temsil edilir:

* `mark-ready`
* `start-review`
* `approve`
* `request-revision`
* `reject`
* `mark-ready-for-publish`
* `retry-ai-draft`

Özellikle:

* `approve` insan onayıdır.
* `request-revision` insan geri bildirimidir.
* `reject` insan reddidir.
* `mark-ready-for-publish` final hazırlık kararıdır.

Bu aksiyonlar otomatik AI sürecine bırakılmamalıdır.

---

# 📊 Status Geçişleri

## Knowledge Item

```text
POST /knowledge-items
  → DRAFT

POST /knowledge-items/{id}/mark-ready
  → READY

POST /knowledge-items/{id}/archive
  → ARCHIVED
```

## Content Project

```text
POST /content-projects
  → CREATED

POST /content-projects/{id}/generate-ai-draft
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

```text
POST /content-projects/{id}/retry-ai-draft
  → AI_REQUESTED
  → AI_PROCESSING
  → DRAFT_READY
```

```text
POST /content-projects/{id}/start-review
  → IN_REVIEW

POST /content-projects/{id}/approve
  → APPROVED

POST /content-projects/{id}/request-revision
  → REVISION_REQUESTED

POST /content-projects/{id}/reject
  → REJECTED

POST /content-projects/{id}/mark-ready-for-publish
  → READY_FOR_PUBLISH

POST /content-projects/{id}/cancel
  → CANCELLED
```

---

# ⚠ Hata Senaryoları

## Geçersiz Status Transition

Örnek:

`CREATED` project için `mark-ready-for-publish` çağrılır.

Beklenen:

```json
{
  "code": "INVALID_PROJECT_STATUS",
  "message": "Bu project mevcut statüde yayına hazır hale getirilemez."
}
```

## Knowledge Item Hazır Değil

Örnek:

`DRAFT` Knowledge Item üzerinden project oluşturulur.

Beklenen:

```json
{
  "code": "KNOWLEDGE_ITEM_NOT_READY",
  "message": "Bu Knowledge Item içerik üretimi için hazır değil."
}
```

## AI Validation Hatası

Örnek:

AI response body boş gelir.

Beklenen:

```json
{
  "code": "EMPTY_AI_RESPONSE",
  "message": "AI geçerli bir içerik üretmedi."
}
```

## Retry Limit Aşımı

Beklenen:

```json
{
  "code": "RETRY_LIMIT_EXCEEDED",
  "message": "Maksimum retry limitine ulaşıldı. Manuel müdahale gerekiyor."
}
```

---

# 🔄 Retry Kuralları

API seviyesinde retry kuralları:

* Retry endpoint ayrı olmalıdır.
* Retry sadece `FAILED` project için çalışmalıdır.
* Retry count backend tarafından kontrol edilmelidir.
* Retry count frontend’den gönderilmemelidir.
* Retry count maksimum `3` olmalıdır.
* Non-retryable hata varsa `409 Conflict` dönmelidir.
* Retry başarılı olursa yeni draft oluşur.
* Retry başarısız olursa project tekrar `FAILED` kalır.
* Retry eski AI request kaydını ezmez.

---

# 📦 Çıktılar

Bu dokümanın çıktıları:

* Knowledge Item API tasarım yönü
* Content Project API tasarım yönü
* Content Draft API tasarım yönü
* AI generation endpoint tasarımı
* Retry endpoint tasarımı
* Review endpoint tasarımı
* Error response standardı
* HTTP status code yaklaşımı
* Admin Panel API kullanım akışı
* API test senaryoları
* Kod aşamasına geçmeden önce gerekli endpoint kapsamı

---

# 🤖 IDE AI Görevi

Bu aşamada IDE AI hâlâ kod yazmayacaktır.

IDE AI’ın görevi:

1. `docs/05-engineering/19-content-pipeline-api-design.md` dosyasını oluşturmak.
2. Bu dokümanı verilen içerikle doldurmak.
3. Mevcut `docs/05-engineering` dosyalarını yeniden numaralandırmamak.
4. Controller, DTO, service, entity, repository veya migration oluşturmamak.
5. API endpoint’lerini kod olarak yazmamak.
6. Bu dokümanı sadece Engineering API design dokümanı olarak eklemek.
7. Sonraki implementation plan tamamlanmadan koda geçmemek.

IDE AI’a verilecek net komut:

```text
Create the file docs/05-engineering/19-content-pipeline-api-design.md with the provided content.
Do not renumber existing docs/05-engineering files.
Do not generate code.
Do not create controllers, DTOs, services, entities, repositories, migrations, frontend pages, or API endpoints yet.
Only add this Engineering API design documentation file.
```

---

# ✅ Tamamlanma Kriterleri

Bu doküman aşağıdaki kriterler sağlandığında tamamlanmış kabul edilir:

* Knowledge Item endpoint yönü tanımlandı.
* Content Project endpoint yönü tanımlandı.
* Content Draft endpoint yönü tanımlandı.
* AI generation action endpoint tanımlandı.
* Retry endpoint ayrı tanımlandı.
* Review action endpoint’leri tanımlandı.
* Status transition güvenliği API seviyesinde korundu.
* Error response standardı belirlendi.
* HTTP status code yaklaşımı belirlendi.
* Admin Panel happy path API akışı tanımlandı.
* Retry ve revision API seviyesinde ayrıştırıldı.
* API test senaryoları belirlendi.
* Kod üretimine geçilmedi.
* Sonraki doküman belirlendi.

---

# 🔍 Kod İnceleme Kontrol Listesi

Kod aşamasında bu dokümana göre şu kontroller yapılmalıdır:

* Controller’lar business rule içermiyor mu?
* Status doğrudan update edilmiyor mu?
* Action endpoint’ler application service üzerinden çalışıyor mu?
* Request DTO’larda status alanı yok mu?
* `approve`, `reject`, `request-revision` ayrı endpoint’ler mi?
* Retry endpoint revision endpoint’ten ayrı mı?
* AI failure durumunda doğru error response dönüyor mu?
* Draft validation başarısızsa draft oluşturulmuyor mu?
* `READY_FOR_PUBLISH` sadece `APPROVED` sonrası mümkün mü?
* Raw provider response API’ye sızmıyor mu?
* HTTP status code’lar doğru kullanılıyor mu?
* Admin Panel’in ihtiyaç duyduğu bilgiler response’larda var mı?
* Endpoint’ler MVP kapsamını gereksiz büyütmüyor mu?

---

# ➡ Sonraki Doküman

Sıradaki doküman:

```text
docs/05-engineering/20-content-pipeline-implementation-plan.md
```

Bu dokümanda kod üretimine geçmeden önce implementation plan netleştirilecektir.

Ele alınacak konular:

* Kod üretim fazları
* Dosya/package sırası
* Backend implementation sırası
* Mock AI gateway stratejisi
* Migration sırası
* Admin Panel implementation sırası
* Test sırası
* IDE AI’a verilecek adım adım görevler
* Kod üretimine başlama kriterleri

Bu doküman tamamlandıktan sonra kontrollü şekilde ilk kod üretim sprintine geçilebilir.
