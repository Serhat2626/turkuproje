# FP-035 — Local FFmpeg Render Verification & Artifact Lifecycle Hardening

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-035: Render Verification & Artifact Lifecycle Hardening |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4099 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. FP-034 Baseline Reference

FP-034 proved real synthetic video generation works:
- Synthetic testsrc → libx264 → MP4
- 3s, 640×360, 24fps
- Output: temp file (deleteOnExit), opaque ref only

FP-035 does NOT expand toward user-facing video delivery.
It hardens internal verification and cleanup only.

---

## 3. What FP-035 Hardens

### Lifecycle Statuses (7)

| Status | Meaning |
|--------|---------|
| CREATED_INTERNAL_ONLY | Artifact exists, internal only |
| GENERATED_TEMPORARY | Temp file created, deleteOnExit |
| VERIFIED_METADATA_ONLY | Bounded metadata verification passed |
| CLEANUP_SCHEDULED_OR_REGISTERED | Cleanup registered |
| CLEANUP_CONFIRMED_OR_BEST_EFFORT | Cleanup done (best-effort) |
| REJECTED_UNSAFE | Verification failed |
| FAILED_SAFE | Internal error, no exposure |

### Verification Limits

| Bound | Limit |
|-------|:-----:|
| Duration | ≤5s |
| FPS | ≤30 |
| Width | ≤1280 |
| Height | ≤720 |
| Size | ≤50MB |
| Format | MP4, WEBM only |

### Issue Codes (9)
NONE, FORMAT_NOT_ALLOWED, DURATION_EXCEEDS_LIMIT, FPS_EXCEEDS_LIMIT, WIDTH_EXCEEDS_LIMIT, HEIGHT_EXCEEDS_LIMIT, SIZE_EXCEEDS_LIMIT, METADATA_MISSING, REF_MISSING

---

## 4. Safe Metadata Fields

| Field | Allowed |
|-------|:-------:|
| opaqueArtifactRef | ✅ |
| format enum (MP4/WEBM/UNKNOWN) | ✅ |
| durationSeconds bounded | ✅ |
| fps bounded | ✅ |
| width/height bounded | ✅ |
| sizeBytes bounded | ✅ |
| verificationStatus enum | ✅ |
| issueCodes list | ✅ |

### Explicitly Forbidden Fields

filePath, absolutePath, workspacePath, inputPath, outputPath, executablePath, commandLine, shellCommand, stdout, stderr, rawLog, storageKey, publicUrl, signedUrl, cdnUrl, base64, binaryData, rawPayload, downloadUrl, publishUrl, secret, apiKey, token, session, principal

---

## 5. Cleanup Semantics

- `registerCleanup(opaqueRef)` → CLEANUP_SCHEDULED_OR_REGISTERED
- `confirmCleanup(opaqueRef)` → CLEANUP_CONFIRMED_OR_BEST_EFFORT
- null/blank ref → FAILED_SAFE
- No path exposed in cleanup operations
- Best-effort: actual file deletion via deleteOnExit

---

## 6. ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (ONLY allowed) | ❌ |
| TurkuRenderArtifactLifecycleService | ❌ | ❌ |
| All other classes | ❌ | ❌ |

**Runtime.exec is absent in the entire codebase.**

---

## 7. Safety Guarantees

- ✅ No public delivery/playback/download/export/storage/CDN/signed URL added
- ✅ Real FFmpeg execution remains LOCAL-only/manual-gated
- ✅ CI uses fake/test doubles only
- ✅ ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- ✅ 0 forbidden field violations (6 classes × 19 fields scanned)

---

## 8. Final Handoff

- **FP-035 is CLOSED**
- **FP-001 → FP-035 CLOSED** (35 features)
- Next feature may start from **FP-036**
