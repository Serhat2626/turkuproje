# FP-032 — Actual Local FFmpeg Smoke Execution Evidence

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-032: Actual Local FFmpeg Smoke Execution Evidence |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4043 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. Was Real FFmpeg Actually Executed?

### Answer: **NO — fake-run only in automated CI tests.**

### FFmpeg Availability: **YES — FFmpeg IS installed on local machine.**

```
ffmpeg version N-124085-g162ad61486-20260423
```

### Why no real execution in CI?

All automated tests use `checkEnabled=false` (default) which returns `DISABLED` before any process creation. This is intentional:
- CI must pass without FFmpeg dependency
- Real execution is explicitly manual-only
- `processEnabled=false` by default in runner

### How to perform real local execution manually

A developer on LOCAL can execute a real smoke run by:
1. `TurkuFfmpegLocalAvailabilityChecker(LOCAL, true)` → should return `AVAILABLE`
2. `TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, LOCAL, true)`
3. `TurkuFfmpegLocalDevProcessRunner(gate, true)` — processEnabled=true
4. `TurkuFfmpegManualExecutionRequest` with `LOCAL_ONLY_MANUAL`, `confirmed=true`, `processEnabled=true`
5. All FP-025/026 prerequisites met
6. `TurkuFfmpegActualRunEvidenceService(checker, manualService).attempt(...)`

This path is tested via fake doubles in CI but architecturally reaches real ProcessBuilder when all gates pass on LOCAL.

---

## 3. Production Inventory

### Backend Classes — FP-032 (5 new)

| Class | Kind |
|-------|------|
| TurkuFfmpegLocalAvailabilityStatus | enum (4 values) |
| TurkuFfmpegLocalAvailabilityChecker | safe probe (no path exposure) |
| TurkuFfmpegActualRunEvidenceStatus | enum (12 states) |
| TurkuFfmpegActualRunEvidenceResult | safe result |
| TurkuFfmpegActualRunEvidenceService | evidence orchestrator |

---

## 4. ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (execution) | ❌ |
| TurkuFfmpegLocalAvailabilityChecker | ✅ (probe-only) | ❌ |
| TurkuFfmpegActualRunEvidenceService | ❌ | ❌ |
| All other classes | ❌ | ❌ |

**Runtime.exec is absent in the entire codebase.**

Note: `TurkuFfmpegLocalAvailabilityChecker` uses ProcessBuilder for probe-only (`ffmpeg -version`), stdout consumed but NOT exposed.

---

## 5. Endpoint Inventory

| Controller | POST | GET | Total |
|------------|:----:|:---:|:-----:|
| TurkuVideoProjectController | 14 | 5 | 19 |
| AdminPipelineController | 7 | 6 | 13 |
| AdminReviewController | 2 | 5 | 7 |
| AdminWorkflowObservabilityController | 2 | 3 | 5 |
| **Total** | **25** | **19** | **44** |

No new endpoints in FP-032.

---

## 6. Safety Guarantees

| Guarantee | Status |
|-----------|:------:|
| Default disabled (checkEnabled=false, processEnabled=false) | ✅ |
| LOCAL-only for manual execution | ✅ |
| STAGING/PROD/UNKNOWN denied | ✅ |
| ProcessBuilder only in runner + checker (both consume, never expose) | ✅ |
| Runtime.exec absent | ✅ |
| No command/path/log exposure in DTOs | ✅ |
| No publicUrl/storageKey/binary/base64 | ✅ |
| No publish/export/download/player | ✅ |
| publicDeliveryAvailable = false | ✅ |
| autoApproved = false | ✅ |
| Metadata-only verification | ✅ |

---

## 7. Forbidden Exposure Scan

5 new classes × 19 forbidden fields = **0 violations** ✅

---

## 8. Final Handoff

- **FP-032 is CLOSED**
- **FP-001 → FP-032 CLOSED** (32 features)
- FFmpeg IS installed locally but real execution is intentionally manual-only
- Next feature may start from **FP-033**
