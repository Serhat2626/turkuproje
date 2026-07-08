# FP-051 — Admin Internal Dry Run Record & Audit-Safe History Foundation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-051: Dry Run Record & History Foundation |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4368 passed, 0 failures |
| **Frontend Tests** | 2198 passed, 0 failures |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Add an audit-safe internal dry-run record/history foundation for admin-triggered dry runs.
Metadata-only record modeling + safe admin visibility. In-memory only, no persistence migration.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| TurkuInternalDryRunRecordStatus | 7 safe statuses (no READY_FOR_PUBLISH/COMPLETED/AUTO_APPROVED) |
| TurkuInternalDryRunRecordIssueCode | 10 issue codes |
| TurkuInternalDryRunRecord | Safe metadata-only record (no forbidden fields) |
| TurkuInternalDryRunRecordSummary | Admin-facing summary DTO (internalOnly=true) |
| TurkuInternalDryRunRecordService | In-memory record creation + history |

---

## 4. Safe Record Fields

- opaqueRecordRef / opaqueDryRunRef / opaqueStoryboardRef / opaqueReviewPackageRef (opaque)
- status / issueCodes (enum-safe)
- sceneCount / generatedClipCount / inputClipCount (integer)
- stitchedArtifactPresent (boolean)
- durationSeconds / fps / width / height (bounded integer)
- sizeBytes (long)
- createdAt / updatedAt (Instant)
- internalOnly (always true in summary)

---

## 5. Forbidden Fields (swept)

- ❌ stdout/stderr/rawLog/commandLine/shellCommand/executablePath
- ❌ filePath/absolutePath/inputPath/outputPath/workspacePath
- ❌ storageKey/publicUrl/signedUrl/cdnUrl/storageProvider
- ❌ base64/binaryData/rawPayload/downloadUrl/publishUrl
- ❌ token/jwt/session/principal/authorizationHeader
- ❌ lyrics/scriptBody/promptText/copyrightedContent/userMedia

---

## 6. Frontend

- InternalDryRunHistoryPanel.tsx: metadata-only history panel
- Types: TurkuInternalDryRunRecordStatus, IssueCode, RecordSummary
- Türkçe labels preserved
- No player/preview/download/export/publish controls
- No unsafe HTML

---

## 7. Tests

### Backend (17 new tests in 8 groups)
- A. Valid dry-run → safe record (opaque refs, metadata)
- B. Rejected/failed record (null, rejected, direct)
- C. Record summary (safe metadata, internalOnly)
- D. History (accumulates, all internal)
- E. Forbidden field sweep (3 classes)
- F. No publish/export statuses (enum sizes)
- G. Endpoint inventory (46 = 26 POST + 20 GET)
- H. ProcessBuilder boundary

### Frontend (13 new tests in 7 groups)
- A. Record status labels (Türkçe)
- B. Record success checks
- C. Metadata summary rendering
- D. History summary
- E. No forbidden controls
- F. Panel source safety
- G. Type safety

---

## 8. Test Baseline

| Suite | Count |
|-------|-------|
| Backend | 4368 |
| Frontend | 2198 |
| Combined | 6566 |

---

## 9. Final Handoff

- **FP-051 is CLOSED**
- Next feature may start from **FP-052**
