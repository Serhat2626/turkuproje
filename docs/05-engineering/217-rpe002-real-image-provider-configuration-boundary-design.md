# RPE-002 — Real Image Provider Configuration Boundary Design

## 1. Status

| Field | Value |
|-------|-------|
| **Feature** | RPE-002: Image Provider Config Boundary |
| **Phase** | RPE — Real Production Execution Phase |
| **Status** | ✅ **CLOSED** |
| **Backend Tests** | 4730 (was 4714, +16) |
| **Frontend Tests** | 2532 (was 2511, +21) |
| **Combined** | 7262 |
| **Endpoints** | **46 = 26 POST + 20 GET** (unchanged) |
| **Migration/schema/config** | **None** |

---

## 2. Goal

Design the real image provider configuration boundary for future production execution, keeping all provider execution disabled and non-executable.

---

## 3. DTO/Model/Service Boundaries

### Enums
| Enum | Values | Purpose |
|------|--------|---------|
| TurkuImageProviderType | 3 | PLACEHOLDER_LOCAL, DETERMINISTIC_FAKE, FUTURE_EXTERNAL_API |
| TurkuProviderConfigStatus | 5 | NOT_CONFIGURED, CONFIGURATION_DRAFT, BLOCKED_BY_SAFETY, WAITING_FOR_OPERATOR_REVIEW, FAILED_SAFE |
| TurkuProviderConfigIssueCode | 6 | CREDENTIAL_NOT_PRESENT, PROVIDER_TYPE_NOT_SELECTED, FALLBACK_NOT_CONFIGURED, SAFETY_VALIDATION_FAILED, OPERATOR_REVIEW_PENDING, INTERNAL_ERROR |

### Models
| Model | Purpose |
|-------|---------|
| TurkuCredentialVaultBoundary | Vault label + presence status only |
| TurkuProviderConfigResult | Config status + capabilities + vault + policies |
| TurkuProviderConfigService | Gate → config boundary mapping |

---

## 4. Credential Vault Boundary

- Only presence status exposed (true/false)
- Never raw credential, token, secret, API key, password, env value
- Vault label is safe Turkish text

---

## 5. Provider Selection/Fallback Policy

- Selection policy: metadata label only
- Fallback policy: metadata label only
- Default: "henüz yapılandırılmadı"

---

## 6. Disabled-By-Default Execution Policy

- `executionEnabled` always `false` in RPE-002
- All 4 capability labels are "(gelecek)"
- Safety validation summary mentions "devre dışı"

---

## 7. Admin UI Behavior

- Provider label display
- Capability labels display
- Credential presence status (✅/❌)
- Vault boundary label
- Selection/fallback summary
- Disabled execution status
- Safety validation summary
- No credential save, test connection, provider execution buttons

---

## 8. Changed Files

### Backend (6 prod + 1 test)
| File | Purpose |
|------|---------|
| TurkuImageProviderType.java | 3 provider types |
| TurkuProviderConfigStatus.java | 5 statuses |
| TurkuProviderConfigIssueCode.java | 6 issue codes |
| TurkuCredentialVaultBoundary.java | Vault presence placeholder |
| TurkuProviderConfigResult.java | Config result DTO |
| TurkuProviderConfigService.java | Gate → config mapping |
| TurkuProviderConfigTest.java | 16 tests in 9 groups |

### Frontend (3 files)
| File | Purpose |
|------|---------|
| types/index.ts | +42 lines RPE-002 types |
| ProviderConfigPreview.tsx | Config status, vault, policies |
| ProviderConfigPreview.test.ts | 21 tests in 8 groups |

---

## 9. Test Baseline

| Suite | Before | After | Delta |
|-------|--------|-------|-------|
| Backend | 4714 | 4730 | +16 |
| Frontend | 2511 | 2532 | +21 |
| Combined | 7225 | 7262 | +37 |

---

## 10. Recommended RPE-003

RPE-003: Render Pipeline Configuration Boundary Design
- Safe render pipeline configuration model
- Render environment boundary placeholder
- FFmpeg capability metadata (not execution)
- Output format/resolution policy metadata
- All still disabled/non-executable

---

## 11. RPE-002 Final Status

**RPE-002: CLOSED**
