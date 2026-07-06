# FP-013 Task 04 — Review Handoff + Safe Preview API Integration for Admin Review

## 1. Amaç

FP-013 Task 02-03'te oluşturulan `ReviewHandoffService` ve `AdminPreviewService` temellerini
kontrollü internal admin API endpoint'leri üzerinden expose etmek ve admin-web konsoluna
güvenli preview (önizleme) paneli entegre etmek.

## 2. Kapsam

| Bileşen              | Değişiklik Türü | Açıklama                                     |
|----------------------|-----------------|----------------------------------------------|
| AdminReviewHandoffRequest  | Yeni DTO       | Admin-facing handoff istek DTO'su              |
| AdminReviewHandoffResponse | Yeni DTO       | Admin-facing handoff sonuç DTO'su              |
| AdminReviewDtoMapper       | Genişletme     | toHandoffRequest + toHandoffResponse eklenme   |
| AdminReviewController      | Genişletme     | POST /handoff + GET /preview ekleme            |
| ReviewPreviewPanel.tsx     | Yeni bileşen   | Güvenli preview paneli                         |
| ReviewWorklist.tsx         | Güncelleme     | Preview panel + PACKAGE hedef tipi ekleme      |
| reviewApi.ts               | Genişletme     | handoffReview + getPreview metotları           |
| types/index.ts             | Genişletme     | FP-013 handoff/preview tipleri                 |
| Frontend testler           | Güncelleme     | 14 fonksiyon, preview panel, worklist testleri |
| Backend testler            | Güncelleme + Yeni | 6 mevcut test güncelleme, 1 yeni test        |

## 3. Güvenlik Sınırları

### 3.1 Yasaklanan Alanlar / Davranışlar

Aşağıdakiler hiçbir DTO, endpoint, bileşen veya testte mevcut DEĞİLDİR:

- Provider URL, storage key, bucket/path bilgileri
- Raw payload, base64, rawImageBytes, rawContent
- Secret, apiKey, config, stack trace
- Public/signed URL oluşturma/döndürme
- Publish/export/CDN semantiği
- Otomatik onay (APPROVED_FOR_INTERNAL_USE handoff ile oluşturulamaz)
- dangerouslySetInnerHTML, innerHTML

### 3.2 Korunan Kurallar

- Human review gate korunur — AI_GENERATED onay anlamına gelmez
- Handoff idempotent — birden fazla çağrı güvenlidir
- Preview read-only — onay anlamına gelmez
- APPROVED_FOR_INTERNAL_USE publish/export onayı DEĞİLDİR
- IMAGE_ASSET preview: sadece etiket/durum bilgisi, görsel bayt/URL yok

## 4. Backend API Değişiklikleri

### 4.1 Yeni Endpoint'ler

| Metot | Path                                    | Açıklama                          |
|-------|----------------------------------------|-----------------------------------|
| POST  | /api/admin/reviews/handoff             | İdempotent handoff — otomatik onay yok |
| GET   | /api/admin/reviews/{targetType}/{targetId}/preview | Güvenli metadata preview |

### 4.2 Endpoint Envanteri (14 toplam)

| #  | Metot | Path                                        | FP     |
|----|-------|---------------------------------------------|--------|
| 1  | POST  | /api/admin/reviews/submit                   | FP-012 |
| 2  | POST  | /api/admin/reviews/start                    | FP-012 |
| 3  | POST  | /api/admin/reviews/request-changes          | FP-012 |
| 4  | POST  | /api/admin/reviews/approve-internal         | FP-012 |
| 5  | POST  | /api/admin/reviews/reject                   | FP-012 |
| 6  | POST  | /api/admin/reviews/return-to-draft          | FP-012 |
| 7  | POST  | /api/admin/reviews/note                     | FP-012 |
| 8  | POST  | /api/admin/reviews/handoff                  | FP-013 |
| 9  | GET   | /api/admin/reviews/worklist                 | FP-012 |
| 10 | GET   | /api/admin/reviews/{t}/{id}/context         | FP-012 |
| 11 | GET   | /api/admin/reviews/{t}/{id}/state           | FP-012 |
| 12 | GET   | /api/admin/reviews/{t}/{id}/audit           | FP-012 |
| 13 | GET   | /api/admin/reviews/{t}/{id}/actions         | FP-012 |
| 14 | GET   | /api/admin/reviews/{t}/{id}/preview         | FP-013 |

