# 40 — FP-002 Final Closure & Operational Baseline

> **Feature FP-002: Scenario Generation Workflow**
>
> Bu doküman FP-002 feature'ının tüm implementation, test ve staging
> validation süreçlerini kapatır. Mevcut staging operational baseline'ı,
> güvenlik garantilerini, bilinen follow-up'ları ve önerilen sonraki
> sprint'i belgelemektedir.

---

## 1. Status

**FP-002 — COMPLETED**

FP-002 "Scenario Generation Workflow" feature'ı 13 task boyunca
product design'dan staging execution'a kadar tamamlanmıştır.

Staging final decision: **PASSED**

Deploy commit: `75dca42`

---

## 2. Scope Closed

FP-002 aşağıdaki kapsamı teslim etmiştir:

| # | Deliverable | Task | Durum |
|---|------------|------|-------|
| 1 | Product/workflow design | Task 01 | ✅ |
| 2 | AI platform contract design | Task 02 | ✅ |
| 3 | Engineering design & task breakdown | Task 03 | ✅ |
| 4 | ScenarioDraft / ScenarioBeat domain model & migration | Task 04 | ✅ |
| 5 | Scenario generation service with mock gateway | Task 05 | ✅ |
| 6 | Scenario gateway config safety (mock fallback) | Task 05 Fix | ✅ |
| 7 | Scenario review service (approve/reject/edit) | Task 06 | ✅ |
| 8 | Admin REST endpoints (6 operations) | Task 07 | ✅ |
| 9 | Admin UI scenario workflow | Task 08 | ✅ |
| 10 | OpenAI scenario provider | Task 09 | ✅ |
| 11 | Local smoke test validation | Task 10 | ✅ |
| 12 | Staging execution plan | Task 11 | ✅ |
| 13 | Staging execution report | Task 12 | ✅ |

---

## 3. Operational Baseline

### Runtime Environment

| Item | Value |
|------|-------|
| Stack name | `anadolu-atlasi-staging` |
| Provider | OpenAI |
| Model | `gpt-4o-mini` |
| Runtime provider variable | `E_AI_PROVIDER=openai` |
| Deploy method | `dev` push → auto build + auto deploy |
| Deploy commit | `75dca42` |

### Provider State

| Gateway | Status |
|---------|--------|
| `OpenAiScenarioAgentGateway` (FP-002) | ✅ Active — `ai.provider=openai` |
| `OpenAiContentAgentGateway` (FP-001) | ✅ Active — same `ai.provider=openai` |
| `MockAiScenarioAgentGateway` | Available as fallback via `@ConditionalOnMissingBean` |
| `MockAiContentAgentGateway` | Available as fallback via `@ConditionalOnMissingBean` |

### Deployment State

- `dev` push triggers auto build + auto deploy (accepted by operator)
- No rollback was required during Task 12 staging execution
- No OPS cleanup or stack rename was performed (deferred)

No secret values documented.

---

## 4. Domain/API Baseline

### Entities

| Entity | Table | Description |
|--------|-------|-------------|
| `ScenarioDraft` | `scenario_drafts` | Versiyonlanmış senaryo taslağı. ContentProject ile ilişkili |
| `ScenarioBeat` | `scenario_beats` | Senaryo bölümleri. ScenarioDraft ile parent-child ilişkisi |

### ScenarioDraft Statuses

| Status | Description |
|--------|-------------|
| `AI_GENERATED` | AI tarafından üretildi, insan onayı bekliyor |
| `NEEDS_REVIEW` | Düzenleme yapıldı, tekrar inceleme gerekiyor |
| `APPROVED` | İnsan editör tarafından onaylandı |
| `REJECTED` | İnsan editör tarafından reddedildi |
| `FAILED` | AI üretimi başarısız oldu |

### ScenarioBeat Types

| Order | Type | Description |
|-------|------|-------------|
| 1 | `HOOK` | Dikkat çekici açılış |
| 2 | `INTRODUCTION` | Konu tanıtımı |
| 3 | `MAIN_NARRATIVE` | Ana anlatı |
| 4 | `EMOTIONAL_CLIMAX` | Duygusal doruk |
| 5 | `CONCLUSION` | Kapanış ve değerlendirme |
| 6 | `CALL_TO_ACTION` | İzleyici eylemi çağrısı |

