# FP-023 — Real Render Provider / FFmpeg Adapter Design with Secure Config

## Status

| Field | Value |
|-------|-------|
| **Feature** | FP-023 — Real Render Provider / FFmpeg Adapter Design with Secure Config |
| **Task** | Task 01 — Design (documentation-only) |
| **Status** | ✅ DESIGN COMPLETE |
| **Baseline commit** | 9ff73f1 |
| **Backend tests** | 3369 (0 failures) |
| **Frontend tests** | 1416 (0 failures) |
| **Combined tests** | 4785 |
| **Endpoints** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Code changes** | None — documentation-only |

---

## 1. Baseline and Current State

FP-022 established:
- `TurkuRenderRunnerGateway` interface for render execution
- `StubTurkuRenderRunnerGateway` producing deterministic opaque artifact refs
- `TurkuRenderExecutionService` orchestrating render job lifecycle
- Render job stages: PREPARED → RUNNING → COMPLETED_INTERNAL / FAILED_SAFE
- Safety envelope with review flags and forbidden-state enforcement
- Internal artifact metadata only — no binary, no file, no download

Current limitations:
- Stub runner only — no real video rendering
- No FFmpeg or cloud execution
- No binary artifact storage
- No render config/secrets
- No production auth

FP-023 bridges from stub to real internal rendering, without enabling publish/export/public delivery.

---

## 2. Product Goal

**Move from stub render execution toward real internal render execution, still internal-only.**

- Replace `StubTurkuRenderRunnerGateway` with a real `FfmpegTurkuRenderRunnerGateway` behind the same interface
- Produce actual rendered video files stored internally
- Artifact metadata visible in admin UI — no download/export/publish
- Completed internal render does NOT mean publish-ready or export-ready
- Human review still required on every artifact
- Auto-approval remains permanently false

### Non-goals for FP-023
- No publish/export/download/public delivery
- No cloud renderer integration
- No production auth
- No public video player
- No CDN/signed URL exposure
- No audio/music/TTS handling

---

## 3. Render Adapter Strategy

### Interface Reuse

The existing `TurkuRenderRunnerGateway` interface remains unchanged:

```java
public interface TurkuRenderRunnerGateway {
    TurkuRenderExecutionResult execute(TurkuRenderExecutionPlan plan);
}
```

### Adapter Hierarchy

```
TurkuRenderRunnerGateway (interface)
├── StubTurkuRenderRunnerGateway         ← default, always available
└── FfmpegTurkuRenderRunnerGateway       ← FP-023, disabled by default
```

### Selection Rules

| Condition | Adapter |
|-----------|---------|
| No FFmpeg config present | StubTurkuRenderRunnerGateway (default) |
| FFmpeg config present + enabled=true | FfmpegTurkuRenderRunnerGateway |
| FFmpeg config present + enabled=false | StubTurkuRenderRunnerGateway |
| Any runtime failure in FFmpeg setup | StubTurkuRenderRunnerGateway (fallback) |

### Key Design Decisions

- **Disabled by default**: Real rendering requires explicit opt-in via safe config
- **No automatic promotion**: Stub → FFmpeg transition requires admin config, never runtime auto-switching
- **Fallback always available**: If FFmpeg adapter fails to initialize, system reverts to stub
- **No hot-swap**: Adapter is resolved at service initialization, not per-request

---

## 4. Secure Config / Secrets Design

### Config Structure (future — not implemented in Task 01)

```yaml
turku:
  render:
    adapter: stub  # stub | ffmpeg
    ffmpeg:
      enabled: false
      executable-path: /usr/bin/ffmpeg    # allowlisted absolute path
      working-directory: /tmp/turku-render # allowlisted directory
      timeout-seconds: 300                # bounded: max 600
      max-duration-seconds: 600           # video output limit
      max-resolution: 1920x1080           # bounded
      max-file-size-mb: 500               # bounded output size
      max-fps: 30                         # bounded
```

### Security Rules

