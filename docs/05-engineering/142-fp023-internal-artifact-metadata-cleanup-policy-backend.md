# FP-023 Task 04 — Internal Artifact Metadata + Cleanup Policy Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-023 Task 04 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |

---

## Artifact Metadata Model Summary

| Type | Kind | Description |
|------|------|------------|
| TurkuRenderArtifactStatus | enum (7) | CREATED, VALIDATED, RETENTION_ACTIVE, CLEANUP_ELIGIBLE, CLEANUP_PLANNED, CLEANED, REJECTED |
| TurkuRenderArtifactReference | record | Opaque ref, renderer mode, format, duration, resolution, size label |
| TurkuRenderArtifactRetentionPolicy | record | Bounded retention (1-168h, default 24h), cleanup flag |
| TurkuRenderArtifactCleanupPlan | record | Cleanup eligibility, reason, retention policy |
| TurkuRenderArtifactSafetyReport | record | Immutable safety flags, review flags |
| TurkuInternalRenderArtifact | composite | Reference + safety + retention + status |
| TurkuRenderArtifactService | service | Create, validate, cleanup plan, safety report |

---

## Cleanup / Retention Policy Summary

- **Default retention**: 24 hours
- **Min/Max**: 1h – 168h (clamped)
- **Auto cleanup**: disabled by default
- **Cleanup plan**: metadata-only — no real file deletion
- **No filesystem access**: no File I/O, no storage provider calls
- **No real deletion**: cleanup is a status transition, not file removal

---

## Integration Summary

- `TurkuRenderArtifactService.createFromExecutionResult()` creates safe artifact metadata from any `TurkuRenderExecutionResult`
- Works with both stub and FFmpeg sandbox results
- Validates opaque refs against 30+ forbidden patterns
- Rejects file paths, URLs, storage keys, base64, secrets, shell commands
- Existing `run-render-job` behavior unchanged

---

## Opaque Ref Policy

### Accepted Patterns
- `internal-stub-artifact:{uuid}`
- `internal-ffmpeg-sandbox:{uuid}`
- `internal-render:{uuid}`

### Rejected Patterns (30+)
- HTTP/HTTPS/FTP/file/S3/GCS/Azure URLs
- Absolute file paths (Unix and Windows)
- publicUrl, signedUrl, cdnUrl, storageKey
- base64, data URIs
- Bearer tokens, Authorization headers
- Passwords, secrets, API keys
- Shell metacharacters

---

## Safety Baseline Preserved

| Rule | Status |
|------|:------:|
| No publish/export | ✅ |
| No download | ✅ |
| No public/signed/CDN delivery | ✅ |
| No auto-approval | ✅ |
| No filesystem access | ✅ |
| No storage provider calls | ✅ |
| No binary/base64 exposure | ✅ |
| No token/JWT/session exposure | ✅ |
| No unsafe HTML | ✅ |
| No copyrighted media | ✅ |
| humanReviewRequired=true | ✅ |
| copyrightReviewRequired=true | ✅ |
| autoApproved=false | ✅ |
| notPublishReady=true | ✅ |
| notExportReady=true | ✅ |

---

## Deferred: Task 05

Task 05 — Admin UI Renderer Mode / Artifact Safety Visibility.
