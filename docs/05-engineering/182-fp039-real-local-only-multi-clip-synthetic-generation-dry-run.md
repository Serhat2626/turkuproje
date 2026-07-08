# FP-039 — Real LOCAL-only Multi-Clip Synthetic Generation Dry Run

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-039: Multi-Clip Synthetic Generation Dry Run |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4183 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-038 Baseline Reference

FP-038 introduced multi-scene internal render orchestration/job planning foundation.
FP-039 performs the first controlled expansion from one generated clip to multiple generated clips.

---

## 3. Purpose

First real LOCAL-only multi-clip synthetic FFmpeg generation dry run.
Generates multiple tiny synthetic scene clips from a safe FP-038 READY_FOR_LOCAL_MANUAL_RENDER job plan.

**This is NOT concat/stitching. No final video assembly. No public delivery.**

---

## 4. Generation Statuses (11)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Created |
| GATED_FOR_LOCAL_MANUAL_RUN | Gated |
| GENERATING_SYNTHETIC_CLIPS | In progress |
| GENERATED_CLIPS_VERIFIED | All clips verified |
| REJECTED_UNSAFE | Policy violation |
| FAILED_SAFE | Internal error |
| GATE_DENIED | Gate check failed |
| MANUAL_FLAG_NOT_ENABLED | Harness disabled |
| PROCESS_DISABLED | Process not enabled |
| FFMPEG_UNAVAILABLE | FFmpeg missing |
| JOB_NOT_READY | Job not ready |

---

## 5. Gate Requirements

Real generation only when ALL pass:
- environment = LOCAL
- multiClipEnabled = true
- clipGenerationEnabled = true (underlying harness)
- processEnabled = true (runner)
- binaryPolicy is available
- jobPlan status = READY_FOR_LOCAL_MANUAL_RENDER

CI uses fake/test-double runner. Real execution is dev-local/manual-only.

---

## 6. Synthetic Input Policy

- Only synthetic lavfi inputs (testsrc/color/anullsrc)
- No copyrighted media
- No user media
- No external file input

---

## 7. Per-Clip Limits

| Limit | Value |
|-------|:-----:|
| Duration | ≤5s |
| FPS | ≤30 |
| Resolution | ≤1280×720 |
| Size | ≤50MB |
| Format | MP4/WEBM |

---

## 8. Safe Result Fields

opaqueJobRef, opaqueClipRunRef, status, issueCodes, sceneCount, generatedClipCount, verifiedClipCount, rejectedClipCount, totalDurationSeconds, maxWidth/Height/Fps, aggregateSizeBytes, opaqueClipRefs, internalOnly=true, realExecutionPerformed

---

## 9. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 10. Integration

- **FP-035**: Each clip verified through lifecycle service
- **FP-036**: Verified clips attachable to bundle manifest as PRIMARY_VIDEO_CLIP
- **FP-038**: Job plan provides scene list for multi-clip generation

---

## 11. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ No concat/stitching/final assembly added
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ No copyrighted media/lyrics/audio/video reproduction
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- ✅ 0 forbidden field violations

---

## 12. Final Handoff

- **FP-039 is CLOSED**
- **FP-001 → FP-039 CLOSED** (39 features)
- Next feature may start from **FP-040**
