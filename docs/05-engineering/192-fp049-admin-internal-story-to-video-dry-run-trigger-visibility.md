# FP-049 — Admin Internal Story-to-Video Dry Run Trigger + Visibility

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-049: Admin Internal Dry Run Trigger |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4337 passed, 0 failures |
| **Frontend Tests** | 2172 passed, 0 failures |
| **Endpoints** | **46 = 26 POST + 20 GET** (+1 POST, +1 GET) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

Add a guarded admin/internal action to trigger an internal story-to-video dry run from safe storyboard metadata and show metadata-only result in admin UI.

This is still internal-only. No public delivery, no playback, no download/export/publish, no storage URL, no real user media.

---

## 3. New Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/turku-video-projects/{id}/trigger-internal-dry-run` | Trigger FP-047 chain via fake/test-double |
| GET | `/admin/turku-video-projects/{id}/internal-dry-run-status` | Get dry-run metadata status |

Both admin-only. No public delivery. No artifact bytes/URLs.

---

## 4. Safe Request Fields

| Field | Type |
|-------|------|
| `opaqueStoryboardRef` | String (opaque) |
| `opaqueStoryRef` | String (opaque) |
| `sceneInputs` | List of safe scene metadata |

Each scene input contains: `opaqueSceneRef`, `sceneOrder`, `targetDurationSeconds`, `targetFps`, `targetWidth`, `targetHeight`, `targetFormat`.

---

## 5. Safe Response Fields

| Field | Type |
|-------|------|
| `opaqueChainRef` | String (opaque) |
| `opaqueStoryboardRef` | String (opaque) |
| `status` | Chain status enum |
| `issueCodes` | Chain issue code list |
| `sceneCount` | int |
| `generatedClipCount` | int |
| `stitchedArtifactPresent` | boolean |
| `reviewPackageStatus` | String (enum name) |
| `adminMetadataVisible` | boolean |
| `totalDurationSeconds` | int |
| `maxWidth/maxHeight/maxFps` | int |
| `totalSizeBytes` | long |
| `internalOnly` | always true |

---

## 6. Admin UI Behavior

- Trigger button clearly labeled internal dry run
- Renders metadata-only summary
- Shows blocked/failed states via issue codes only
- No player/preview/download/export/publish controls
- No public URL/link rendering
- No unsafe HTML

---

## 7. Forbidden Controls/Fields

- ❌ No player/preview/download/export/publish
- ❌ No storage/public/signed/CDN URL
- ❌ No path/command/stdout/stderr/rawLog
- ❌ No token/JWT/session/principal/auth header
- ❌ No prompt/script/lyrics/user media
- ❌ No auto-approval
- ❌ No READY_FOR_PUBLISH / COMPLETED

---

## 8. CI / FFmpeg Boundary

- CI remains fake/test-double based
- Real FFmpeg remains local/manual-only
- ProcessBuilder only in TurkuFfmpegLocalDevProcessRunner
- Runtime.exec absent

---

## 9. Tests

### Backend (9 new tests)
- A. Valid trigger → ADMIN_METADATA_VISIBLE
- B. Unsafe request → REJECTED_UNSAFE
- C. Request DTO no forbidden fields
- D. Response DTO no forbidden fields + internalOnly
- E. Endpoint inventory (46 = 26 POST + 20 GET)
- F. ProcessBuilder inventory
- G. Endpoint admin-only

### Frontend (12 new tests)
- A. Status labels
- B. Success check
- C. Metadata summary
- D. No forbidden controls
- E. No player/download/export/publish
- F. Response type safety

---

## 10. Test Baseline

| Suite | Count |
|-------|-------|
| Backend | 4337 |
| Frontend | 2172 |
| Combined | 6509 |

---

## 11. Final Handoff

- **FP-049 is CLOSED**
- Next feature may start from **FP-050**
