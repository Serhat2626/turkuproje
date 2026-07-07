# FP-030 Task 01 — Real FFmpeg Process Boundary (LOCAL/DEV/TEST)

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-030 Task 01 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |

---

## Real Process Boundary Design

### Allowed ProcessBuilder Location

**ONE class only**: `TurkuFfmpegLocalDevProcessRunner`

This is the only production class that may use `ProcessBuilder`. All other classes are verified clean via reflective test.

### Gate Conditions (before process creation)

| Condition | Required |
|-----------|:--------:|
| FP-028 gate evaluate → not DENIED | ✅ |
| Mode = REAL_DEV_TEST_ONLY | ✅ |
| Environment ∈ {LOCAL, DEV, TEST} | ✅ |
| processEnabled = true | ✅ |
| Binary policy available | ✅ |

If ANY condition fails → process is NEVER created.

### Denied Environments

| Environment | Result |
|-------------|:------:|
| STAGING | GATE_DENIED |
| PROD | GATE_DENIED |
| UNKNOWN | GATE_DENIED |

---

## Added Backend Classes (5)

| Class | Kind |
|-------|------|
| TurkuFfmpegProcessStatus | enum (9 states) |
| TurkuFfmpegProcessReasonCode | enum (16 codes) |
| TurkuFfmpegProcessRequest | safe request (no command/path) |
| TurkuFfmpegProcessResult | safe result (no stdout/stderr) |
| TurkuFfmpegLocalDevProcessRunner | THE ONE ProcessBuilder class |

---

## Test Strategy

- `processEnabled=false` by default — CI never launches FFmpeg
- All tests use fake/skipped execution path
- No machine FFmpeg availability required
- Real execution only via explicit `processEnabled=true` + LOCAL/DEV/TEST gate

---

## Safety Scan

| Check | Result |
|-------|:------:|
| ProcessBuilder only in LocalDevProcessRunner | ✅ |
| Runtime.exec absent everywhere | ✅ |
| stdout/stderr consumed but NOT exposed in result | ✅ |
| No command/path/log fields in DTOs | ✅ |
| No publicUrl/storageKey/base64/binary | ✅ |
| publicDeliveryAvailable = false | ✅ |
| autoApproved = false | ✅ |
| 5 classes × 19 forbidden fields = 0 violations | ✅ |

---

## Next: Task 02

First real internal smoke run with `processEnabled=true` on local machine (optional), regression, final closure.
