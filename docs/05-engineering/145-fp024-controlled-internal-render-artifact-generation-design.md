# FP-024 — Controlled Internal Render Artifact Generation Design

## 1. Baseline and Status

| Field | Value |
|-------|-------|
| **Feature** | FP-024 — Controlled Internal Render Artifact Generation with Real FFmpeg Disabled-by-Default Config |
| **Predecessor** | FP-023 (CLOSED) |
| **Latest Commit** | 6ac366b |
| **Backend Tests** | 3530, 0 failures |
| **Frontend Tests** | 1620, 0 failures |
| **Combined** | 5150 |
| **Endpoints** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |

### Current State

- FP-022: Render job + stub execution (opaque internal artifact refs)
- FP-023: Render adapter contract, secure FFmpeg command builder, FFmpeg disabled-by-default sandbox, artifact metadata/cleanup, admin safety visibility
- No real FFmpeg execution
- No real video binary
- No filesystem/storage integration
- No video player, no download/export/publish/public delivery

---

## 2. Product Goal

Move from sandbox/no-process render adapter toward **controlled internal render artifact generation**, while maintaining strict internal-only and admin-reviewed safety posture.

### What Changes

| Before (FP-023) | After (FP-024) |
|------------------|----------------|
| FFmpeg command plan built but never executed | FFmpeg can execute behind disabled-by-default config gate |
| Stub produces opaque artifact ref only | Real execution produces internal file + opaque ref |
| No filesystem involvement | Controlled workspace directory with safety validation |
| No rendered binary | Internal binary exists in bounded temp workspace |

### What Does NOT Change

- Artifact remains internal-only — no public access
- No download, export, publish, or public delivery
- No video player
- API never exposes file path, storage key, binary, base64, or public URL
- Human review always required, auto-approval never enabled
- Stub remains default renderer

---

## 3. Render Enablement Strategy

### Execution Priority Chain

```
1. Config absent      → STUB (default, always safe)
2. Config present     → validate config
3. Config invalid     → FFMPEG_CONFIG_MISSING (fail closed, use stub)
4. Config valid       → check ffmpeg.enabled flag
5. enabled=false      → FFMPEG_DISABLED (use stub)
6. enabled=true       → FFMPEG_READY (real execution allowed)
7. Execution fails    → FFMPEG_FAILED_SAFE (use stub on retry)
```

### Fail-Closed Principle

Any missing, malformed, or unsafe configuration element causes the system to fall back to stub renderer. Real FFmpeg execution requires **all** of:
- Config file/properties present
- `turku.render.ffmpeg.enabled=true` explicitly set
- Executable path validated and allowlisted
- Work directory validated and allowlisted
- All bounds (timeout, duration, resolution, size, fps) within safe limits

---

## 4. Secure Config Design

### Config Properties

```yaml
turku:
  render:
    ffmpeg:
      enabled: false                              # disabled by default
      executable-path: /usr/bin/ffmpeg             # must be allowlisted
      work-directory: /tmp/turku-render-workspace  # must be allowlisted
      temp-directory: /tmp/turku-render-temp       # must be allowlisted
      max-timeout-seconds: 120                     # hard kill after this
      max-duration-seconds: 600                    # max video duration
      max-output-size-mb: 500                      # max output file size
      max-resolution: 1920x1080                    # max resolution
      max-fps: 30                                  # max frame rate
      allowed-formats:                             # output format allowlist
        - mp4
        - webm
      allowed-codecs:                              # codec allowlist
        - h264
        - vp9
```

### Allowlist Validation

| Property | Validation |
|----------|-----------|
| executable-path | Must match canonical path in allowlist, must exist, must not contain shell metacharacters |
| work-directory | Must be canonical, within allowed parent dirs, writable, not system dir |
| temp-directory | Must be canonical, within allowed parent dirs, writable, auto-cleaned |
| formats | Must be in hardcoded allowlist |
| codecs | Must be in hardcoded allowlist |
| resolution | Must parse as WxH, both ≤ allowlisted max |
| fps | Must be 1–60 |
| duration | Must be 1–600 seconds |
| timeout | Must be 10–300 seconds |
| output size | Must be 1–1000 MB |

### What Config Must NOT Include

- User-controlled command arguments
- Shell command strings
- Environment variable injection
- Arbitrary input paths from request body
- Secrets, tokens, API keys
- Network URLs as input/output
- Cloud storage bucket/key references

---

## 5. Internal Artifact Generation Policy

### Generation Preconditions

