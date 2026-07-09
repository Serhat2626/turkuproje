# LIVE-004 — Manual-Gated Real FFmpeg Render + Render Artifact Verification

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | LIVE-004: FFmpeg Render + Render Artifact Verification |
| **Phase** | LIVE — Controlled Real Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4869 (was 4853, +16) |
| **Frontend Tests** | 2712 (was 2691, +21) |
| **Combined** | 7581 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Enable manual-gated real FFmpeg render execution for verified internal image artifacts. CI/default remains fake. Public delivery/download/export/publish disabled.

---

## 3. Render Execution Modes

| Mode | Behavior |
|------|----------|
| CI_FAKE | Default. Deterministic fake render. No gates needed. |
| LOCAL_MANUAL_REAL_FFMPEG | Real FFmpeg. Requires kill switch off + operator gate + artifacts verified. Human review required. |

---

## 4. Kill Switch + Operator Gate Rules

1. **Kill switch active** → BLOCKED_BY_KILL_SWITCH (overrides all)
2. **Operator gate not passed** → BLOCKED_BY_OPERATOR_GATE
3. **Artifacts not verified** → BLOCKED_BY_ARTIFACT_READINESS
4. **All gates passed** → REAL_RENDER_AVAILABLE_INTERNAL

ProcessBuilder usage remains only in TurkuFfmpegLocalDevProcessRunner.

---

## 5. CI Fake vs Local/Manual Real FFmpeg

| Aspect | CI_FAKE | LOCAL_MANUAL_REAL_FFMPEG |
|--------|---------|-------------------------|
| Gates needed | None | All 3 |
| Render artifact | Fake verified | Real verified + human review |
| ProcessBuilder | Not used | In TurkuFfmpegLocalDevProcessRunner only |
| Artifact refs | frnd: prefix | rrnd: prefix |
| Command/path/log | Never exposed | Never exposed |

---

## 6. Render Artifact Verification

- Each render produces a `TurkuLiveRenderArtifact` with verified flag + note
- CI fake: auto-verified, no human review
- Real: auto-verified + human review required
- No command, path, stdout, stderr, rawLog in artifact metadata

---

## 7. Admin UI Behavior

- Render status with safe labels
- Kill switch / operator gate / artifact readiness states
- Render mode (CI Sahte / Yerel/Manuel FFmpeg)
- Render artifact with verification indicator
- Human review required indicator
- No command/path/log/storage URL display
- No download/export/publish/public delivery buttons

---

## 8. Changed Files

### Backend (6 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuLiveRenderMode.java | 2 modes |
| TurkuLiveRenderStatus.java | 7 statuses |
| TurkuLiveRenderIssueCode.java | 7 issue codes |
| TurkuLiveRenderArtifact.java | Sanitized render artifact |
| TurkuLiveRenderResult.java | Render result DTO |
| TurkuLiveRenderExecutionService.java | 3-gate render service |
| TurkuLiveRenderExecutionTest.java | 16 tests in 9 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +52 lines LIVE-004 types |
| LiveRenderExecutionPreview.tsx | Status, mode, gates, artifact |
| LiveRenderExecutionPreview.test.ts | 21 tests in 7 groups |

---

## 9. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4853 | 4869 | +16 |
| Frontend | 2691 | 2712 | +21 |
| Combined | 7544 | 7581 | +37 |

---

## 10. Recommended LIVE-005

LIVE-005: LIVE Phase Final Closure + Full Chain Regression
- Full LIVE chain regression (LIVE-001 → LIVE-004)
- End-to-end: credential → generation → storage → render → verification
- Safety sweep across all LIVE classes
- Phase closure documentation and readiness baseline

---

## 11. LIVE-004 Final Status

**LIVE-004: CLOSED**