### Endpoints

| # | Operation | Method | Path |
|---|-----------|--------|------|
| 1 | Generate | POST | `/api/admin/content-projects/{projectId}/scenarios/generate` |
| 2 | List | GET | `/api/admin/content-projects/{projectId}/scenarios` |
| 3 | Detail | GET | `/api/admin/scenarios/{scenarioId}` |
| 4 | Edit | PUT | `/api/admin/scenarios/{scenarioId}` |
| 5 | Approve | POST | `/api/admin/scenarios/{scenarioId}/approve` |
| 6 | Reject | POST | `/api/admin/scenarios/{scenarioId}/reject` |

**Note:** Swagger may show 5 path entries because `GET` and `PUT` on `/api/admin/scenarios/{scenarioId}` share a single path. There are 6 distinct operations.

---

## 5. Admin UI Baseline

Scenario workflow section exists in Content Project detail page:

| Feature | Description | Status |
|---------|-------------|--------|
| Generate scenario | Admin can trigger scenario generation from approved ContentDraft | ✅ |
| List versions | Admin can see all scenario versions for a project | ✅ |
| Open detail | Admin can view full scenario with 6 beats | ✅ |
| Edit 6 beats | Admin can edit individual beat bodies and scenario metadata | ✅ |
| Explicit approve | Admin can approve a scenario via dedicated button | ✅ |
| Explicit reject | Admin can reject a scenario with reason | ✅ |
| Read-only states | APPROVED, REJECTED, FAILED scenarios are read-only | ✅ |
| Status badges | Visual status indicators for each scenario version | ✅ |

---

## 6. Safety and Governance Baseline

These rules are established by FP-002 and must be preserved in all future features:

### AI Output Rules

| # | Rule | Enforcement |
|---|------|-------------|
| 1 | AI output must never become `APPROVED` automatically | `ScenarioGenerationService` creates with `AI_GENERATED` status |
| 2 | AI output must never become `READY_FOR_PUBLISH` automatically | No such transition exists in scenario domain |
| 3 | Scenario generation requires approved, human-reviewed ContentDraft | `findApprovedDraft()` checks `ContentDraft.status == APPROVED` |

### Human Review Rules

| # | Rule | Enforcement |
|---|------|-------------|
| 4 | Approve requires explicit admin action | `POST /approve` with `ScenarioReviewService.approve()` |
| 5 | Reject requires explicit admin action | `POST /reject` with `ScenarioReviewService.reject()` |
| 6 | Save/edit must not approve | PUT sets status to `NEEDS_REVIEW`, never `APPROVED` |
| 7 | No generic status update endpoint | No `PATCH /status` or `PUT /status` exists |
| 8 | No generic status dropdown | UI uses dedicated approve/reject buttons only |

### Data Safety Rules

| # | Rule | Enforcement |
|---|------|-------------|
| 9 | Internal AI contract DTOs not exposed through REST | Separate DTO layer for API responses |
| 10 | Raw provider payload not stored or exposed | Not persisted in DB, not in REST response |
| 11 | Raw prompt not stored or exposed | Not persisted in DB, not in REST response |
| 12 | API keys/secrets not committed or documented | CI variables only, masked in GitLab |

### Architecture Rules

| # | Rule | Enforcement |
|---|------|-------------|
| 13 | Provider-specific code behind gateway boundary | `ai.gateway.openai` package isolation |
| 14 | Mock provider available as fallback | `@ConditionalOnMissingBean` in `AiGatewayConfig` |
| 15 | Controllers stay thin | Controllers delegate to service layer |
| 16 | Business rules in service/application layer | `ScenarioGenerationService`, `ScenarioReviewService` |

---

## 7. Validation Baseline

Final validation results from Task 12 staging execution:

### Automated Tests

| Check | Result |
|-------|--------|
| Backend unit tests | ✅ 295/295 pass, 0 failures |
| Frontend TypeScript compile | ✅ Clean, no errors |
| Frontend Vite build | ✅ Passing |

### Staging Smoke Tests

