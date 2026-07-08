# FP-041 — First Real LOCAL Manual Synthetic Clip Stitching Evidence

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-041: Real Local Stitching Evidence |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4219 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-040 Baseline Reference

FP-040 introduced clip stitching harness with fake/test-double execution.
FP-041 adds real LOCAL-only manual FFmpeg stitching through the existing runner boundary.

---

## 3. Purpose

First real LOCAL-only manual FFmpeg stitching run evidence.
Multiple synthetic clips → one stitched internal synthetic artifact.

---

## 4. Real Stitching Path

1. `TurkuClipStitchingHarness.attempt()` validates input clips
2. Delegates to `TurkuFfmpegLocalDevProcessRunner.stitchClips()`
3. Runner generates N synthetic clips via testsrc, writes concat list, stitches via `ffmpeg -f concat`
4. Returns opaque ref only — no path/command/log exposure
5. All temp files use `deleteOnExit()`

---

## 5. Gate Requirements

Real stitching only when ALL pass:
- stitchingEnabled = true
- binaryPolicy available
- runner processEnabled = true
- runner gate mode = REAL_DEV_TEST_ONLY
- input clips validated

CI: fake/test-double only. Runner processEnabled = false by default.

---

## 6. Synthetic Input Policy

- testsrc + anullsrc only
- No copyrighted media
- No user media
- No external file input
- Duration per clip ≤5s, total ≤60s
- fps ≤30, resolution ≤1280×720
- Format MP4/WEBM

---

## 7. Safe Output Metadata

opaqueStitchRef, status, issueCodes, inputClipCount, totalDurationSeconds, maxWidth/Height/Fps, estimatedOutputSizeBytes, outputFormat, opaqueOutputRef, internalOnly=true, realExecutionPerformed, stitchedArtifactVerified

---

## 8. Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, token, jwt, session, principal, authorizationHeader, lyrics, scriptBody, promptText, copyrightedContent

---

## 9. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ Real FFmpeg stitching via runner boundary only
- ✅ CI uses fake/test doubles only
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- ✅ No copyrighted media
- ✅ 0 forbidden field violations

---

## 10. Final Handoff

- **FP-041 is CLOSED**
- **FP-001 → FP-041 CLOSED** (41 features)
- Next feature may start from **FP-042**
