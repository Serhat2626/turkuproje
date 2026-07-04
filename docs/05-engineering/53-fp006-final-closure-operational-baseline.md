# FP-006 — Final Closure & Operational Baseline

## 1. Status

| Alan | Değer |
|------|-------|
| **FP-006 Durumu** | **CLOSED** |
| **Kapanış Tarihi** | 2026-07-04 |
| **Kapanış Task** | Task 14 — Final Closure & Operational Baseline |
| **Kapanış Türü** | Dokümantasyon / operasyonel baseline |
| **Branch** | `dev` |
| **Son Validasyon** | Local smoke ✅ + Staging smoke ✅ |
| **Backend Test** | 979/979 pass |
| **Frontend Test** | 134/134 pass |
| **TypeScript** | Clean |
| **Frontend Build** | Success |

---

## 2. Feature Özeti

FP-006, **görsel varlık planlama ve image generation prompt handoff** özelliğini tamamlamıştır.

### Teslim Edilenler

- **Görsel varlık planlama workflow'u** — APPROVED StoryboardDraft kaynak olarak kullanılır
- **AI-destekli görsel prompt üretimi** — sahne bazlı ve thumbnail için image-ready prompt metinleri
- **Sahne bazlı image prompt planı** — her StoryboardScene için `englishImagePrompt`, `turkishVisualContext`, `negativePrompt`, stil/kompozisyon/kamera/ışık/kültürel yönlendirme
- **Thumbnail prompt planı** — kapak görseli için optimize edilmiş prompt
- **İnsan review/edit/approve/reject workflow'u** — AI çıktısı asla otomatik onaylanmaz
- **Admin UI entegrasyonu** — VisualAssetWorkflowSection bileşeni
- **Mock provider** — deterministik, local/staging validasyon için
- **OpenAI chat/text provider** — gpt-4o-mini ile görsel plan prompt üretimi (text completion, image API değil)
- **Local ve staging validasyon** — 31/31 local + 32/32 staging API smoke test

### Önemli Sınırlama

> **FP-006, prompt planlama ve handoff özelliğidir. Gerçek image generation değildir.**
>
> Bu özellik, image generation tool'larına (DALL-E, Midjourney, Stable Diffusion vb.) input olarak kullanılacak prompt metinlerini üretir, düzenler ve onaylar. Görsel dosya üretimi, media storage, video/audio generation veya publishing FP-006 kapsamında değildir.

---

## 3. Product Boundary

### Kapsam İçi (In Scope)

| Bileşen | Açıklama |
|---------|----------|
| `VisualAssetPlan` | Görsel varlık plan aggregate |
| `SceneVisualPlan` | Sahne bazlı image prompt planı |
| `ThumbnailPlan` | Thumbnail prompt planı |
| `englishImagePrompt` | Image generation tool'a verilecek İngilizce prompt (ürün çıktısı) |
| `turkishVisualContext` | Görsel bağlam açıklaması (ürün çıktısı) |
| `negativePrompt` | Negatif prompt (ürün çıktısı) |
| `styleDirective` / `compositionNotes` / `cameraFraming` / `lightingNotes` / `culturalDetails` | Görsel yönlendirme alanları |
| `thumbnailPrompt` | Thumbnail image prompt (ürün çıktısı) |
| Edit / review / approve / reject | İnsan review workflow'u |
| One-approved-per-storyboard | Tekil onay kuralı |
| Mock provider | Deterministik test provider |
| OpenAI text provider | Chat completion ile prompt üretimi |
| Admin UI | VisualAssetWorkflowSection |

### Kapsam Dışı (Out of Scope)

| Bileşen | Durum |
|---------|-------|
| DALL-E image generation | ❌ FP-006'da yok |
| OpenAI Image API | ❌ FP-006'da yok |
| Midjourney / Stable Diffusion | ❌ FP-006'da yok |
| Media upload / storage | ❌ FP-006'da yok |
| `MediaAsset` entity | ❌ FP-006'da yok |
| `GeneratedAsset` entity | ❌ FP-006'da yok |
| `imageUrl` / storage object key | ❌ FP-006'da yok |
| Video / audio generation | ❌ FP-006'da yok |
| Publishing / upload / scheduling | ❌ FP-006'da yok |
| `READY_FOR_PUBLISH` | ❌ FP-006'da yok |
| Generic status update endpoint | ❌ FP-006'da yok |
| Analytics / quality review | ❌ FP-006'da yok |