| Check | Result |
|-------|--------|
| Mock scenario smoke | ✅ Passed (full workflow: generate → edit → approve → reject) |
| Real OpenAI scenario smoke | ✅ Passed (providerType=OPENAI, modelName=gpt-4o-mini, 6 beats) |
| Migration verification | ✅ Passed (tables exist, no sensitive columns) |
| Backend health | ✅ Passed (DB, Redis, SSL all UP) |
| Admin UI health | ⚠️ Not accessible externally (firewall) — API-level verification completed |
| FP-001 regression | ✅ Passed (endpoints, approve flow, governance) |
| Governance checks | ✅ 12/12 passed |
| Rollback | Not needed |

---

## 8. Important Fixes During FP-002

### Fix 1: Scenario Gateway Config Safety (Task 05 Fix)

| Item | Detail |
|------|--------|
| Problem | Missing `AiScenarioAgentGateway` bean could cause context startup failure |
| Solution | Mock scenario gateway registered via `@ConditionalOnMissingBean` as fallback |
| File | `AiGatewayConfig.java` |
| Impact | Backend starts safely regardless of `ai.provider` value |
| Governance | No governance weakening — mock only activates when real provider is absent |

### Fix 2: ContentDraft Approve Gap

| Item | Detail |
|------|--------|
| Problem | `ContentProjectService.approve()` only set `ContentProject.status = APPROVED` but did not update `ContentDraft.status` |
| Impact | FP-002 scenario generation requires `ContentDraft.status == APPROVED` as prerequisite — all generate calls returned 409 |
| Solution | `approve()` now also sets latest ContentDraft to APPROVED within the same `@Transactional` method |
| File | `ContentProjectService.java` |
| Commit | `a4d37fc` |
| Governance preserved | AI still cannot auto-approve. Approve remains explicit admin action. No generic status endpoint introduced. READY_FOR_PUBLISH remains separate explicit action |
| Tests | 295/295 pass after fix |

### Fix 3: CI/Deploy Alignment

| Item | Detail |
|------|--------|
| CI variable fix | `DEPLOY_SERVER` → `DEPLOY_SERVER_NEW` (commit `9e58d10`) |
| Auto-deploy | Removed `when: manual` from deploy job — `dev` push triggers auto build + deploy (commit `78f67f3`) |
| Scope | Staging only. No production pipeline exists yet |
| Operator acceptance | Operator confirmed and accepted auto-deploy on `dev` |
| Runtime variable | `E_AI_PROVIDER=openai` set by operator in GitLab CI/CD Variables |
| Secrets | No secrets printed, committed, or documented |

---

## 9. Known Follow-ups

Non-blocking items deferred from FP-002:

| # | Item | Priority | Notes |
|---|------|----------|-------|
| 1 | OPS-001: Stack naming cleanup | Low | `anadolu-atlasi-staging` remains — rename deferred |
| 2 | Separate provider config | Low | Consider `content.provider` vs `scenario.provider` if different models needed |
| 3 | Admin UI external access | Low | Firewall port for frontend — not blocking API validation |
| 4 | FP-003: Storyboard Workflow | Next sprint | Scene-based visual breakdown |
| 5 | Future: Media generation | Future | Image/video generation from storyboard |
| 6 | Future: Cost dashboard | Future | AI usage analytics and prompt management UI |

No implementation tasks created here.

---

## 10. FP-003 Readiness

**FP-002 is closed.**

The project is ready to start the next feature sprint.

**Recommended next feature: FP-003 — Storyboard Workflow**

FP-003 should follow the same process:

1. Start with product/workflow design (Task 01)
2. Define AI platform contract (Task 02)
3. Create engineering design & task breakdown (Task 03)
4. Proceed to implementation only after design approval

FP-003 must not jump directly into implementation.

---

## 11. Acceptance Confirmation

| Criteria | Status |
|----------|--------|
| FP-002 final closure document created | ✅ |
| FP-002 clearly marked completed | ✅ |
| Operational baseline documented | ✅ |
| Domain/API baseline documented | ✅ |
| Admin UI baseline documented | ✅ |
| Safety/governance rules preserved (16 rules) | ✅ |
| Validation baseline documented | ✅ |
| Important fixes recorded (3 fixes) | ✅ |
| Known follow-ups are non-blocking | ✅ |
| FP-003 readiness stated | ✅ |
| No code/config/deployment changes made | ✅ |
| No secrets/raw prompt/raw provider payload documented | ✅ |
