# FP-024 Task 03 — Internal Render Workspace + File Safety Backend

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-024 Task 03 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |
| **Frontend** | Unchanged |
| **Filesystem Access** | None |
| **Real File Creation** | None |

---

## Workspace Model Summary

### New Types (8)

| Type | Kind |
|------|------|
| TurkuRenderWorkspace | composite model |
| TurkuRenderWorkspacePolicy | policy (extensions, bounds) |
| TurkuRenderWorkspaceRef | opaque workspace ref |
| TurkuRenderSafeFileRef | opaque file ref |
| TurkuRenderWorkspaceValidationResult | validation result |
| TurkuRenderWorkspaceIssue | issue with 13 codes |
| TurkuRenderFileSafetyReport | immutable safety flags |
| TurkuRenderWorkspaceValidator | comprehensive validator |

---

## File Safety Validation Summary

### Pattern Rejection

| Pattern | Detection |
|---------|-----------|
| Path traversal (`..`, `/../`, `%2e%2e`) | ✅ |
| Absolute Unix paths (`/usr`, `/bin`, `/etc`, `/tmp`) | ✅ |
| Absolute Windows paths (`C:\`, `D:\`, `\\`) | ✅ |
| Relative paths (`dir/file`, `dir\file`) | ✅ |
| URLs (http/https/ftp/file/s3/gs/az) | ✅ |
| Storage keys (storageKey, publicUrl, signedUrl, cdnUrl) | ✅ |
| Base64/data URIs | ✅ |
| Secrets (password=, Bearer, Authorization:, jwt=) | ✅ |
| Commands (commandLine, shellCommand, /bin/bash) | ✅ |
| Shell metacharacters (`;`, `&&`, `\|\|`, `$(`, `` ` ``) | ✅ |

### Extension Allowlist

Allowed: `mp4`, `webm`, `png`, `jpg`, `jpeg`

All other extensions rejected.

---

## Opaque Ref Policy

- Workspace refs: `internal-workspace:{uuid}`
- File refs: `internal-file:{uuid}`
- No filesystem paths in any ref
- No storage keys, URLs, or secrets in refs
- All refs validated before acceptance

---

## No-Filesystem / No-File-Creation / No-Deletion Policy

- No `File`, `Path`, `Files` API usage
- No real directory creation
- No real file creation
- No real file deletion
- Workspace and file refs are metadata-only
- Cleanup is metadata-only (eligibility flag, no actual delete)

---

## Integration Summary

- Workspace model connects to render config/adapter via shared safety envelope patterns
- Workspace policy inherits bounded limits from config design
- Existing render job/execution behavior unchanged
- Stub remains default renderer
- FFmpeg real execution remains disabled

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
| 8 classes × 28 forbidden fields = 0 violations | ✅ |

---

## Deferred: Task 04

Task 04 — Controlled FFmpeg Execution Behind Disabled-by-Default Gate.
