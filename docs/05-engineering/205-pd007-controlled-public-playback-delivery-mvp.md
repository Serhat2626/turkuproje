# PD-007 — Controlled Public Playback / Delivery MVP

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | PD-007: Controlled Public Playback MVP |
| **Phase** | PD — Production Delivery |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4519 passed, 0 failures |
| **Frontend Tests** | 2304 passed, 0 failures |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Purpose

First controlled public playback/delivery MVP for approved media. Works only after PD-006 delivery gate eligibility. Opaque playback handle — no direct URL/path/bucket exposure.

---

## 3. New Backend Models

| Class | Purpose |
|-------|---------|
| ProductionMediaPublicPlaybackStatus | 6 safe statuses |
| ProductionMediaPublicPlaybackIssueCode | 10 issue codes |
| ProductionMediaPublicPlaybackRequest | Gate + storage + prep metadata |
| ProductionMediaPublicPlaybackResult | Opaque handle, controlledPlaybackEligible |
| ProductionMediaPublicPlaybackPolicy | Gate/storage/format/size validation |
| ProductionMediaPublicPlaybackService | Full chain → opaque playback handle |

---

## 4. Frontend

| File | Purpose |
|------|---------|
| types/index.ts | +37 lines PD-007 types |
| ControlledPublicPlayback.tsx | Status labels, ready check, metadata, placeholder |
| ControlledPublicPlayback.test.ts | 19 tests in 8 groups |

---

## 5. Playback Handle Chain

PD-005 Human Review → PD-006 Delivery Gate → PD-007 Controlled Playback Handle

All prerequisites must pass. Result: opaque `pub-play:` ref.

---

## 6. Forbidden Fields (absent)

- ❌ publicUrl/signedUrl/cdnUrl/streamUrl/downloadUrl/publishUrl
- ❌ bucketName/objectKey/filePath/absolutePath
- ❌ stdout/stderr/rawLog/commandLine/shellCommand
- ❌ base64/binaryData/rawPayload
- ❌ token/jwt/session/principal/accessKey/secretKey/secret
- ❌ lyrics/scriptBody/promptText/copyrightedContent/userMedia

---

## 7. Test Baseline

| Suite | Count |
|-------|-------|
| Backend | 4519 |
| Frontend | 2304 |
| Combined | 6823 |

---

## 8. Final Handoff

- **PD-007 is CLOSED**
- Next: PD-008
