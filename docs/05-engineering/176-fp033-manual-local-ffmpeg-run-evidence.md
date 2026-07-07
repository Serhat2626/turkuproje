# FP-033 — Manual Local FFmpeg Run Evidence

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-033: Manual Local FFmpeg Run Evidence |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4058 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. Was Real FFmpeg Actually Executed?

### Answer: **YES** ✅

Real FFmpeg was executed via `ProcessBuilder` through the gated runner path during test execution.

### Evidence

| Item | Detail |
|------|--------|
| **FFmpeg binary** | Installed locally: `N-124085-g162ad61486-20260423` |
| **Execution path** | `TurkuFfmpegManualRunHarness` → `TurkuFfmpegLocalDevProcessRunner.executeProcess()` |
| **ProcessBuilder invoked** | YES — `ProcessBuilder("ffmpeg", "-version")` |
| **Exit code** | 0 |
| **Runner status** | `PROCESS_COMPLETED_SAFE` |
| **Harness status** | `REAL_LOCAL_EXECUTION_SUCCEEDED` |
| **Environment** | LOCAL |
| **Mode** | REAL_DEV_TEST_ONLY |
| **processEnabled** | true (explicitly set in test) |
| **manualRunEnabled** | true (explicitly set in test) |
| **Gate passed** | YES |

### What was NOT exposed

- ❌ No stdout/stderr content leaked to DTO/API/UI
- ❌ No command line string exposed
- ❌ No executable path exposed
- ❌ No output file path exposed
- ❌ No binary/base64 data exposed
- ❌ No public URL generated

### How execution happened

Test class `TurkuFfmpegManualRunEvidenceTest.LocalSuccess`:
1. Created `TurkuFfmpegExecutionGate(REAL_DEV_TEST_ONLY, LOCAL, true)`
2. Created `TurkuFfmpegLocalDevProcessRunner(gate, true)` — processEnabled=true
3. Created `TurkuFfmpegManualRunHarness(runner, true)` — manualRunEnabled=true
4. Called `harness.attempt(request, SYSTEM_FFMPEG_LOCAL)`
5. Runner called `ProcessBuilder("ffmpeg", "-version").start()`
6. Process completed with exit 0
7. Status: `REAL_LOCAL_EXECUTION_SUCCEEDED`
8. Assertion passed ✅

---

## 3. Production Inventory

### Backend Classes — FP-033 (3 new)

| Class | Kind |
|-------|------|
| TurkuFfmpegManualRunEvidenceStatus | enum (10 states) |
| TurkuFfmpegManualRunEvidenceResult | safe result |
| TurkuFfmpegManualRunHarness | manual-only harness (disabled by default) |

---

## 4. ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (ONLY allowed) | ❌ |
| TurkuFfmpegManualRunHarness | ❌ (delegates to runner) | ❌ |
| All other classes | ❌ | ❌ |

**Runtime.exec is absent in the entire codebase.**

---

## 5. Endpoint Inventory

| Controller | POST | GET | Total |
|------------|:----:|:---:|:-----:|
| TurkuVideoProjectController | 14 | 5 | 19 |
| AdminPipelineController | 7 | 6 | 13 |
| AdminReviewController | 2 | 5 | 7 |
| AdminWorkflowObservabilityController | 2 | 3 | 5 |
| **Total** | **25** | **19** | **44** |

No new endpoints in FP-033.

---

## 6. Safety Guarantees

| Guarantee | Status |
|-----------|:------:|
| Manual harness disabled by default | ✅ |
| processEnabled=false by default | ✅ |
| Real execution only with explicit LOCAL + all flags | ✅ |
| STAGING/PROD/UNKNOWN denied | ✅ |
| ProcessBuilder only in LocalDevProcessRunner | ✅ |
| Runtime.exec absent | ✅ |
| No command/path/log exposure | ✅ |
| No publicUrl/storageKey/binary/base64 | ✅ |
| No publish/export/download/player | ✅ |
| publicDeliveryAvailable = false | ✅ |
| autoApproved = false | ✅ |

---

## 7. Forbidden Exposure Scan

3 new classes × 19 forbidden fields = **0 violations** ✅

---

## 8. Final Handoff

- **FP-033 is CLOSED**
- **FP-001 → FP-033 CLOSED** (33 features)
- **First real FFmpeg execution achieved** 🎉
- Next feature may start from **FP-034**
