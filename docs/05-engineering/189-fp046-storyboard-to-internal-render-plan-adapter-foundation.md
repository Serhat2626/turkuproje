# FP-046 — Storyboard to Internal Render Plan Adapter Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-046: Storyboard Render Plan Adapter |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4293 passed, 0 failures |
| **Frontend Tests** | 2160 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Safe adapter layer from existing story/scenario/storyboard metadata into FP-037 scene render plans.
Connects product/storyboard concepts to the internal story-to-video render chain without public delivery.

---

## 3. Adapter Input (Safe Metadata Only)

- opaque storyboard/story/scene refs
- scene order (positive, unique)
- target duration (1–5s)
- target fps (1–30)
- target resolution (1–1280×720)
- target format

---

## 4. Adapter Output

- TurkuStoryboardRenderPlanAdapterResult containing validated FP-037 scene plans
- Status: READY_FOR_INTERNAL_RENDER_CHAIN when all plans pass
- Scene plans feed directly into FP-038 orchestration and FP-044 dry run

---

## 5. Statuses (6)

CREATED_INTERNAL_ONLY, STORYBOARD_METADATA_ACCEPTED, SCENE_RENDER_PLANS_CREATED, READY_FOR_INTERNAL_RENDER_CHAIN, REJECTED_UNSAFE, FAILED_SAFE

---

## 6. Issue Codes (14)

NONE, STORYBOARD_REF_MISSING, STORY_REF_MISSING, NO_SCENES, TOO_MANY_SCENES, SCENE_ORDER_INVALID, SCENE_ORDER_DUPLICATE, SCENE_REF_MISSING, DURATION_EXCEEDS_LIMIT, FPS_EXCEEDS_LIMIT, WIDTH_EXCEEDS_LIMIT, HEIGHT_EXCEEDS_LIMIT, MISSING_PRIMARY_VIDEO_EXPECTATION, SCENE_PLAN_VALIDATION_FAILED

---

## 7. Integration

- **FP-037**: Scene render plans created via validate()
- **FP-038**: Adapter plans feed buildJobPlan() directly
- **FP-044**: Scene count feeds E2E dry run harness

---

## 8. Forbidden Fields

lyrics, scriptBody, promptText, copyrightedContent, filePath, absolutePath, inputPath, outputPath, workspacePath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader

---

## 9. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL
- ✅ No auto-approval or publish readiness
- ✅ No prompt/script/lyrics/user media exposure
- ✅ CI fake/test-double based
- ✅ Real FFmpeg remains local/manual-only
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner

---

## 10. Final Handoff

- **FP-046 is CLOSED**
- **FP-001 → FP-046 CLOSED** (46 features)
- Next feature may start from **FP-047**