| Rule | Enforcement |
|------|------------|
| No hardcoded secrets | Config loaded from environment/file only |
| No frontend-visible config | Config is backend-only, never exposed in API |
| No command logging with secrets | Sanitized logging — executable path only |
| Allowlisted executable path | Validated against known FFmpeg locations |
| Allowlisted working directory | Validated prefix, no traversal |
| Bounded timeout | Max 600 seconds, enforced by ProcessBuilder + watchdog |
| Bounded file size | Max 500MB output, checked post-render |
| Bounded duration | Max 600 seconds video, checked post-render |
| No shell interpolation | ProcessBuilder with explicit args, never shell |
| No user-controlled command args | All args generated from internal render plan only |

### Forbidden in Config

- No inline secrets (API keys, tokens)
- No cloud storage credentials in config
- No public URL templates
- No CDN config
- No publish/export target config

---

## 5. Render Input Policy

### Allowed Inputs

| Source | Description |
|--------|------------|
| Render manifest scene list | Internal scene refs from composition plan |
| Internal image refs | Opaque refs like `internal-scene-image:{uuid}` |
| Narration timing | Duration/offset from storyboard metadata |
| Target format/resolution/fps | From render plan config |

### Forbidden Inputs

| Source | Status |
|--------|:------:|
| Public URLs | ❌ |
| Signed/CDN URLs | ❌ |
| Storage keys | ❌ |
| Raw provider payloads | ❌ |
| Base64/binary in API | ❌ |
| User-controlled file paths | ❌ |
| External URLs | ❌ |
| Lyrics text body | ❌ |
| Copyrighted audio/video references | ❌ |

### Input Resolution Flow

```
TurkuRenderExecutionPlan
  → scene count, duration, resolution, fps, format
  → renderer resolves internal refs to local file paths (backend-only)
  → file paths NEVER exposed in API/DTO/response
```

---

## 6. Render Output / Artifact Policy

### Allowed Output Metadata

| Field | Example | Exposed in API |
|-------|---------|:--------------:|
| artifactRef | `internal-render:{uuid}` | ✅ (opaque) |
| format | `mp4` | ✅ |
| durationSeconds | `120.0` | ✅ |
| resolution | `1920x1080` | ✅ |
| sizeLabel | `~45MB` | ✅ |
| humanReviewRequired | `true` | ✅ |
| autoApproved | `false` | ✅ |

### Forbidden Output Fields

| Field | Status |
|-------|:------:|
| Absolute file path | ❌ Never in API |
| Storage key | ❌ Never in API |
| Public URL | ❌ Never |
| Signed URL | ❌ Never |
| CDN URL | ❌ Never |
| Download URL | ❌ Never |
| Base64 content | ❌ Never |
| Binary data | ❌ Never |
| Raw FFmpeg output | ❌ Never |

### Retention / Cleanup Design

| Policy | Value |
|--------|-------|
| Internal render output location | Allowlisted working directory only |
| Retention period | Config-driven, default 24h |
| Cleanup trigger | Scheduled or on new render |
| Cleanup scope | Working directory only |
| Orphan detection | Artifacts without active job reference |
| No external storage | Internal filesystem only in FP-023 |

---

## 7. FFmpeg Safety Boundary

### Process Execution Model

```java
// SAFE: ProcessBuilder with explicit args
ProcessBuilder pb = new ProcessBuilder(
    config.getExecutablePath(),     // allowlisted
    "-i", inputPath,                // resolved internally
    "-c:v", "libx264",             // hardcoded codec
    "-preset", "medium",            // hardcoded preset
    "-t", String.valueOf(maxDuration), // bounded
    "-y", outputPath               // in allowlisted directory
);
pb.directory(new File(config.getWorkingDirectory()));
pb.redirectErrorStream(true);

// FORBIDDEN: never use shell execution
// Runtime.getRuntime().exec("ffmpeg " + args)  // NEVER
// ProcessBuilder("sh", "-c", command)          // NEVER
```

### Argument Builder Contract

