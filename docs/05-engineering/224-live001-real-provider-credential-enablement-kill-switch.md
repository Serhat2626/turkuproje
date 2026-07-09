# LIVE-001 — Real Provider Credential Enablement + Manual Kill Switch

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | LIVE-001: Provider Credential Enablement + Kill Switch |
| **Phase** | LIVE — Controlled Real Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4827 (was 4815, +12) |
| **Frontend Tests** | 2655 (was 2639, +16) |
| **Combined** | 7482 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. LIVE Phase Purpose

LIVE phase enables controlled real execution capabilities one boundary at a time, with explicit kill switches, operator gates, and disabled-by-default policies. Each LIVE task adds one real capability boundary without enabling execution until all safety prerequisites are met.

---

## 3. LIVE-001 Goal

Enable real image provider credential/config boundary safely with explicit manual kill switch and disabled-by-default execution. No real provider call yet.

---

## 4. DTO/Model/Service Boundaries

### Enums
| Enum | Values |
|------|--------|
| TurkuLiveProviderStatus | 5: DISABLED_BY_DEFAULT, CREDENTIAL_PRESENT, BLOCKED_BY_KILL_SWITCH, WAITING_FOR_OPERATOR_ENABLEMENT, FAILED_SAFE |
| TurkuLiveProviderIssueCode | 5: CREDENTIAL_NOT_PRESENT, KILL_SWITCH_ACTIVE, OPERATOR_ENABLEMENT_REQUIRED, PREREQUISITE_NOT_MET, INTERNAL_ERROR |

### Models
| Model | Purpose |
|-------|---------|
| TurkuKillSwitch | Global execution blocker — active/reason/activatedBy |
| TurkuLiveProviderReadinessReport | Provider status + credential presence + kill switch + prerequisites |
| TurkuLiveProviderReadinessService | Evaluates credential presence + operator enablement + kill switch |

---

## 5. Kill Switch Behavior

- **Default: ACTIVE** — blocks all real provider execution
- When active: status = BLOCKED_BY_KILL_SWITCH regardless of other flags
- When inactive + credential present + operator enabled: status = CREDENTIAL_PRESENT
- Kill switch overrides all other conditions

---

## 6. Disabled-by-Default Policy

- No credential → DISABLED_BY_DEFAULT
- Credential present but operator not enabled → WAITING_FOR_OPERATOR_ENABLEMENT
- All three flags required for CREDENTIAL_PRESENT status:
  1. Kill switch inactive
  2. Credential present
  3. Operator enabled

---

## 7. Config/Env Changes

**None in LIVE-001.** Credential wiring will be added in future LIVE task when real provider call is enabled.

---

## 8. Admin UI Behavior

- Provider status with safe labels
- Credential presence status only (value hidden)
- Kill switch state with active/inactive indicator
- Operator enablement status
- Prerequisites list
- Blocked reasons
- Validation summary
- No provider run, image generation, render, storage, download, publish buttons

---

## 9. Changed Files

### Backend (5 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuLiveProviderStatus.java | 5 statuses |
| TurkuLiveProviderIssueCode.java | 5 issue codes |
| TurkuKillSwitch.java | Kill switch with default-active |
| TurkuLiveProviderReadinessReport.java | Readiness report DTO |
| TurkuLiveProviderReadinessService.java | Credential + operator + kill switch evaluation |
| TurkuLiveProviderReadinessTest.java | 12 tests in 8 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +35 lines LIVE-001 types |
| LiveProviderReadinessPreview.tsx | Provider status, kill switch, credential |
| LiveProviderReadinessPreview.test.ts | 16 tests in 6 groups |

---

## 10. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4815 | 4827 | +12 |
| Frontend | 2639 | 2655 | +16 |
| Combined | 7454 | 7482 | +28 |

---

## 11. Recommended LIVE-002

LIVE-002: Real Provider API Call Boundary + Controlled Single-Scene Test
- First real provider API call (single scene, operator-gated)
- Kill switch integration
- Safe result metadata capture
- No batch/bulk execution

---

## 12. LIVE-001 Final Status

**LIVE-001: CLOSED**
