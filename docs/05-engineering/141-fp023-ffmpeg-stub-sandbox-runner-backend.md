# FP-023 Task 03 — FFmpeg Stub/Sandbox Runner Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-023 Task 03 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Runner Resolver Summary

### TurkuRenderRunnerResolver

Selects between stub and FFmpeg gateways based on adapter mode:

| Condition | Active Runner | Mode |
|-----------|--------------|------|
| Default (no config) | StubTurkuRenderRunnerGateway | STUB |
| FFmpeg not enabled | StubTurkuRenderRunnerGateway | STUB |
| FFmpeg enabled, no config | StubTurkuRenderRunnerGateway | FFMPEG_CONFIG_MISSING |
| FFmpeg enabled + config | FfmpegTurkuRenderRunnerGateway | FFMPEG_READY |
| FFmpeg failed to init | StubTurkuRenderRunnerGateway | FFMPEG_FAILED_SAFE |

Diagnostics method provides detailed adapter result with failure codes and safety envelope.

---

## FFmpeg Gateway — Disabled / Sandbox Behavior

### FfmpegTurkuRenderRunnerGateway

- **Disabled by default** (`enabled=false`)
- Implements `TurkuRenderRunnerGateway` interface
- When disabled: returns `RENDERER_UNAVAILABLE` failure with opaque trace ref
- When enabled (sandbox mode):
  1. Builds `TurkuFfmpegCommandPlan` via secure command builder
  2. Validates plan (format, resolution, fps, duration, shell safety)
  3. Does **NOT** execute the plan — no ProcessBuilder, no OS process
  4. Returns sandbox result with opaque `internal-ffmpeg-sandbox:{uuid}` artifact ref

### No Process Execution Policy

- No `ProcessBuilder` field or invocation
- No `Runtime.getRuntime().exec()` call
- No `Process` or `ProcessHandle` usage
- No FFmpeg binary required
- No OS-level side effects
- No file creation or deletion

---

## Command Plan Usage

The FFmpeg gateway uses `TurkuFfmpegCommandBuilder` to:
1. Validate the render execution plan
2. Check format/resolution/fps/duration bounds
3. Reject shell metacharacters and forbidden inputs
4. Produce a validated `TurkuFfmpegCommandPlan` with safe arg list

The command plan is a **representation only**. In FP-023, it is never executed.

---

## Failure Code Mapping

| Code | Trigger |
|------|---------|
| RENDER_PROVIDER_DISABLED | FFmpeg adapter disabled (default) |
| RENDER_CONFIG_MISSING | Enabled but config not present |
| RENDERER_UNAVAILABLE | Disabled gateway execute() call |
| SAFETY_VIOLATION | Command plan validation failed |
| RENDER_FAILED_SAFE | General safe failure |

Messages are sanitized via `TurkuRenderAdapterSanitizer` — no passwords, paths, URLs, or secrets in output.

---

## Stub Runner Compatibility

- `StubTurkuRenderRunnerGateway` continues to work unchanged
- Full E2E render flow (bootstrap → render completed) verified with stub
- Both stub and FFmpeg implement `TurkuRenderRunnerGateway` interface
- Resolver defaults to stub — existing behavior preserved

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
| No shell/process execution | ✅ |
| No ProcessBuilder field | ✅ |
| No file path in API | ✅ |
| humanReviewRequired=true | ✅ |
| copyrightReviewRequired=true | ✅ |
| autoApproved=false | ✅ |
| notPublishReady=true | ✅ |
| notExportReady=true | ✅ |

---

## Deferred: Task 04

Task 04 — Internal Artifact Metadata + Cleanup Policy Backend.