```java
public interface TurkuFfmpegArgumentBuilder {
    List<String> buildArgs(TurkuRenderExecutionPlan plan, 
                           TurkuFfmpegConfig config,
                           Path inputPath, 
                           Path outputPath);
}
```

**Rules:**
- All args must be from internal render plan or hardcoded codec/preset values
- No string interpolation from user input
- No shell metacharacters
- No environment variable expansion
- Output validated against allowlisted patterns

### File Allowlists

| Type | Policy |
|------|--------|
| Input files | Must be under allowlisted input directory |
| Output files | Must be under allowlisted working directory |
| Path traversal | Rejected (no `..`, no symlink resolution beyond allowlist) |
| File extension | Allowlisted: `.mp4`, `.webm` only |

### Timeout / Kill Behavior

| Parameter | Value | Enforcement |
|-----------|-------|------------|
| Process timeout | Config `timeout-seconds`, max 600 | `Process.waitFor(timeout, SECONDS)` |
| Kill on timeout | `Process.destroyForcibly()` | Immediate |
| Kill on shutdown | Registered shutdown hook | Best-effort |
| Output size check | Post-render, before artifact registration | Reject if over limit |
| Duration check | Post-render metadata validation | Reject if over limit |

### Resolution / Duration / Size Bounds

| Parameter | Default Max | Absolute Max |
|-----------|:-----------:|:------------:|
| Resolution | 1920×1080 | 3840×2160 |
| Duration | 600s | 900s |
| File size | 500MB | 1GB |
| FPS | 30 | 60 |

---

## 8. Renderer Failure Model

### Failure Codes

| Code | Trigger |
|------|---------|
| `RENDER_PROVIDER_DISABLED` | FFmpeg adapter not enabled in config |
| `RENDER_CONFIG_MISSING` | Required config field absent or invalid |
| `RENDER_INPUT_MISSING` | Internal input ref could not be resolved to file |
| `RENDER_TIMEOUT` | Process exceeded timeout-seconds |
| `RENDER_PROCESS_FAILED` | FFmpeg process exited with non-zero code |
| `RENDER_OUTPUT_REJECTED` | Output exceeds size/duration/resolution limits |
| `RENDER_SECURITY_VIOLATION` | Path traversal, forbidden arg, or shell injection attempt |
| `RENDER_FAILED_SAFE` | Catch-all safe failure (no partial output) |

### Failure Behavior

- All failures produce `TurkuRenderExecutionResult(success=false, ...)`
- Partial output files are deleted on failure
- No partial artifact registration
- Failure code + sanitized message logged
- No raw FFmpeg stderr in API response
- No file path in API response
- Job transitions to `RENDER_JOB_FAILED_SAFE`

### Existing Failure Codes (FP-022) — Preserved

| Code | Source |
|------|--------|
| `MANIFEST_INCOMPLETE` | FP-022 |
| `READINESS_CHECK_FAILED` | FP-022 |
| `RENDERER_UNAVAILABLE` | FP-022 |
| `RENDERER_TIMEOUT` | FP-022 |
| `RENDERER_INTERNAL_ERROR` | FP-022 |
| `SAFETY_VIOLATION` | FP-022 |
| `COPYRIGHT_REVIEW_PENDING` | FP-022 |
| `HUMAN_REVIEW_REQUIRED` | FP-022 |

FP-023 adds the new codes above. No removal of existing codes.

---

## 9. Workflow Compatibility

### Existing FP-022 Endpoints — Unchanged

| Method | Path | Status |
|--------|------|:------:|
| POST | `/admin/turku-video-projects/{id}/prepare-render-job` | ✅ Reused |
| GET | `/admin/turku-video-projects/{id}/render-job` | ✅ Reused |
| POST | `/admin/turku-video-projects/{id}/run-render-job` | ✅ Reused |
| GET | `/admin/turku-video-projects/{id}/render-execution-safety` | ✅ Reused |

### Endpoint Inventory Policy

