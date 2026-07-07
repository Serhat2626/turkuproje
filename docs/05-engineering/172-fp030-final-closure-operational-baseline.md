# FP-030 Final Closure — Operational Baseline

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | FP-030: Real FFmpeg Process Boundary + Local/Dev/Test Smoke Run |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 3986 passed, 0 failures |
| **Frontend Tests** | 2105 passed, 0 failures |
| **Endpoints** | **44 = 25 POST + 19 GET** |
| **Migration/schema/config** | **None** |

---

## 2. Full FP-030 Task Chain

| Task | Commit | Scope | Test Δ |
|------|--------|-------|:------:|
| 01 | 076002c | Process boundary — ONE ProcessBuilder class, gate, request/result | +21 backend |
| 02 | *(this)* | Smoke run service + admin visibility + closure | backend + frontend |

---

## 3. What Actually Happened

**Execution skipped safely (processEnabled=false by default).**

- The `TurkuFfmpegSmokeRunService` validates full FP-025/026/028/030 chain
- With `processEnabled=false` (default): returns `EXECUTION_SKIPPED_DISABLED`
- With `DISABLED_BINARY`: returns `EXECUTION_SKIPPED_UNAVAILABLE`
- With STAGING/PROD/UNKNOWN: returns `GATE_DENIED` (before process creation)
- Real ProcessBuilder code EXISTS in `TurkuFfmpegLocalDevProcessRunner.executeProcess()` but is unreachable unless `processEnabled=true` AND LOCAL/DEV/TEST AND REAL_DEV_TEST_ONLY
- CI passes without FFmpeg installed — all tests use fake/disabled runner
- No real FFmpeg process was executed in CI

---

## 4. Production Inventory

### Backend Classes — FP-030 (8 new total)

| Class | Kind | Task |
|-------|------|:----:|
| TurkuFfmpegProcessStatus | enum (9 states) | 01 |
| TurkuFfmpegProcessReasonCode | enum (16 codes) | 01 |
| TurkuFfmpegProcessRequest | safe request | 01 |
| TurkuFfmpegProcessResult | safe result | 01 |
| TurkuFfmpegLocalDevProcessRunner | THE ONE ProcessBuilder class | 01 |
| TurkuFfmpegSmokeRunStatus | enum (12 states) | 02 |
| TurkuFfmpegSmokeRunResult | smoke run result | 02 |
| TurkuFfmpegSmokeRunService | smoke run service | 02 |

### Frontend Types — FP-030 (2 new)
TurkuFfmpegSmokeRunStatus, TurkuFfmpegSmokeRunResult

### Admin UI — FP-030 (1 panel + 5 labels)
- Smoke run panel with status/process/candidate
- Labels: LOCAL/DEV/TEST only, disabled default, no publish, no player, no command

---

## 5. ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (allowed) | ❌ |
| TurkuFfmpegSmokeRunService | ❌ | ❌ |
| TurkuFfmpegSmokeRunResult | ❌ | ❌ |
| All other production classes | ❌ | ❌ |

**Runtime.exec is absent in the entire codebase.**

---

## 6. Endpoint Inventory

| Controller | POST | GET | Total |
|------------|:----:|:---:|:-----:|
| TurkuVideoProjectController | 14 | 5 | 19 |
| AdminPipelineController | 7 | 6 | 13 |
| AdminReviewController | 2 | 5 | 7 |
| AdminWorkflowObservabilityController | 2 | 3 | 5 |
| **Total** | **25** | **19** | **44** |

No new endpoints in FP-030.

---

## 7. Safety Guarantees

| Guarantee | Status |
|-----------|:------:|
| Default disabled (processEnabled=false) | ✅ |
| STAGING/PROD/UNKNOWN denied before process | ✅ |
| LOCAL/DEV/TEST only with explicit flags | ✅ |
| ProcessBuilder only in LocalDevProcessRunner | ✅ |
| Runtime.exec absent | ✅ |
| stdout/stderr consumed but NOT exposed | ✅ |
| No command/path/log in DTOs | ✅ |
| No public/signed/CDN URL | ✅ |
| No storage key/binary/base64 | ✅ |
| No publish/export/download/player | ✅ |
| publicDeliveryAvailable = false (always) | ✅ |
| autoApproved = false (always) | ✅ |

---

## 8. Forbidden Exposure Scan

8 backend classes + 2 frontend types + 1 UI panel scanned:
- filePath, absolutePath, inputPath, outputPath ❌
- storageKey, publicUrl, signedUrl, cdnUrl ❌
- commandLine, shellCommand, executablePath ❌
- stdout, stderr, rawLog ❌
- base64, binaryData, rawPayload ❌
- secret, apiKey, token, session, principal ❌
- downloadUrl, publishUrl, exportUrl ❌
- dangerouslySetInnerHTML, innerHTML ❌

**Result: 0 violations** ✅

---

## 9. Final Handoff

- **FP-030 is CLOSED**
- **FP-001 → FP-030 CLOSED** (30 features)
- Next feature may start from **FP-031**