Artifact generation requires **all** of:
1. Render job in PREPARED state
2. Render readiness report: READY
3. Composition plan exists
4. Render manifest exists
5. FFmpeg config valid and enabled
6. Human review not bypassed

### Artifact Reference Policy

| Rule | Enforced |
|------|:--------:|
| Artifact ref remains opaque (`internal-render:{uuid}`) | ✅ |
| API never exposes file path | ✅ |
| API never exposes storage key | ✅ |
| API never exposes binary/base64 | ✅ |
| API never exposes public/signed/CDN URL | ✅ |
| Artifact not downloadable | ✅ |
| Artifact not exportable | ✅ |
| Artifact not publishable | ✅ |
| File exists only in controlled workspace dir | ✅ |

---

## 6. File System Safety

### Canonical Path Validation

```
Input:  /tmp/turku-render-workspace/../../../etc/passwd
Step 1: Resolve to canonical → /etc/passwd
Step 2: Check parent → NOT within allowed workspace
Step 3: REJECT
```

### Directory Traversal Prevention

- All paths resolved to canonical form before use
- Output must be child of allowed workspace directory
- Input must be child of allowed temp directory
- No `..` segments accepted after canonicalization
- No symlink following beyond workspace boundary

### Extension Allowlist

Only these output extensions are accepted:
- `.mp4`
- `.webm`

Any other extension causes rejection and safe failure.

### Temp Cleanup Policy

- Temp files created per-render-job in isolated subdirectory
- Subdirectory named by job UUID
- Cleanup attempted after render completes (success or failure)
- Cleanup is best-effort — failure logged but not fatal
- No cleanup of files outside workspace boundary
- No recursive delete beyond job subdirectory

### No Arbitrary File Operations

| Forbidden | Enforced |
|-----------|:--------:|
| Read arbitrary file | ✅ |
| Write outside workspace | ✅ |
| Delete outside job temp dir | ✅ |
| Create symlinks | ✅ |
| Follow symlinks beyond workspace | ✅ |
| Frontend receives file paths | ✅ |
| Log absolute paths to API response | ✅ |

---

## 7. Renderer Execution Boundary

### Strict Command Builder Only

- `TurkuFfmpegCommandBuilder` remains the sole source of command arguments
- No user-supplied arguments in any position
- All arguments come from validated config + render execution plan
- Shell metacharacters rejected at build time

### ProcessBuilder Policy (Implementation Task)

```java
ProcessBuilder pb = new ProcessBuilder(commandPlan.toSafeArgList());
pb.redirectErrorStream(true);
pb.directory(workspaceDir);  // canonical, validated
// NO pb.command("sh", "-c", ...)  — never shell
// NO environment variable injection
// NO input pipe from user data
```

### Timeout and Kill Behavior

1. ProcessBuilder starts with bounded timeout from config
2. If process exceeds timeout → `process.destroyForcibly()`
3. Result: `FFMPEG_FAILED_SAFE` with `RENDER_TIMEOUT` failure code
4. Temp files cleaned up
5. Stub fallback available on retry

### Safe Failure Codes

| Code | Trigger |
|------|---------|
| RENDER_PROVIDER_DISABLED | Config absent or disabled |
| RENDER_CONFIG_MISSING | Config present but incomplete |
| RENDER_INPUT_MISSING | Required input files not found |
| RENDER_TIMEOUT | Process exceeded timeout |
| RENDER_PROCESS_FAILED | Non-zero exit code |
| RENDER_OUTPUT_REJECTED | Output failed verification |
| RENDER_SECURITY_VIOLATION | Path traversal / unsafe input |
| RENDER_FAILED_SAFE | General safe failure |

---

## 8. Artifact Verification

After FFmpeg process completes successfully:

| Check | Action on Failure |
|-------|-------------------|
| Output file exists | RENDER_OUTPUT_REJECTED |
| Output within workspace dir | RENDER_SECURITY_VIOLATION |
| Output size ≤ max | RENDER_OUTPUT_REJECTED |
| Output extension in allowlist | RENDER_OUTPUT_REJECTED |
| Output file readable | RENDER_OUTPUT_REJECTED |
| Output not empty (≥ 1KB) | RENDER_OUTPUT_REJECTED |
| Output canonical path matches expected | RENDER_SECURITY_VIOLATION |

### Post-Verification

- Create `TurkuInternalRenderArtifact` with opaque ref
- Record size, format, duration metadata
- Set `humanReviewRequired=true`, `autoApproved=false`
- Set `notPublishReady=true`, `notExportReady=true`
- File remains in workspace — not exposed via API

