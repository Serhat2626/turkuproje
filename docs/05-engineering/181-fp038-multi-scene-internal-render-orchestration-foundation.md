# FP-038 — Multi-Scene Internal Render Orchestration Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-038: Multi-Scene Render Orchestration |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4160 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-037 Baseline Reference

FP-037 introduced scene render plan → bundle assembly foundation.
FP-038 builds on this to orchestrate multiple scene plans into a job.

---

## 3. Purpose

Internal metadata-only multi-scene render orchestration layer.
Models how validated scene render plans are grouped, ordered, checked, and prepared for a future LOCAL-only multi-clip render run.

**This is NOT real multi-scene rendering. No FFmpeg execution. No public delivery.**

---

## 4. Job Statuses (6)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Job created |
| SCENES_ATTACHED | Scenes added |
| VALIDATED_FOR_INTERNAL_RENDER_ONLY | Validated |
| READY_FOR_LOCAL_MANUAL_RENDER | All policies pass |
| REJECTED_UNSAFE | Policy violation |
| FAILED_SAFE | Internal error |

**No READY_FOR_PUBLISH. No COMPLETED.**

---

## 5. Job Issue Codes (17)

NONE, EMPTY_SCENE_LIST, TOO_MANY_SCENES, SCENE_ORDER_NOT_POSITIVE, SCENE_ORDER_NOT_CONTIGUOUS, SCENE_ORDER_DUPLICATE, SCENE_REF_DUPLICATE, SCENE_REF_MISSING, JOB_REF_MISSING, SCENE_NOT_READY, SCENE_REJECTED, SCENE_FAILED, MISSING_PRIMARY_VIDEO, DURATION_PER_SCENE_EXCEEDS_LIMIT, TOTAL_DURATION_EXCEEDS_LIMIT, FPS_EXCEEDS_LIMIT, RESOLUTION_EXCEEDS_LIMIT

---

## 6. Policy Limits

| Limit | Value |
|-------|:-----:|
| Max scenes per job | 20 |
| Min scenes per job | 1 |
| Scene order | positive, unique, contiguous (1,2,3,...) |
| Duration per scene | ≤5s |
| Total duration | ≤60s |
| FPS | ≤30 |
| Resolution | ≤1280×720 |
| Primary video | required per scene |
| Scene refs | unique |

---

## 7. Aggregate Metadata

Service calculates bounded aggregates:
- sceneCount, totalExpectedDurationSeconds
- maxWidth, maxHeight, maxFps
- requiredArtifactCount

---

## 8. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 9. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ No real multi-scene rendering added
- ✅ No real FFmpeg expansion required
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ No copyrighted media/lyrics/audio/video reproduction
- ✅ 0 forbidden field violations

---

## 10. Final Handoff

- **FP-038 is CLOSED**
- **FP-001 → FP-038 CLOSED** (38 features)
- Next feature may start from **FP-039**