---

## 4. Architecture Baseline

### Veri Akışı

```
APPROVED StoryboardDraft (FP-003)
    │
    ▼
VisualAssetPlanGenerationService
    │
    ▼
AiVisualAssetAgentGateway (interface)
    │
    ├── MockAiVisualAssetAgentGateway  (ai.provider=mock)
    └── OpenAiVisualAssetAgentGateway  (ai.provider=openai)
    │
    ▼
VisualAssetPlan aggregate
    ├── SceneVisualPlan (per scene)
    └── ThumbnailPlan
    │
    ▼
AdminVisualAssetPlanController (REST API)
    │
    ▼
Admin Web — VisualAssetWorkflowSection
    │
    ▼
İnsan review / edit / approve / reject
```

### Feature Bağımlılıkları

| İlişki | Durum |
|--------|-------|
| FP-003 StoryboardDraft | ✅ Kaynak veri — APPROVED StoryboardDraft gerekli |
| FP-004 PlatformPackageDraft | ❌ Hard dependency yok |
| FP-005 Export | ❌ Hard dependency yok |
| Gelecek platform adaptasyonu | Mümkün ama FP-006'da yok |

---

## 5. Backend Baseline

### Domain Katmanı

| Sınıf | Paket | Açıklama |
|-------|-------|----------|
| `VisualAssetPlanStatus` | `visualasset.entity` | Status enum (AI_GENERATED, NEEDS_REVIEW, APPROVED, REJECTED, FAILED) |
| `VisualAssetPlan` | `visualasset.entity` | Ana aggregate entity |
| `SceneVisualPlan` | `visualasset.entity` | Sahne bazlı prompt planı |
| `ThumbnailPlan` | `visualasset.entity` | Thumbnail prompt planı |
| `VisualAssetPlanRepository` | `visualasset.repository` | JPA repository |
| `V8__fp006_visual_asset_planning_tables.sql` | `db/migration` | Flyway V8 migration |

### AI Contract / Provider

| Sınıf | Paket | Açıklama |
|-------|-------|----------|
| `VisualAssetPlanAgentRequest` | `ai.contract` | AI gateway request DTO |
| `VisualAssetSceneInput` | `ai.contract` | Scene input DTO |
| `VisualAssetThumbnailInput` | `ai.contract` | Thumbnail input DTO |
| `VisualAssetPlanAgentResponse` | `ai.contract` | AI gateway response DTO |
| `VisualAssetSceneOutput` | `ai.contract` | Scene output DTO |
| `VisualAssetThumbnailOutput` | `ai.contract` | Thumbnail output DTO |
| `AiVisualAssetAgentGateway` | `ai.gateway` | Provider-agnostic gateway interface |
| `MockAiVisualAssetAgentGateway` | `ai.gateway.mock` | Deterministik mock provider |
| `OpenAiVisualAssetAgentGateway` | `ai.gateway.openai` | OpenAI chat/text provider |
| `OpenAiVisualAssetPromptBuilder` | `ai.gateway.openai` | System/user prompt builder |
| `OpenAiVisualAssetResponseParser` | `ai.gateway.openai` | JSON response parser |

### Service Katmanı

| Sınıf | Paket | Açıklama |
|-------|-------|----------|
| `VisualAssetPlanGenerationService` | `visualasset.service` | Plan üretim servisi |
| `VisualAssetPlanGenerationResult` | `visualasset.service` | Üretim sonuç DTO |
| `VisualAssetPlanQueryService` | `visualasset.service` | List/detail sorgulama |
| `VisualAssetPlanReviewService` | `visualasset.service` | Edit/approve/reject işlemleri |
| `VisualAssetPlanReviewResult` | `visualasset.service` | Review sonuç DTO |
| `UpdateVisualAssetPlanCommand` | `visualasset.service` | Parent edit command |
| `UpdateSceneVisualPlanCommand` | `visualasset.service` | Scene edit command |
| `UpdateThumbnailPlanCommand` | `visualasset.service` | Thumbnail edit command |

