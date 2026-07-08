# RPE-005 — Safe Image Prompt Package + Manual Approval Gate

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RPE-005: Image Prompt Package + Manual Approval |
| **Phase** | RPE — Real Production Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4775 (was 4762, +13) |
| **Frontend Tests** | 2588 (was 2572, +16) |
| **Combined** | 7363 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Create safe image prompt package boundary for future real image generation with manual approval gate. No real provider call, no image generation.

---

## 3. DTO/Model/Service Boundaries

### Enums
| Enum | Values |
|------|--------|
| TurkuImagePromptPackageStatus | 5: DRAFT, PENDING_MANUAL_APPROVAL, BLOCKED_BY_SAFETY, WAITING_FOR_OPERATOR_REVIEW, FAILED_SAFE |
| TurkuImagePromptPackageIssueCode | 6: EXECUTION_PACKAGE_NOT_READY, SCENE_VISUAL_BRIEF_MISSING, SAFETY_CONSTRAINT_VIOLATED, PROMPT_CONTENT_BLOCKED, OPERATOR_REVIEW_PENDING, INTERNAL_ERROR |

### Models
| Model | Purpose |
|-------|---------|
| TurkuSceneImagePrompt | Per-scene safe prompt summary + constraints |
| TurkuImagePromptPackageResult | Package with scene prompts + approval gate |
| TurkuImagePromptPackageService | Execution package → prompt package mapping |

---

## 4. Prompt Package Rules

- Safe summarized prompt text only
- No lyrics, full script, copyrighted text
- No provider raw payload, base64, binary
- No credentials, storage key/URL, path, command
- 5 safety constraints per scene
- Manual approval gate metadata only — no auto-approval

---

## 5. Manual Approval Gate Model

- `manualApprovalGranted` always `false` in RPE-005
- `approvalGateLabel` describes pending operator review
- No approval action or auto-approval logic

---

## 6. Changed Files

### Backend (5 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuImagePromptPackageStatus.java | 5 statuses |
| TurkuImagePromptPackageIssueCode.java | 6 issue codes |
| TurkuSceneImagePrompt.java | Per-scene safe prompt |
| TurkuImagePromptPackageResult.java | Package result DTO |
| TurkuImagePromptPackageService.java | Exec pkg → prompt mapping |
| TurkuImagePromptPackageTest.java | 13 tests in 8 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +42 lines RPE-005 types |
| ImagePromptPackagePreview.tsx | Prompt status, scenes, constraints |
| ImagePromptPackagePreview.test.ts | 16 tests in 7 groups |

---

## 7. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4762 | 4775 | +13 |
| Frontend | 2572 | 2588 | +16 |
| Combined | 7334 | 7363 | +29 |

---

## 8. Recommended RPE-006

RPE-006: RPE Phase Safety Closure + Future Execution Readiness Baseline
- Full RPE chain regression tests
- Safety sweep across all RPE classes
- Phase closure and readiness documentation

---

## 9. RPE-005 Final Status

**RPE-005: CLOSED**
