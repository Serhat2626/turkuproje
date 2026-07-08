# FP-037 — Internal Scene Render Plan to Bundle Assembly Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-037: Scene Render Plan to Bundle Assembly |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4141 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-036 Baseline Reference

FP-036 introduced metadata-only render bundle/manifest foundation.
FP-037 builds on this to plan how storyboard scenes map to bundle assembly requests.

---

## 3. Purpose

Internal safe planning/assembly layer between storyboard/scene concepts and FP-036 render bundle manifests.
Models how scenes would be converted into internal render bundle assembly requests using metadata-only plans.

**This is NOT real multi-scene rendering. No real FFmpeg expansion required.**

---

## 4. Scene Render Plan Statuses (5)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Plan created |
| VALIDATED_FOR_INTERNAL_RENDER_ONLY | Validated against policy |
| READY_FOR_BUNDLE_ASSEMBLY | All expectations met, ready for assembly |
| REJECTED_UNSAFE | Policy violation |
| FAILED_SAFE | Internal error |

**No READY_FOR_PUBLISH. No COMPLETED.**

---

## 5. Artifact Expectation Roles

Reuses TurkuRenderBundleArtifactRole:
- PRIMARY_VIDEO_CLIP (required)
- POSTER_FRAME (optional)
- AUDIO_STUB (optional)
- VERIFICATION_REPORT (optional)
- INTERNAL_METADATA (optional)
- UNKNOWN → rejected

---

## 6. Safe Scene Plan Fields

| Field | Type |
|-------|------|
| opaqueSceneRef | String |
| opaquePlanRef | String |
| sceneOrder | bounded int |
| expectedDurationSeconds | bounded int |
| expectedFps | bounded int |
| expectedWidth/Height | bounded int |
| artifactExpectations | List of role+required |
| status | PlanStatus enum |
| issueCodes | List of issue code enums |

---

## 7. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 8. Policy Limits

| Limit | Value |
|-------|:-----:|
| Max scenes per batch | 20 |
| Scene order | positive, unique |
| Duration per scene | ≤5s |
| FPS | ≤30 |
| Resolution | ≤1280×720 |
| Primary video expectation | exactly 1 required |
| UNKNOWN role | rejected |

---

## 9. Bundle Assembly

- Accepts validated plan + artifact refs
- Delegates to FP-036 TurkuRenderBundleManifestService
- Missing required artifacts → rejected bundle
- Rejected/failed artifacts propagate to bundle
- Only safe metadata passed

---

## 10. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ No real multi-scene rendering added
- ✅ No real FFmpeg expansion required
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ No copyrighted media/lyrics/audio/video reproduction
- ✅ 0 forbidden field violations

---

## 11. Final Handoff

- **FP-037 is CLOSED**
- **FP-001 → FP-037 CLOSED** (37 features)
- Next feature may start from **FP-038**