### 4.3 Exception Handling

| Exception Sınıfı                | HTTP Status | Kullanım         |
|--------------------------------|-------------|------------------|
| ReviewApplicationException     | 404         | Mevcut           |
| ReviewTransitionViolationException | 409     | Mevcut           |
| ReviewHandoffException         | 422         | Yeni (FP-013)    |
| AdminPreviewException          | 404         | Yeni (FP-013)    |
| IllegalArgumentException       | 400         | Mevcut           |

## 5. DTO Yapıları

### 5.1 AdminReviewHandoffRequest

```java
targetType: ReviewTargetType       // Zorunlu
targetId: UUID                     // Zorunlu
source: ReviewHandoffSource        // Zorunlu
actorRole: ReviewActorRole         // Zorunlu
actorReference: String             // Zorunlu
safeTitle: String                  // Opsiyonel
safeSummary: String                // Opsiyonel
safeReasonCode: String             // Opsiyonel
safeMessage: String                // Opsiyonel
```

### 5.2 AdminReviewHandoffResponse

```java
targetType: ReviewTargetType
targetId: UUID
previousState: ReviewState
currentState: ReviewState
created: boolean
updated: boolean
preserved: boolean
auditEventType: ReviewAuditEventType
safeReasonCode: String
safeMessage: String
```

### 5.3 SafeAdminPreview (Doğrudan Kullanım)

Task 03'te oluşturulan `SafeAdminPreview` ayrı DTO'ya dönüştürülmeden
preview endpoint response body olarak doğrudan kullanılır — zaten güvenli ve immutable.

## 6. Frontend Bileşenleri

### 6.1 ReviewPreviewPanel.tsx (Yeni)

- Props: `targetType`, `targetId`
- `reviewApi.getPreview()` ile preview yükleme
- Güvenli metin gösterimi (`safeText()`)
- IMAGE_ASSET: sadece etiket/durum gösterimi
- Yükleniyor / hata / kullanılamıyor durumları
- `data-testid` attributes

### 6.2 ReviewWorklist.tsx (Güncelleme)

- `ReviewPreviewPanel` import ve render
- `PACKAGE` hedef tipi TARGET_TYPE_OPTIONS'a eklendi
- Preview paneli konsol üzerinde gösteriliyor

### 6.3 reviewApi.ts (Güncelleme)

- `handoffReview(data)` — POST /admin/reviews/handoff
- `getPreview(targetType, targetId)` — GET /admin/reviews/{t}/{id}/preview
- Toplam 14 export fonksiyon

### 6.4 types/index.ts (Güncelleme)

Yeni tipler:
- `ReviewHandoffSource` union type
- `AdminReviewHandoffRequest` interface
- `AdminReviewHandoffResponse` interface
- `AdminPreviewMode` union type
- `AdminPreviewAvailability` union type
- `SafeAdminPreview` interface
- `PACKAGE` → `ReviewTargetType`'a eklendi

## 7. Test Kapsamı

### 7.1 Backend Yeni Test

`AdminReviewHandoffPreviewControllerTest.java`:
- A. Handoff happy paths (create, preserve NEEDS_REVIEW/IN_REVIEW/APPROVED/REJECTED)
- B. Handoff validation/failure (5 validasyon + 2 exception handler)
- C. Preview happy paths (5 target type + missing target)
- D. Safe response body regression (forbidden field kontrolü)
- E. Endpoint inventory regression (14 total, forbidden path)
- F. Boundary regression (no migration, storage, provider, orchestration)

