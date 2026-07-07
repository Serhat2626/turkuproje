# FP-031 Task 01 — Local-only FFmpeg Manual Execution Enablement

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-031 Task 01 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |

---

## Current Truth (from FP-030)

- ProcessBuilder boundary EXISTS in `TurkuFfmpegLocalDevProcessRunner`
- No real process was executed — processEnabled=false by default
- ProcessBuilder allowed ONLY in that one class
- Runtime.exec remains forbidden

---

## Local-only Manual Enablement Policy

### Required gates (ALL must be true)

| Gate | Condition |
|------|-----------|
| Policy | LOCAL_ONLY_MANUAL |
| Manual confirmation | manualExecutionConfirmed = true |
| Process enabled | processEnabled = true |
| Environment | LOCAL only (DEV/TEST/STAGING/PROD/UNKNOWN denied) |
| Binary | Not DISABLED_BINARY |
| FP-025 readiness | READY_INTERNAL_ONLY |
| FP-025 safety chain | All 4 checks safe |
| FP-026 review | APPROVED_FOR_INTERNAL_NEXT_STEP |
| FP-030 runner gate | Not denied |

If ANY gate fails → execution NEVER starts.

---

## Added Backend Classes (7)

| Class | Kind |
|-------|------|
| TurkuFfmpegManualExecutionPolicy | enum (2 values) |
| TurkuFfmpegManualExecutionStatus | enum (15 states) |
| TurkuFfmpegManualExecutionReasonCode | enum (16 codes) |
| TurkuFfmpegManualExecutionRequest | safe request (no command/path) |
| TurkuFfmpegManualExecutionResult | safe result (no stdout/stderr) |
| TurkuFfmpegFixtureMetadata | metadata-only verification |
| TurkuFfmpegManualExecutionService | full chain service |

---

## Fake-run CI Strategy

- All tests use default runner (processEnabled=false)
- CI passes without FFmpeg installed
- No real process ever executed in automated tests
- `EXECUTION_SKIPPED_FAKE` is the expected CI status

---

## Optional Manual Local Execution

Developer can enable real LOCAL execution by:
1. Creating `TurkuFfmpegExecutionGate` with `REAL_DEV_TEST_ONLY`, `LOCAL`, `true`
2. Creating `TurkuFfmpegLocalDevProcessRunner(gate, true)` — processEnabled=true
3. Creating request with `LOCAL_ONLY_MANUAL`, `manualExecutionConfirmed=true`, `processEnabled=true`
4. Providing all readiness/review/safety prerequisites

This is intentionally manual and explicit. Production, staging, and CI never reach this path.

---

## Fixture Metadata Verification

| Bound | Limit |
|-------|:-----:|
| Formats | mp4, webm only |
| Duration | ≤600s |
| FPS | ≤60 |
| Resolution | ≤3840×2160 |
| Size | ≤1000 MB |
| Data | Metadata only, opaque output ref |

---

## ProcessBuilder Inventory

| Class | ProcessBuilder | Runtime.exec |
|-------|:--------------:|:------------:|
| TurkuFfmpegLocalDevProcessRunner | ✅ (allowed) | ❌ |
| TurkuFfmpegManualExecutionService | ❌ | ❌ |
| All other classes | ❌ | ❌ |

---

## Safety Scan

7 classes × 19 forbidden fields = **0 violations** ✅

---

## Next: Task 02

Close FP-031 with regression, admin visibility if needed, and closure doc.
