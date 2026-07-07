# FP-021 Task 04 — Admin UI Composition Timeline / Readiness Preview

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-021 Task 04 |
| **Type** | Full-stack (Backend + Frontend) |
| **Status** | COMPLETED |
| **Endpoints** | 40 = 23 POST + 17 GET |
| **Migration/schema/config** | None |

---

## Endpoint Added (1)

| Method | Path | Description |
|--------|------|------------|
| GET | `/admin/turku-video-projects/{id}/composition-preview` | Read-only composition timeline/readiness preview |

### TurkuVideoProjectController Inventory: 14 POST + 5 GET = 19

---

## Backend Summary

### New DTOs
| Type | Purpose |
|------|---------|
| TurkuCompositionPreviewScene | Single scene in composition timeline |
| TurkuCompositionPreviewResponse | Full composition preview with readiness summary |

### Endpoint Behavior
- Reads composition plan + render manifest from project
- Builds safe timeline scene list from composition plan scenes
- Computes readiness status: BLOCKED / NEEDS_REVIEW
- Includes review flags: NOT_PUBLISH_READY, NOT_EXPORT_READY, INTERNAL_PREVIEW_ONLY
- `humanReviewRequired=true`, `autoApproved=false`, `copyrightReviewRequired=true` always
- Safe at any stage — returns empty/BLOCKED state gracefully

---

## Frontend Summary

### Types Added (8)
| Type | Purpose |
|------|---------|
| TurkuCompositionPlanResponse | Composition plan creation response |
| TurkuRenderManifestResponse | Render manifest creation response |
| TurkuRenderReadinessStatus | READY / NEEDS_REVIEW / BLOCKED |
| TurkuRenderReadinessIssue | Single readiness issue |
| TurkuRenderReadinessReport | Full readiness assessment |
| TurkuCompositionPreviewScene | Single scene in timeline |
| TurkuCompositionPreviewResponse | Full composition preview |

Two new workflow stages: `COMPOSITION_PLAN_PREPARED`, `RENDER_MANIFEST_PREPARED`.

### API Client Methods Added (4)
| Method | Endpoint | HTTP |
|--------|----------|------|
| prepareTurkuCompositionPlan | prepare-composition-plan | POST |
| prepareTurkuRenderManifest | prepare-render-manifest | POST |
| getTurkuRenderReadiness | render-readiness | GET |
| getTurkuCompositionPreview | composition-preview | GET |

No Authorization headers. No publish/export/download methods.

### UI Changes

#### Composition / Render Planning Section
- Appears after composition preview data is available
- Clearly labeled "Dahili Planlama — Yayın/Dışa Aktarım Yok"
- Disclaimer: "Bu bölüm dahili render planlaması içindir"

#### Readiness Panel
- Status badge: BLOCKED (red) or NEEDS_REVIEW (yellow)
- Composition ready: ✅/❌
- Manifest ready: ✅/❌
- Readiness score: N/100
- Issues list with warnings
- Review flags: HUMAN_REVIEW_REQUIRED, COPYRIGHT_REVIEW_REQUIRED, NOT_PUBLISH_READY, NOT_EXPORT_READY, INTERNAL_PREVIEW_ONLY

#### Composition Timeline
- Scene order with scene numbers
- Scene title + duration badge
- Narration summary (not lyrics)
- Image ref label (safe internal-only)
- Mood / transition / camera direction details

#### Safety Summary Panel
- İnsan incelemesi: Evet
- Otomatik onay: Hayır
- Telif incelemesi: Evet
- Yayın hazır: Hayır
- Dışa aktarım hazır: Hayır

#### Stage Actions Added
- Kompozisyon Planı Hazırla (enabled at IMAGE_BATCH_COMPLETED)
- Render Manifest Hazırla (enabled at COMPOSITION_PLAN_PREPARED)

---

## No-Render / No-Export Policy

- No video player element
- No rendered video file
- No download/export/publish buttons
- No downloadable file
- No public/signed/CDN URLs
- Preview is metadata-only

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML (dangerouslySetInnerHTML/innerHTML) | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |
| No rendered video file | ✅ |
| No downloadable file | ✅ |
| No video player element | ✅ |

---

## Deferred: Task 05 — E2E Render-Planning Safety Regression + Closure