### 7.2 Backend Mevcut Test Güncellemeleri (6 dosya)

Endpoint sayısı: 12 → 14, constructor: 1 param → 3 param

### 7.3 Frontend Testler

- `reviewApi.test.ts`: 14 fonksiyon, handoffReview/getPreview kontrol
- `ReviewPreviewPanel.test.ts`: Güvenlik regresyon, forbidden string kontrolü
- `ReviewWorklist.test.ts`: PACKAGE, preview panel entegrasyonu

## 8. Migration Durumu

**Migration YOKTUR** — schema değişikliği yapılmadı.

## 9. Orchestration Durumu

**Orchestration entegrasyonu YOKTUR** — handoff endpoint admin-only ve manual.
Orchestration entegrasyonu sonraki task'larda planlanmaktadır.

## 10. Sonraki Adımlar

- Task 05+: Orchestration → handoff otomatik entegrasyonu
- Generation service'lerden handoff çağrısı (generation tamamlandığında)
- Preview paneli zenginleştirme (scene/beat detayları)

## 11. Dosya Listesi

### Backend (Yeni)
- `AdminReviewHandoffRequest.java`
- `AdminReviewHandoffResponse.java`
- `AdminReviewHandoffPreviewControllerTest.java`

### Backend (Güncelleme)
- `AdminReviewDtoMapper.java`
- `AdminReviewController.java`
- `AdminReviewControllerTest.java`
- `AdminReviewApiBoundaryRegressionTest.java`
- `AdminReviewAvailabilityTest.java`
- `AdminReviewWorklistTest.java`
- `ReviewHandoffServiceTest.java`
- `AdminPreviewServiceTest.java`

### Frontend (Yeni)
- `ReviewPreviewPanel.tsx`
- `ReviewPreviewPanel.test.ts`

### Frontend (Güncelleme)
- `reviewApi.ts`
- `types/index.ts`
- `reviewApi.test.ts`
- `ReviewWorklist.tsx`
- `ReviewWorklist.test.ts`

## 12. Commit Bilgileri

```
feat(FP-013): Task 04 — Review Handoff + Safe Preview API Integration

- POST /api/admin/reviews/handoff (idempotent, no auto-approval)
- GET /api/admin/reviews/{t}/{id}/preview (safe metadata only)
- AdminReviewHandoffRequest/Response DTOs
- AdminReviewDtoMapper: toHandoffRequest + toHandoffResponse
- ReviewPreviewPanel.tsx: safe preview panel component
- ReviewWorklist: preview integration + PACKAGE target type
- reviewApi: handoffReview + getPreview (14 total functions)
- Frontend types: handoff/preview types + PACKAGE
- AdminReviewHandoffPreviewControllerTest (categories A-F)
- 6 existing test updates: endpoint count 12→14
- No migration, no orchestration, no generation change
- No provider/storage/publish/export/public URL/signed URL
```

## 13. Bağımlılıklar

| Bağımlılık         | Versiyon | Durum     |
|-------------------|----------|-----------|
| ReviewHandoffService | FP-013 T02 | Mevcut |
| AdminPreviewService  | FP-013 T03 | Mevcut |
| SafeAdminPreview     | FP-013 T03 | Mevcut |
| ReviewTextSafety     | FP-012     | Mevcut |

## 14. Risk Değerlendirmesi

| Risk                                  | Seviye | Mitigation                              |
|---------------------------------------|--------|----------------------------------------|
| Preview'de asset bytes/URL sızması    | Yüksek | SafeAdminPreview immutable, forbidden field regression test |
| Handoff otomatik onay                 | Yüksek | ReviewHandoffService never auto-approves |
| Endpoint sayısı kayması               | Düşük  | 7 test dosyasında 14 assertion          |
| Frontend unsafe HTML injection        | Orta   | safeText(), no dangerouslySetInnerHTML  |
