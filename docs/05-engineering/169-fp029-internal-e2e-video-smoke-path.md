# FP-029 Task 01 — Internal E2E Video Smoke Path

## Status

| Field | Value |
|-------|-------|
| **Task** | FP-029 Task 01 |
| **Status** | ✅ COMPLETED |
| **Endpoints** | 0 added (unchanged) |
| **Endpoint Inventory** | 44 = 25 POST + 19 GET |
| **Migration/schema/config** | None |

---

## Added Backend Classes (6)

| Class | Kind |
|-------|------|
| TurkuInternalSmokePathStatus | enum (10 states) |
| TurkuInternalSmokePathReasonCode | enum (14 codes) |
| TurkuInternalSmokeRequest | opaque refs + bounded counts |
| TurkuInternalSmokeRenderPlan | metadata-only plan (mp4/webm) |
| TurkuInternalSmokePathResult | result with status/reason/output/plan |
| TurkuInternalSmokeOrchestrator | FP-025/026/028 chain validation |

## Added Frontend Types (4)

TurkuInternalSmokePathStatus, SmokePathReasonCode, SmokeRenderPlan, SmokePathResult

## Added UI Panels (3 + 6 labels)

| Panel | Content |
|-------|---------|
| Smoke Path Durumu | Status/reason/readiness/review/gate |
| Render Plan Özeti | Format/output ref/verification |
| Safety | 6 Turkish labels |

---

## Orchestration Chain

1. FP-025 readiness → READY_INTERNAL_ONLY required
2. FP-025 safety chain → all safe
3. FP-026 review → APPROVED_FOR_INTERNAL_NEXT_STEP required
4. FP-028 FFmpeg gate → LOCAL/DEV/TEST allowed
5. Plan creation → mp4/webm, bounded
6. Candidate creation → opaque output ref

---

## Safety

- No ProcessBuilder/execution in orchestrator
- No stdout/stderr/rawLog/commandLine/path
- Bounds: duration ≤600s, fps ≤60, resolution ≤3840×2160, size ≤1000MB
- Formats: mp4/webm only
- publicDeliveryAvailable = false (always)
- autoApproved = false (always)
- 6 classes × 24 forbidden fields = 0 violations
