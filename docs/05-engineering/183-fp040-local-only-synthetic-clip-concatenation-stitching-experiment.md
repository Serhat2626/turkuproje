# FP-040 — Local-only Synthetic Clip Stitching Experiment

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-040: Clip Stitching Experiment |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4205 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-039 Baseline Reference

FP-039 introduced multi-clip synthetic generation.
FP-040 adds the first clip stitching/concat experiment using verified clips from FP-039.

---

## 3. Purpose

First LOCAL-only synthetic clip stitching experiment.
Validates, fake-stitches, and verifies multiple verified synthetic clips into a single internal stitched artifact.

**This is NOT public delivery. No playback/download/export.**

---

## 4. Stitching Statuses (11)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Created |
| GATED_FOR_LOCAL_MANUAL_RUN | Gated |
| VALIDATED_INPUT_CLIPS | Inputs validated |
| STITCHING_SYNTHETIC_CLIPS | In progress |
| STITCHED_ARTIFACT_VERIFIED | Verified |
| REJECTED_UNSAFE | Policy violation |
| FAILED_SAFE | Internal error |
| GATE_DENIED | Gate failed |
| MANUAL_FLAG_NOT_ENABLED | Disabled |
| PROCESS_DISABLED | Process off |
| FFMPEG_UNAVAILABLE | FFmpeg missing |
| INPUT_INVALID | Bad input |

---

## 5. Gate Requirements

Real stitching only when all pass:
- stitchingEnabled = true
- binaryPolicy available
- input clips validated

CI uses fake/test-double. Real FFmpeg local/manual only.

---

## 6. Stitching Policy Limits

| Limit | Value |
|-------|:-----:|
| Min input clips | 2 |
| Max input clips | 10 |
| Duration per clip | ≤5s |
| Total duration | ≤60s |
| FPS | ≤30 |
| Resolution | ≤1280×720 |
| Output size | ≤150MB |
| Format | MP4/WEBM |

---

## 7. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 8. Integration

- **FP-035**: Stitched artifact verified through lifecycle service
- **FP-036**: Stitched output attachable to bundle manifest
- **FP-039**: Input clips from multi-clip generation result

---

## 9. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ No copyrighted media/lyrics/audio/video reproduction
- ✅ No user media processing
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- ✅ 0 forbidden field violations

---

## 10. Final Handoff

- **FP-040 is CLOSED**
- **FP-001 → FP-040 CLOSED** (40 features)
- Next feature may start from **FP-041**
