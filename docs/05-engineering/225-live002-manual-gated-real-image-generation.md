# LIVE-002 — Manual-Gated Real Image Generation

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | LIVE-002: Manual-Gated Real Image Generation |
| **Phase** | LIVE — Controlled Real Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4841 (was 4827, +14) |
| **Frontend Tests** | 2676 (was 2655, +21) |
| **Combined** | 7517 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Enable manual-gated real image generation for approved safe prompt packages. CI/default stays fake. Real provider execution requires all three gates: kill switch inactive + operator gate passed + provider readiness confirmed.

---

## 3. Execution Modes

| Mode | Behavior |
|------|----------|
| CI_FAKE | Default. Deterministic fake results. No gates needed. |
| LOCAL_MANUAL_REAL_PROVIDER | Real provider call. Requires all 3 gates. Results require human review. |

---

## 4. Kill Switch + Operator Gate Rules

1. **Kill switch active** → BLOCKED_BY_KILL_SWITCH (overrides all)
2. **Operator gate not passed** → BLOCKED_BY_OPERATOR_GATE
3. **Provider not ready** → BLOCKED_BY_PROVIDER_READINESS
4. **All gates passed** → REAL_PROVIDER_RESULT_AVAILABLE

Gate evaluation order: kill switch → operator gate → provider readiness.

---

## 5. CI Fake vs Local/Manual Real Provider

| Aspect | CI_FAKE | LOCAL_MANUAL_REAL_PROVIDER |
|--------|---------|---------------------------|
| Gates needed | None | All 3 |
| Results | Deterministic fake | Sanitized real provider result |
| Human review | Not required | Always required |
| Provider call | None | Simulated (real in future) |
| Artifact refs | fimg: prefix | limg: prefix |

---

## 6. Admin UI Behavior

- Generation status with safe labels
- Kill switch state (AKTIF/Devre disi)
- Operator gate state (Gecildi/Bekleniyor)
- Provider readiness state
- Generation mode (CI Sahte / Yerel/Manuel Gercek)
- Sanitized result metadata per scene
- Human review required indicator
- No render/download/export/publish/FFmpeg buttons

---

## 7. Changed Files

### Backend (5 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuLiveGenerationMode.java | 2 modes |
| TurkuLiveGenerationStatus.java | 7 statuses |
| TurkuLiveImageResultMetadata.java | Sanitized result metadata |
| TurkuLiveGenerationResult.java | Generation result DTO |
| TurkuLiveImageGenerationService.java | Gate-checked generation service |
| TurkuLiveImageGenerationTest.java | 14 tests in 9 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +43 lines LIVE-002 types |
| LiveImageGenerationPreview.tsx | Status, mode, gates, results |
| LiveImageGenerationPreview.test.ts | 21 tests in 7 groups |

---

## 8. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4827 | 4841 | +14 |
| Frontend | 2655 | 2676 | +21 |
| Combined | 7482 | 7517 | +35 |

---

## 9. Recommended LIVE-003

LIVE-003: Real Provider API Integration + Safe Response Capture
- Actual HTTP client call to real image provider (OpenAI-compatible)
- Kill switch + operator gate enforcement at HTTP level
- Safe response sanitization pipeline
- No render/FFmpeg/storage/publish

---

## 10. LIVE-002 Final Status

**LIVE-002: CLOSED**