- Task 01: No endpoint changes (44 = 25 POST + 19 GET)
- Future tasks: Minimize additions. Reuse existing `run-render-job` for both stub and FFmpeg
- No endpoint explosion — adapter selection is internal, not a separate endpoint

### Stage Machine — Unchanged

All 19 existing stages preserved. No new stages in FP-023 unless required for artifact lifecycle.

---

## 10. Admin Review Gates

### Immutable Safety Rules

| Rule | Value | Overridable |
|------|-------|:-----------:|
| humanReviewRequired | `true` | ❌ Never |
| copyrightReviewRequired | `true` | ❌ Never |
| autoApproved | `false` | ❌ Never |
| notPublishReady | `true` | ❌ Never |
| notExportReady | `true` | ❌ Never |

### Completed Internal Render ≠ Publish Ready

A successfully rendered video file is:
- ✅ Available for admin internal review (metadata only)
- ❌ NOT available for public viewing
- ❌ NOT available for download
- ❌ NOT available for export
- ❌ NOT available for publish
- ❌ NOT auto-approved

### SYSTEM Actor Blocked

- `ReviewActorRole.SYSTEM` remains blocked on all state-changing POST endpoints
- No auto-approval path
- No scheduled/automated publish
- `actorRole` remains `TEST_FALLBACK` compatibility input only

---

## 11. Observability / Audit

### Safe Logging

| What | Logged | Format |
|------|:------:|--------|
| Render job ID | ✅ | UUID |
| Attempt ID | ✅ | UUID |
| Renderer alias | ✅ | `deterministic-stub` or `ffmpeg-local` |
| Render mode | ✅ | `DETERMINISTIC_STUB` or `FFMPEG_LOCAL` |
| Duration (ms) | ✅ | Numeric |
| Failure code | ✅ | Enum name |
| Safe trace ref | ✅ | `stub-trace:{uuid}` or `ffmpeg-trace:{uuid}` |

### Forbidden in Logs

| What | Status |
|------|:------:|
| FFmpeg command line | ❌ |
| File system paths | ❌ |
| Config values | ❌ |
| Secrets/API keys | ❌ |
| FFmpeg stderr output | ❌ (sanitized summary only) |
| Input file contents | ❌ |
| Output file contents | ❌ |

### Audit Trail

- Each render attempt stored as `TurkuRenderAttempt` with safe metadata only
- Failure reasons stored as enum codes, never raw error strings
- Trace refs are opaque UUIDs, never contain paths/URLs

---

## 12. Frontend / Admin Implications

### Admin UI Changes (Future Tasks)

| Element | Display | Status |
|---------|---------|:------:|
| Renderer mode label | `Deterministic Stub` / `FFmpeg Local` | ✅ Safe text |
| Artifact ref | Opaque `internal-render:{uuid}` | ✅ Safe text |
| Artifact metadata | format, duration, resolution, size label | ✅ Safe text |
| Failure code | Enum display name | ✅ Safe text |
| Render duration | Milliseconds | ✅ Safe number |

### Forbidden UI Elements

| Element | Status |
|---------|:------:|
| Video player (`<video>`) | ❌ Not in FP-023 |
| Download button | ❌ Never |
| Export button | ❌ Never |
| Publish button | ❌ Never |
| File path display | ❌ Never |
| Storage key display | ❌ Never |
| Public/signed/CDN URL | ❌ Never |
| FFmpeg command display | ❌ Never |
| Raw stderr display | ❌ Never |
| Binary/base64 content | ❌ Never |

---

## 13. Safety Baseline

### Standing Rules — FP-023 Preserves All

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No generic status update | ✅ |
| No provider/storage/raw/base64/secret exposure | ✅ |
| No unsafe HTML (dangerouslySetInnerHTML/innerHTML) | ✅ |
| No token/JWT/session/principal/authorizationHeader | ✅ |
| No copyrighted lyrics/audio/video reproduction | ✅ |
| No rendered video file exposed in API | ✅ |
| No downloadable file | ✅ |
| No video player | ✅ |
| No FFmpeg command/secret leakage | ✅ |
| SYSTEM blocked on POST endpoints | ✅ |
| 34+ forbidden DTO fields verified absent | ✅ |
| No PUT/DELETE endpoints | ✅ |
| No shell execution | ✅ |
| No user-controlled command args | ✅ |