### REST Katmanı

| Sınıf | Paket | Açıklama |
|-------|-------|----------|
| `AdminVisualAssetPlanController` | `visualasset.controller` | Admin REST controller |
| Request/Response DTO'lar | `visualasset.controller.dto` | API DTO'ları |

---

## 6. REST API Baseline

### Endpoint Tablosu

| Method | Path | Açıklama |
|--------|------|----------|
| `POST` | `/api/admin/storyboard-drafts/{id}/visual-asset-plans/generate` | Görsel varlık planı üret |
| `GET` | `/api/admin/storyboard-drafts/{id}/visual-asset-plans` | Storyboard'a ait planları listele |
| `GET` | `/api/admin/content-projects/{id}/visual-asset-plans` | Content project'e ait planları listele |
| `GET` | `/api/admin/visual-asset-plans/{planId}` | Plan detayı (scenePlans + thumbnailPlan) |
| `PATCH` | `/api/admin/visual-asset-plans/{planId}` | Parent plan düzenle |
| `PATCH` | `/api/admin/visual-asset-plans/{planId}/scenes/{sceneNumber}` | Sahne prompt düzenle |
| `PATCH` | `/api/admin/visual-asset-plans/{planId}/thumbnail` | Thumbnail prompt düzenle |
| `POST` | `/api/admin/visual-asset-plans/{planId}/approve` | Planı onayla |
| `POST` | `/api/admin/visual-asset-plans/{planId}/reject` | Planı reddet (reason zorunlu) |

### Olmayan Endpoint'ler

FP-006'da aşağıdaki endpoint'ler kasıtlı olarak **yoktur**:

- ❌ Generic status update endpoint (`PUT .../status`)
- ❌ Image generation endpoint
- ❌ Upload / publish / schedule endpoint
- ❌ Media asset endpoint
- ❌ READY_FOR_PUBLISH endpoint

---

## 7. Frontend Baseline

### Admin Web Deliverables

| Dosya | Açıklama |
|-------|----------|
| `src/api/visualAssetApi.ts` | API client (generate, list, detail, edit, approve, reject) |
| `src/components/visualasset/VisualAssetWorkflowSection.tsx` | Ana workflow bileşeni |
| `src/types/index.ts` | FP-006 TypeScript type tanımları |
| ContentProjectsPage entegrasyonu | VAP bölümü entegre edildi |
| `src/api/__tests__/visualAssetApi.test.ts` | API client testleri (24 test) |
| `src/components/visualasset/__tests__/VisualAssetWorkflowSection.test.tsx` | UI testleri (29 test) |

### UI Yetenekleri

- ✅ APPROVED storyboard'dan görsel plan üretme
- ✅ Plan listesi görüntüleme
- ✅ Plan detayı (sahne kartları + thumbnail kartı)
- ✅ `englishImagePrompt` kopyalama butonu
- ✅ `thumbnailPrompt` kopyalama butonu
- ✅ Parent plan / sahne prompt / thumbnail prompt düzenleme
- ✅ Onay ve red aksiyonları
- ✅ Status badge'leri
- ✅ Güvenli hata mesajları
- ✅ Non-approved storyboard'da generate butonu gizli/devre dışı

### UI'da Olmayan Elementler

- ❌ Generate Image butonu
- ❌ Upload Image butonu
- ❌ Media Library butonu
- ❌ Publish butonu
- ❌ Schedule butonu
- ❌ READY_FOR_PUBLISH aksiyonu
- ❌ Generic status dropdown
- ❌ Gerçek görsel önizleme

---

## 8. Status Lifecycle Baseline

### VisualAssetPlanStatus Enum

```
AI_GENERATED ──→ NEEDS_REVIEW ──→ APPROVED
     │                │
     │                ▼
     ├──→ REJECTED    │
     │                │
     ▼                ▼
   FAILED         REJECTED
```

### Durum Geçiş Kuralları

