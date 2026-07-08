# RPE-006 — Manual-Gated Image Generation Execution Boundary

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RPE-006: Image Generation Execution Boundary |
| **Phase** | RPE — Real Production Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4789 (was 4775, +14) |
| **Frontend Tests** | 2607 (was 2588, +19) |
| **Combined** | 7396 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Create first controlled image generation execution boundary. CI/default = fake/test-double. Real provider disabled by default, modeled as local/manual-only placeholder.

---

## 3. DTO/Model/Service Boundaries

### Enums
| Enum | Values |
|------|--------|
| TurkuImageGenerationExecutionMode | 3: DISABLED, CI_FAKE, LOCAL_MANUAL_ONLY |
| TurkuImageGenerationStatus | 5: NOT_STARTED, FAKE_RESULT_AVAILABLE, BLOCKED_BY_SAFETY, WAITING_FOR_MANUAL_GATE, FAILED_SAFE |
| TurkuImageGenerationIssueCode | 6: PROMPT_PACKAGE_NOT_APPROVED, MANUAL_GATE_NOT_PASSED, PROVIDER_EXECUTION_DISABLED, SAFETY_CONSTRAINT_VIOLATED, OPERATOR_REVIEW_PENDING, INTERNAL_ERROR |

### Models
| Model | Purpose |
|-------|---------|
| TurkuFakeImageResultMetadata | Opaque ref + label + format/resolution hint |
| TurkuImageGenerationExecutionResult | Status + mode + fake results + validation |
| TurkuImageGenerationExecutionService | Prompt pkg → CI_FAKE execution |

---

## 4. CI Fake vs Local/Manual-Only Boundary

- **CI_FAKE**: Default mode. Deterministic fake results. No provider call.
- **LOCAL_MANUAL_ONLY**: Future placeholder. Requires explicit operator gate.
- **DISABLED**: No execution at all.

---

## 5. Manual Gate Behavior

- `manualGatePassed` always `false` in RPE-006
- Real provider requires future manual gate + operator approval
- No auto-approval

---

## 6. Changed Files

### Backend (6 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuImageGenerationExecutionMode.java | 3 modes |
| TurkuImageGenerationStatus.java | 5 statuses |
| TurkuImageGenerationIssueCode.java | 6 issue codes |
| TurkuFakeImageResultMetadata.java | Fake result metadata |
| TurkuImageGenerationExecutionResult.java | Execution result DTO |
| TurkuImageGenerationExecutionService.java | Prompt → fake execution |
| TurkuImageGenerationExecutionTest.java | 14 tests in 8 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +46 lines RPE-006 types |
| ImageGenerationExecutionPreview.tsx | Status, mode, fake results |
| ImageGenerationExecutionPreview.test.ts | 19 tests in 7 groups |

---

## 7. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4775 | 4789 | +14 |
| Frontend | 2588 | 2607 | +19 |
| Combined | 7363 | 7396 | +33 |

---

## 8. Recommended RPE-007

RPE-007: RPE Phase Safety Closure + Full Chain Regression
- Full RPE chain regression (RPE-001 → RPE-006)
- Safety sweep across all RPE classes
- Phase closure documentation and readiness baseline

---

## 9. RPE-006 Final Status

**RPE-006: CLOSED**