---

## 14. Proposed FP-023 Task Plan

| Task | Title | Scope | Endpoints |
|------|-------|-------|:---------:|
| **01** | Design (this doc) | Documentation-only | 0 |
| **02** | Render Adapter Contract + Secure Command Builder Backend | `TurkuFfmpegConfig`, `TurkuFfmpegArgumentBuilder`, `TurkuRenderAdapterResolver`, new failure codes, tests | 0 |
| **03** | FFmpeg Stub/Sandbox Runner Backend (disabled by default) | `FfmpegTurkuRenderRunnerGateway` (disabled), sandbox execution, timeout/kill, file bounds check, tests | 0 |
| **04** | Internal Artifact Metadata + Cleanup Policy Backend | Artifact lifecycle, retention config, cleanup service, safe artifact DTO fields, tests | 0–1 |
| **05** | Admin UI Renderer Mode / Artifact Safety Visibility | Frontend renderer mode display, artifact metadata panel, safety labels, no video player, tests | 0 |
| **06** | E2E Render Adapter Safety Regression + Closure | Full regression, safety verification, closure doc | 0 |

### Endpoint Budget

- FP-023 target: 0 new endpoints (reuse existing `run-render-job`)
- If artifact lifecycle requires a read endpoint: max +1 GET
- Final inventory: 44–45 endpoints

---

## 15. Acceptance Criteria for FP-023 Closure

- [ ] `FfmpegTurkuRenderRunnerGateway` exists, disabled by default
- [ ] `TurkuFfmpegConfig` with bounded/validated fields
- [ ] `TurkuFfmpegArgumentBuilder` with strict arg generation
- [ ] All forbidden fields absent from DTOs (34+ checked)
- [ ] All forbidden UI controls absent
- [ ] SYSTEM blocked on POST
- [ ] No shell execution in codebase
- [ ] No config/secrets in API responses
- [ ] No file paths in API responses
- [ ] No publish/export/download endpoints or behavior
- [ ] Safety envelope flags include render adapter mode
- [ ] Failure codes cover all FP-023 failure paths
- [ ] Artifact cleanup policy tested
- [ ] Backend + frontend tests green
- [ ] Closure doc written

---

## 16. Task 02 Readiness Checklist

Before starting Task 02, verify:

- [x] FP-022 closed, all tests green
- [x] `TurkuRenderRunnerGateway` interface stable
- [x] `StubTurkuRenderRunnerGateway` works as fallback
- [x] `TurkuRenderExecutionPlan` has scene count, duration, resolution, fps, format, renderer alias, render mode
- [x] `TurkuRenderExecutionResult` has success, durationMs, failureCode, artifact, safeTraceRef
- [x] `TurkuRenderFailureCode` enum extensible
- [x] Endpoint inventory: 44 = 25 POST + 19 GET
- [x] Safety baseline documented and tested
- [x] This design doc accepted

### Task 02 Deliverables

1. `TurkuFfmpegConfig` — bounded config POJO (not Spring-wired yet)
2. `TurkuFfmpegArgumentBuilder` — strict arg builder, no shell
3. `TurkuRenderAdapterResolver` — selects stub or FFmpeg based on config
4. New failure codes: `RENDER_PROVIDER_DISABLED`, `RENDER_CONFIG_MISSING`, `RENDER_INPUT_MISSING`, `RENDER_SECURITY_VIOLATION`, `RENDER_OUTPUT_REJECTED`
5. Tests for config validation, arg builder, adapter resolution, failure codes
6. Doc: `docs/05-engineering/140-fp023-render-adapter-contract-secure-command-builder.md`