| Kural | Açıklama |
|-------|----------|
| AI çıktısı | Yalnızca `AI_GENERATED` oluşturur |
| Parent/scene/thumbnail edit | `NEEDS_REVIEW` yapar |
| Approve | Yalnızca `AI_GENERATED` veya `NEEDS_REVIEW` durumundan |
| Reject | Yalnızca `AI_GENERATED` veya `NEEDS_REVIEW` durumundan |
| APPROVED edit | `NEEDS_REVIEW` yapar |
| REJECTED edit | `NEEDS_REVIEW` yapar |
| FAILED | Edit/approve/reject yapılamaz |
| READY_FOR_PUBLISH | ❌ FP-006'da yok |
| Generic status update | ❌ FP-006'da yok |

---

## 9. One-Approved Rule

### Kural Tanımı

Bir StoryboardDraft için aynı anda yalnızca **bir** APPROVED VisualAssetPlan bulunabilir.

### Uygulama Detayları

| Özellik | Detay |
|---------|-------|
| **Enforcement seviyesi** | Service katmanı (`VisualAssetPlanReviewService`) |
| **İkinci plan onayı** | HTTP 409 Conflict döner |
| **Mevcut onaylı plan** | Sessizce unapprove **edilmez** |
| **Hedef plan** | Mutate **edilmez** (başarısız durumda kalır) |
| **DB kısıtı** | Partial unique index gelecekte hardening olarak eklenebilir |

---

## 10. Provider Baseline

### Mock Provider

| Özellik | Detay |
|---------|-------|
| **Sınıf** | `MockAiVisualAssetAgentGateway` |
| **Davranış** | Deterministik — her sahne için template prompt üretir |
| **Modlar** | SUCCESS, TIMEOUT, EMPTY_RESPONSE, INVALID_FORMAT |
| **Kullanım** | Local development, unit test, staging validasyon |
| **providerType** | `MOCK` |

### OpenAI Provider

| Özellik | Detay |
|---------|-------|
| **Sınıf** | `OpenAiVisualAssetAgentGateway` |
| **API türü** | Chat/text completion (gpt-4o-mini) |
| **Çıktı** | Görsel planlama prompt metni (JSON formatında) |
| **Image API** | ❌ Kullanılmaz |
| **DALL-E** | ❌ Kullanılmaz |
| **Model baseline** | `gpt-4o-mini` |
| **providerType** | `OPENAI` |
| **Runtime seçim** | `ai.provider` / `AI_PROVIDER` env variable |

### Validasyon Notu

- Task 13 staging validasyonu `MOCK` mode ile yapılmıştır.
- OpenAI implementasyonu mocked client ile unit test edilmiştir, gerçek API çağrısı testlerde yapılmamıştır.

---

## 11. Data / Migration Baseline

### V8 Migration

`V8__fp006_visual_asset_planning_tables.sql` aşağıdaki tabloları oluşturur:

| Tablo | Açıklama |
|-------|----------|
| `visual_asset_plans` | Ana plan tablosu |
| `scene_visual_plans` | Sahne bazlı prompt planları |
| `thumbnail_plans` | Thumbnail prompt planları |

### Teknik Detaylar

