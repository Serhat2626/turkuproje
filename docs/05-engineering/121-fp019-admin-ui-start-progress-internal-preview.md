# FP-019 Task 05 — Admin UI Start / Progress / Internal Preview Integration

## Status

| Field | Value |
|-------|-------|
| Task | FP-019 Task 05 |
| Status | **COMPLETED** |
| Type | Frontend + docs |
| Backend changes | Scoped (InternalApprovalReadinessPanel test scope fix only) |
| Endpoint count | **30 = 16 POST + 14 GET** (unchanged) |
| Migration/schema/config | None |

---

## Frontend Types Summary

Added 17 TypeScript types/interfaces to `types/index.ts`:

| Type | Category |
|------|----------|
| TurkuVideoWorkflowStage | Enum/union (12 stages) |
| TurkuSourceConfidence | Enum/union (4 levels) |
| TurkuImageBatchStatus | Enum/union (5 states) |
| TurkuVideoBootstrapRequest | Request DTO |
| TurkuVideoBootstrapResponse | Response DTO |
| TurkuVideoProjectResponse | Response DTO |
| TurkuVideoProgressResponse | Response DTO |
| TurkuNarrativeContext | Domain DTO |
| TurkuStoryPackage | Domain DTO |
| TurkuScenarioPlan | Domain DTO |
| TurkuStoryboardPlan | Domain DTO |
| TurkuStoryboardScene | Domain DTO |
| TurkuSceneAssetPlan | Domain DTO |
| TurkuSceneAssetItem | Domain DTO |
| TurkuSceneImageBatch | Domain DTO |
| TurkuSceneImageRequest | Domain DTO |
| TurkuSceneImageResult | Domain DTO |
| TurkuPreparationResponse | Response DTO |
| TurkuAssetPreparationResponse | Response DTO |
| TurkuImageBatchResponse | Response DTO |

### Forbidden type fields (NOT included)
token, jwt, session, credential, password, secret, apiKey, accessKey, principalRaw, authorizationHeader, publicUrl, signedUrl, cdnUrl, storageKey, base64, exportUrl, publishUrl, downloadUrl, imageData, binaryData

### Forbidden states (NOT included)
PUBLISHED, EXPORTED, AUTO_APPROVED

---

## API Client Summary

File: `turkuVideoApi.ts` — 9 methods:

| Method | HTTP | Endpoint |
|--------|------|----------|
| bootstrapTurkuVideoProject | POST | /admin/turku-video-projects/bootstrap |
| getTurkuVideoProject | GET | /admin/turku-video-projects/{id} |
| getTurkuVideoProjectProgress | GET | /admin/turku-video-projects/{id}/progress |
| prepareTurkuStory | POST | /admin/turku-video-projects/{id}/prepare-story |
| prepareTurkuScenario | POST | /admin/turku-video-projects/{id}/prepare-scenario |
| prepareTurkuStoryboard | POST | /admin/turku-video-projects/{id}/prepare-storyboard |
| prepareTurkuSceneAssets | POST | /admin/turku-video-projects/{id}/prepare-scene-assets |
| prepareTurkuImageBatch | POST | /admin/turku-video-projects/{id}/prepare-image-batch |
| completeTurkuImageBatch | POST | /admin/turku-video-projects/{id}/complete-image-batch |

- No Authorization header
- No Bearer/JWT/session/token handling
- No publish/export/download endpoints
- No base64/storage/provider endpoints
- actorRole is TEST_FALLBACK compatibility only

---

## UI Component/Page Summary

File: `TurkuVideoWorkflowPage.tsx`
Route: `/turku-video`
Sidebar: 🎵 Türkü Video

### Bootstrap Form
- Türkü adı (required)
- Bölge (optional)
- Atmosfer (optional)
- Hedef süre (optional)
- Kaynak notları (optional)
- Anlatım tarzı (optional)
- Görsel tarz (optional)
- Projeyi Başlat butonu

### Project Summary Panel
- Proje ID
- Türkü adı
- Mevcut aşama
- Kaynak güvenilirliği
- İnsan İncelemesi Gerekli badge
- Telif İncelemesi Gerekli badge

### Progress Panel
- Stage timeline (7 stages with done/current/pending indicators)
- Progress summary
- Next available action

### Stage Action Buttons
| Button | Enabled at |
|--------|-----------|
| Hikâye Hazırla | DRAFT_CREATED |
| Senaryo Hazırla | STORY_CONTEXT_PREPARED |
| Storyboard Hazırla | SCENARIO_PREPARED |
| Varlık Planı Hazırla | STORYBOARD_PREPARED |
| Görsel Batch Hazırla | SCENE_ASSET_PLAN_PREPARED |
| Batch Tamamla | IMAGE_BATCH_PREPARED |

Buttons are enabled/disabled based on current workflow stage.

### Internal Preview Panel
- Story summary
- Scenario summary
- Storyboard summary
- Asset plan summary
- Image batch summary
- Completion status
- Disclaimer: "Bu önizleme yalnızca dahili inceleme içindir"

---

## Actor Boundary / actorRole Display

- actorRole is NOT displayed as user identity
- actorRole is NOT labeled as production auth
- Labels: "Dahili İş Akışı", "Dahili Üretim", "Sadece İç Kullanım"
- No "Kullanıcı Kimliği", "Oturum Açan", "Giriş Yapan" labels

---

## Safety Baseline

| Rule | Enforced? |
|------|:---------:|
| No dangerouslySetInnerHTML | ✅ |
| No innerHTML | ✅ |
| No publish/export/download controls | ✅ |
| No Authorization header | ✅ |
| No JWT/token/session/principal | ✅ |
| No base64/storage/provider exposure | ✅ |
| No public/signed/CDN URL rendering | ✅ |
| No unsafe HTML rendering | ✅ |
| No auto-approval controls | ✅ |
| No generic status update | ✅ |
| No copyrighted lyrics/audio/video fields | ✅ |

---

## Endpoint Count

**30 = 16 POST + 14 GET** — unchanged from Task 04.

---

## Backend Changes

Only one scoped backend test fix:
- `InternalApprovalReadinessPanel.test.ts`: Scoped FP-014 forbidden type value check to end before FP-019 additions (COMPLETED now legitimately exists in TurkuImageBatchStatus).

---

## Migration/Schema/Config

**None** — unchanged.

---

## Test Summary

### Frontend
- 807 tests, 0 failures (was 697, +110)
- New test files:
  - `turkuVideoApi.test.ts`: 13 tests (API function existence, forbidden functions, source safety)
  - `TurkuVideoWorkflowPage.test.ts`: ~97 tests (types, page structure, safety, route integration)

### Backend
- 2927 tests, 0 failures (unchanged from Task 04)

---

## Deferred

- **Task 06**: E2E Safety Regression + Final Closure
