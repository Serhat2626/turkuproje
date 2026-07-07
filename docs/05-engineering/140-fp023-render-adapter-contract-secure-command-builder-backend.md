# FP-023 Task 02 — Render Adapter Contract + Secure Command Builder Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-023 Task 02 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Adapter Contract Summary

### New Types

| Type | Kind | Description |
|------|------|------------|
| TurkuRenderAdapterMode | enum (5) | STUB, FFMPEG_DISABLED, FFMPEG_CONFIG_MISSING, FFMPEG_READY, FFMPEG_FAILED_SAFE |
| TurkuRenderAdapterFailureCode | enum (8) | RENDER_PROVIDER_DISABLED through RENDER_FAILED_SAFE |
| TurkuRenderAdapterRequest | record | projectId, jobId, executionPlan, requestedMode |
| TurkuRenderAdapterResult | record | success, adapterMode, failureCode, sanitizedMessage, traceReference, safetyEnvelope, durationMs |
| TurkuRenderAdapterSafetyEnvelope | record | humanReviewRequired=true, copyrightReviewRequired=true, autoApproved=false, notPublishReady=true, notExportReady=true, safetyFlags, reviewFlags |
| TurkuRenderAdapterTraceReference | record | Opaque trace ref (prefix:uuid), no file paths/URLs |
| TurkuRenderAdapterPolicy | service | Shell metachar detection, forbidden input detection, adapter mode resolution |
| TurkuRenderAdapterSanitizer | service | Message sanitization (passwords, paths, URLs redacted), trace ref sanitization |

---

## Secure Command Builder Summary

### New Types

| Type | Kind | Description |
|------|------|------------|
| TurkuFfmpegCommandPlan | record | executableToken, safeArgs, limits, inputSpec, outputSpec, validated, validationError |
| TurkuFfmpegCommandBuilder | service | Validates and builds safe command plans — no shell, no interpolation |
| TurkuFfmpegInputSpec | record | internalRef, sceneNo, durationSeconds, format — no file paths/URLs |
| TurkuFfmpegOutputSpec | record | outputRef, targetFormat, targetResolution, targetFps, maxDurationSeconds — no file paths/URLs |
| TurkuFfmpegExecutionLimits | record | Bounded timeout/duration/size/resolution/fps with clamping |

### Command Builder Safety Rules

| Rule | Enforcement |
|------|------------|
| No shell string | ProcessBuilder-style arg list only |
| No command interpolation | All args from internal plan or hardcoded codec values |
| No user-controlled raw args | Args generated internally only |
| Shell metacharacter rejection | `;`, `&&`, `||`, `` ` ``, `$(`, `>`, `<`, `rm`, `curl`, etc. |
| Forbidden input rejection | publicUrl, signedUrl, cdnUrl, storageKey, base64, Bearer, Authorization, etc. |
| Allowed formats | mp4, webm only |
| Allowed codecs | libx264, libvpx-vp9 only |
| Allowed presets | ultrafast, fast, medium only |

---

## Limits Summary

| Parameter | Default | Absolute Max |
|-----------|:-------:|:------------:|
| Timeout | 300s | 600s |
| Duration | 600s | 900s |
| File size | 500MB | 1024MB |
| Width | 1920 | 3840 |
| Height | 1080 | 2160 |
| FPS | 30 | 60 |

All limits clamped at construction time (min=1, max=absolute max).

---

## Disabled / Default Behavior

- Default adapter mode: **STUB**
- FFmpeg adapter: **disabled by default**
- No real FFmpeg execution in Task 02
- No ProcessBuilder invocation
- Existing `StubTurkuRenderRunnerGateway` continues to work unchanged
- `TurkuRenderRunnerGateway` interface unchanged (1 method)

---

## No-Real-Execution Policy

- `TurkuFfmpegCommandPlan` is a **representation only** — no process execution
- `TurkuFfmpegCommandBuilder` produces validated plans, does not run FFmpeg
- No `ProcessBuilder`, `Runtime.exec()`, or shell invocation in Task 02
- Real execution deferred to Task 03

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |
| No rendered video file in API | ✅ |
| No downloadable file | ✅ |
| No shell execution | ✅ |
| No user-controlled command args | ✅ |
| 37+ forbidden DTO fields checked | ✅ |

---

## Deferred: Task 03

Task 03 — FFmpeg Stub/Sandbox Runner Backend, disabled by default.