---

## 9. API Design Proposal

### Reuse Existing Endpoints

| Endpoint | Change |
|----------|--------|
| `POST /admin/turku-video-projects/{id}/run-render-job` | May use real FFmpeg if config enabled; response shape unchanged |
| `GET /admin/turku-video-projects/{id}/render-job` | May include real artifact metadata (opaque ref only) |
| `GET /admin/turku-video-projects/{id}/render-execution-safety` | May include renderer config mode in safety envelope |

### No New Endpoints Expected

Prefer 0 new endpoints. If needed for render config visibility:
- At most 1 GET: `/admin/turku-video-projects/{id}/render-config-status`
- Returns config mode only (STUB/FFMPEG_DISABLED/FFMPEG_READY) — no paths/secrets

### API Response Safety

| Rule | Enforced |
|------|:--------:|
| No file path in response | ✅ |
| No storage key in response | ✅ |
| No binary/base64 in response | ✅ |
| No public/signed/CDN URL | ✅ |
| No download URL | ✅ |
| No config secrets/paths in response | ✅ |
| No ffmpeg command line in response | ✅ |

---

## 10. Admin UI Implications

### Show Safely

- Renderer config mode badge (STUB / FFMPEG_DISABLED / FFMPEG_READY)
- Internal artifact generated status (yes/no/pending)
- Artifact opaque ref
- Renderer used label
- Safety envelope flags

### Still Forbidden

| Element | Status |
|---------|:------:|
| Video player | ❌ |
| Download button | ❌ |
| Export button | ❌ |
| Publish button | ❌ |
| File path display | ❌ |
| Storage key display | ❌ |
| Binary/base64 display | ❌ |
| Public/signed/CDN URL | ❌ |
| FFmpeg command line display | ❌ |

---

## 11. Safety Baseline

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
| No file path in API response | ✅ |
| No command line in API response | ✅ |
| humanReviewRequired=true | ✅ |
| copyrightReviewRequired=true | ✅ |
| autoApproved=false | ✅ |
| notPublishReady=true | ✅ |
| notExportReady=true | ✅ |

---

## 12. Proposed Dense FP-024 Task Plan

| Task | Title | Scope |
|------|-------|-------|
| **01** | **Design** | This document (documentation-only) |
| **02** | Secure Render Config Model + Validation Backend | Config model, validation, allowlists, fail-closed defaults. No real execution. |
| **03** | Internal Render Workspace + File Safety Backend | Workspace path validation, canonical checks, traversal prevention, extension allowlist, temp cleanup model. No real file I/O. |
| **04** | Controlled FFmpeg Execution Behind Disabled-by-Default Gate | ProcessBuilder integration behind config gate, timeout/kill, safe failure codes. Real execution only if config explicitly enables. |
| **05** | Internal Artifact Verification + Metadata Integration | Output verification (size/format/path), artifact metadata creation from real output, safety report. |
| **06** | Admin UI Render Config / Artifact Status Visibility | Frontend types, UI panels for config mode, artifact status, no video player/download. |
| **07** | E2E Internal Render Artifact Safety Regression + Closure | Full regression, closure doc, FP-024 CLOSED. |

---

## 13. Acceptance Criteria for FP-024 Closure

- [ ] Secure render config model exists with fail-closed defaults
- [ ] Config validation rejects unsafe/incomplete configs
- [ ] Workspace path validation prevents directory traversal
- [ ] Extension allowlist enforced
- [ ] FFmpeg execution disabled by default, enabled only with valid config
- [ ] ProcessBuilder used (not shell), with timeout and kill
- [ ] Output verified (exists, bounded size, correct format, within workspace)
- [ ] Artifact ref remains opaque in API
- [ ] API never exposes file path, storage key, binary, base64, public URL
- [ ] Admin UI shows config mode and artifact status safely
- [ ] No video player, download, export, publish
- [ ] humanReviewRequired=true, autoApproved=false on all paths
- [ ] Full E2E regression passes
- [ ] Endpoint inventory stable or minimally extended
- [ ] Migration/schema/config changes documented if any

---

## 14. Task 02 Readiness Checklist

Task 02 — Secure Render Config Model + Validation Backend — is ready when:

- [x] FP-024 design document exists and accepted
- [x] Config property structure defined (section 4)
- [x] Allowlist validation rules defined (section 4)
- [x] Fail-closed defaults documented (section 3)
- [x] No real FFmpeg execution in Task 02
- [x] No endpoint changes expected in Task 02
- [x] Safety baseline documented (section 11)
