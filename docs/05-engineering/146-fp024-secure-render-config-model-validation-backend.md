# FP-024 Task 02 — Secure Render Config Model + Validation Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-024 Task 02 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |
| **Real FFmpeg Execution** | None |
| **Filesystem Access** | None |

---

## Config Model Summary

### New Types (7)

| Type | Kind |
|------|------|
| TurkuRenderConfigMode | enum (6 values) |
| TurkuRenderConfigFailureCode | enum (19 values) |
| TurkuRenderSecureConfig | final class |
| TurkuRenderConfigValidationResult | final class |
| TurkuRenderConfigValidationIssue | final class |
| TurkuRenderConfigSafetyEnvelope | final class |
| TurkuRenderConfigValidator | class |

### Config Mode Values

| Value | Meaning |
|-------|---------|
| STUB_ONLY | Stub renderer, no FFmpeg config |
| FFMPEG_DISABLED | Config present but enabled=false |
| FFMPEG_CONFIG_MISSING | Config absent |
| FFMPEG_CONFIG_INVALID | Config present but fails validation |
| FFMPEG_CONFIG_VALID | Config present, enabled, all checks pass |
| FFMPEG_FAILED_SAFE | Config was valid but execution failed safely |

### Config Properties

- enabled flag (default: false)
- executable token (opaque)
- workspace/temp tokens (opaque)
- maxTimeoutSeconds (10–300)
- maxDurationSeconds (1–600)
- maxOutputSizeMb (1–1000)
- maxResolution (up to 3840x2160)
- maxFps (1–60)
- allowedFormats (mp4, webm)
- allowedCodecs (h264, vp9)

---

## Validation Rules Summary

### Fail-Closed Behavior

- null config → CONFIG_MISSING
- disabled config → FFMPEG_DISABLED
- Any invalid field → FFMPEG_CONFIG_INVALID
- All validation issues returned as safe codes/messages

### Pattern Rejection

| Pattern | Detection |
|---------|-----------|
| Shell metacharacters (`;`, `&&`, `\|\|`, `$(`, `` ` ``, etc.) | ✅ |
| Path traversal (`..`, `/../`, `%2e%2e`) | ✅ |
| URLs (http/https/ftp/s3/gs/file) | ✅ |
| Secrets (password=, secret=, Bearer, Authorization:) | ✅ |
| Storage keys (storageKey, publicUrl, signedUrl, cdnUrl) | ✅ |
| Base64/data URIs | ✅ |

### Bounds Enforcement

| Property | Min | Max |
|----------|-----|-----|
| timeout | 10s | 300s |
| duration | 1s | 600s |
| output size | 1MB | 1000MB |
| fps | 1 | 60 |
| resolution width | 1 | 3840 |
| resolution height | 1 | 2160 |

---

## Disabled-by-Default Behavior

- Default constructor creates disabled config
- Validator returns FFMPEG_DISABLED for disabled config
- No execution path reachable without explicit enabled=true + valid config

---

## No-Execution / No-Filesystem Policy

- No ProcessBuilder in any Task 02 class
- No Runtime.exec()
- No File I/O
- No filesystem access
- No real FFmpeg invocation
- Stub remains default

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No unsafe HTML | ✅ |
| No token/JWT/session exposure | ✅ |
| humanReviewRequired=true | ✅ |
| autoApproved=false | ✅ |
| 7 classes × 29 forbidden fields = 0 violations | ✅ |

---

## Deferred: Task 03

Task 03 — Internal Render Workspace + File Safety Backend.