| Özellik | Detay |
|---------|-------|
| **UUID üretimi** | Application/JPA tarafında (`BaseEntity`) |
| **DB-side UUID** | `gen_random_uuid()` kullanılmaz |
| **PostgreSQL uyumu** | PostgreSQL 12+ (önceki lesson'dan) |
| `imageUrl` / `filePath` / `objectKey` kolonları | ❌ Yok |
| `MediaAsset` / `GeneratedAsset` FK | ❌ Yok |

### Entity İlişkileri

```
VisualAssetPlan ──→ StoryboardDraft (sourceStoryboardDraftId)
VisualAssetPlan ──→ ContentProject (contentProjectId)
VisualAssetPlan ◆──→ SceneVisualPlan (1:N, cascade)
VisualAssetPlan ◆──→ ThumbnailPlan (1:1, cascade)
SceneVisualPlan ──→ StoryboardScene (sourceStoryboardSceneId)
ThumbnailPlan ──→ StoryboardScene (sourceStoryboardSceneId, optional)
```

### Staging Operasyonel Not

> **Önemli:** Staging validasyonu V8 tabloları mevcut şekilde geçmiştir, ancak staging DB instance'ında `flyway_schema_history` tablosu bulunmamaktadır. Production rollout öncesinde migration ownership ve Flyway history alignment teyit edilmelidir.

---

## 12. Validation Baseline

### Backend Testler

| Metric | Değer |
|--------|-------|
| **Toplam test** | 979 |
| **Geçen** | 979 |
| **Başarısız** | 0 |
| **Build** | SUCCESS |

### Frontend Testler

| Metric | Değer |
|--------|-------|
| **Toplam test** | 134 |
| **Geçen** | 134 |
| **Başarısız** | 0 |
| **TypeScript** | Clean (0 error) |
| **Vite build** | Success (70 module, 1.50s) |

### Local Smoke Test (Task 12)

| Metric | Değer |
|--------|-------|
| **API smoke test** | 31/31 pass |
| **Provider** | MOCK |
| **Scene count** | 6 |
| **Workflow** | generate → query → edit → approve → one-approved → reject → negative |
| **Kod değişikliği** | Yok |

### Staging Smoke Test (Task 13)

| Metric | Değer |
|--------|-------|
| **API smoke test** | 32/32 pass |
| **Provider** | MOCK |
| **Backend health** | UP |
| **Admin web** | HTTP 200 |
| **PostgreSQL** | Healthy |
| **Redis** | Healthy |
| **MinIO** | Running |
| **Kod değişikliği** | Yok |
| **Commit** | Yok (validation-only) |

---

## 13. Safety / Security Baseline

### Asla Saklanmayan / Expose Edilmeyen Alanlar

| Alan | Durum |
|------|-------|
| `rawPrompt` | ❌ API/UI/log'da yok |
| `internalPrompt` | ❌ API/UI/log'da yok |
| `orchestrationPrompt` | ❌ API/UI/log'da yok |
| `providerRequest` | ❌ API/UI/log'da yok |
| `providerResponse` | ❌ API/UI/log'da yok |
| `providerPayload` | ❌ API/UI/log'da yok |
| `apiKey` | ❌ API/UI/log'da yok |
| `secret` | ❌ API/UI/log'da yok |
| `sk-` | ❌ API/UI/log'da yok |
| `imageUrl` | ❌ API/UI/log'da yok |
| `filePath` | ❌ API/UI/log'da yok |
| `bucket` | ❌ API/UI/log'da yok |
| `objectKey` | ❌ API/UI/log'da yok |
| `mediaAssetId` | ❌ API/UI/log'da yok |
| `generatedAssetId` | ❌ API/UI/log'da yok |
| `publishUrl` | ❌ API/UI/log'da yok |
| `uploadUrl` | ❌ API/UI/log'da yok |

### İzin Verilen Ürün Çıktıları

| Alan | Durum |
|------|-------|
| `englishImagePrompt` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `turkishVisualContext` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `negativePrompt` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `styleDirective` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `compositionNotes` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `thumbnailPrompt` | ✅ API/UI'da gösterilir (ürün çıktısı) |
| `providerType` | ✅ Güvenli metadata |
| `modelName` | ✅ Güvenli metadata |

### Teyitler

- ✅ API response'larında unsafe alan yok
- ✅ UI'da unsafe alan yok
- ✅ Staging log'larında unsafe alan yok (Task 13 doğrulanmış)
- ✅ Commit'lerde secret yok

---

## 14. Governance Baseline

| Kural | Durum |
|-------|-------|
| İnsan review zorunlu | ✅ AI çıktısı asla otomatik onaylanmaz |
| `READY_FOR_PUBLISH` yok | ✅ FP-006'da bu durum yok |
| Generic status update yok | ✅ Endpoint yok |
| Publishing yok | ✅ Endpoint/UI yok |
| Scheduling yok | ✅ Endpoint/UI yok |
| Media generation yok | ✅ Image/video/audio üretilmez |
| Gerçek image generation yok | ✅ DALL-E/Midjourney/SD çağrılmaz |
| `MediaAsset` / `GeneratedAsset` yok | ✅ Entity kullanılmaz/oluşturulmaz |
| Prompt metni ürün çıktısıdır | ✅ `englishImagePrompt` vb. gösterilebilir |
| Internal orchestration prompt ürün çıktısı değildir | ✅ Expose edilmez |

---

## 15. Known Non-Blockers / Operational Notes

1. **Staging MOCK mode:** Task 13 staging validasyonu MOCK provider ile yapılmıştır. Gerçek OpenAI staging smoke test'i, API key güvenli şekilde yapılandırıldığında daha sonra yapılabilir.

2. **Staging DB Flyway history:** Staging DB'de `flyway_schema_history` tablosu bulunmamaktadır. V8 tabloları mevcuttur ve app başarıyla başlamıştır, ancak production rollout öncesinde migration governance teyit edilmelidir.

3. **Hibernate constraint uyarısı:** Backend startup'ta `constraint "uk8841wb7kjqdan071rcek6qkfl" of relation "thumbnail_plans" does not exist, skipping` uyarısı görülmüştür. Kozmetik, non-blocking.

4. **READY_FOR_PUBLISH UI referansı:** `READY_FOR_PUBLISH` ifadesi frontend bundle'da yalnızca ContentProject status filter dropdown ve generic badge color map'inde bulunmaktadır. VisualAssetPlan workflow'unda aksiyon olarak **yoktur**.

5. **Gerçek image generation:** Görsel dosya üretimi gelecek bir feature'dır. FP-006 yalnızca prompt planlama ve handoff kapsamındadır.

---

## 16. Future Follow-up

Aşağıdaki özellikler FP-006 kapsamında **değildir** ve gelecek feature'lar olarak planlanmalıdır:

| Gelecek Feature | Açıklama |
|-----------------|----------|
| Gerçek image generation provider entegrasyonu | DALL-E, Midjourney, Stable Diffusion vb. |
| `MediaAsset` / `GeneratedAsset` model | Üretilen medya dosyaları |
| Image storage | S3/MinIO'da görsel depolama |
| Platform bazlı görsel adaptasyonu | Platforma özel görsel boyut/stil |
| Visual export handoff | Dış araçlara aktarım |
| Video assembly / rendering | FFmpeg ile video birleştirme |
| Voiceover / audio | TTS ve ses dosyası üretimi |
| Publishing / scheduling | Platform yayın otomasyonu |
| Analytics / quality review | Kalite metrikleri |
| Production migration hardening | Flyway history alignment |

### Önerilen Sonraki Feature

İki olası yön:

- **FP-007 — Image Generation & Media Asset Pipeline:** Gerçek image generation ile medya pipeline'ı
- **FP-007 — Visual Prompt Export / Provider Handoff:** Image generation öncesi dış araçlara export katmanı

Hangi yönün seçileceği, gerçek medya üretiminin mi yoksa başka bir handoff/export katmanının mı önce gelmesi gerektiğine bağlıdır.

---

## 17. Final Decision

> **FP-006 CLOSED.**
>
> Görsel varlık planlama ve image generation prompt handoff özelliği tamamlanmıştır.
> Tüm backend, frontend, provider, API, UI, migration, test ve staging baseline'ları dokümante edilmiştir.
>
> Gerçek image generation, ayrı bir gelecek feature olarak planlanmalıdır ve FP-006 closure'ı ile karıştırılmamalıdır.

---

## 18. Commit History Summary

| Task | Commit | Özet |
|------|--------|------|
| Task 04 | `b56761d` | Domain model + V8 migration |
| Task 05 | `22ce960` | AI gateway DTO + mock provider |
| Task 06 | `9be2e98` | Generation service |
| Task 07 | `33925cf` | Review / query service |
| Task 08 | `7726f4e` | REST API |
| Task 09 | `43115a1` | OpenAI text provider |
| Task 10 | `906de17` | Admin UI workflow |
| Task 11 | `118b754` | Frontend tests / build validation |
| Task 12 | — | Local smoke validation (no commit) |
| Task 13 | — | Staging validation (no commit) |
| Task 14 | *(bu commit)* | Final closure & operational baseline doc |

---

## 19. Document History

| Versiyon | Tarih | Açıklama |
|----------|-------|----------|
| v1.0 | 2026-07-04 | FP-006 final closure and operational baseline |
