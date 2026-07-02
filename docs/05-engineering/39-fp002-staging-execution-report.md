# FP-002 Staging Execution Report

## 1. Status

**READY FOR OPERATOR DEPLOYMENT**

Pre-deployment checks completed. Deployment requires operator to trigger the GitLab CI/CD pipeline manually (`deploy-staging` stage with `when: manual`).

Local machine does not have SSH access to the staging server. All runtime validations below are documented as **operator checklists** to execute after deployment.

---

## 2. Environment

| Item | Value |
|------|-------|
| Stack Name | `anadolu-atlasi-staging` |
| Git Branch | `dev` |
| Commit Hash | `d2b5451f1e5ef3b7eaa2d5711c983f94bdbec9a9` |
| Backend Image | `registry.gitlab.com/solostartup/turkuproje/backend:dev` |
| Frontend Image | `registry.gitlab.com/solostartup/turkuproje/admin-web:dev` |
| AI Provider | `openai` (from CI variable `E_AI_PROVIDER`) |
| AI Model | `gpt-4o-mini` (from CI variable `E_AI_OPENAI_MODEL`) |
| Spring Boot | 3.5.0 |
| Java | 21 (eclipse-temurin:21-jre-alpine) |
| Node | 22 (node:22-alpine) |
| CI Pipeline | `.gitlab-ci.yml` — `build` → `deploy-staging` (manual) |

No secret values documented.

---

## 3. Pre-Deployment Checks

| # | Check | Result |
|---|-------|--------|
| 1 | Git branch `dev` clean | ✅ No uncommitted changes |
| 2 | Commit hash | `d2b5451` |
| 3 | Backend tests | ✅ 295/295, 0 failures, BUILD SUCCESS |
| 4 | Frontend TSC | ✅ Clean, no errors |
| 5 | Frontend Vite build | ✅ Built in 757ms |
| 6 | Migration file exists | ✅ `V4__fp002_scenario_tables.sql` |
| 7 | No secret committed | ✅ Only placeholders (`YOUR_OPENAI_API_KEY_HERE`) |
| 8 | Stack target | `anadolu-atlasi-staging` (no rename) |
| 9 | CI deploy-staging defined | ✅ `when: manual` in `.gitlab-ci.yml` |
| 10 | CI variables required | `SSH_PRIVATE_KEY`, `DEPLOY_SERVER`, `E_AI_PROVIDER`, `E_AI_OPENAI_API_KEY` |
| 11 | Rollback image identifiable | Before deploy: record current image tag |

---

## 4. Deployment — Operator Instructions

### Step 1: Record Rollback Tags (BEFORE deploy)

SSH into staging server and record current image tags:

```bash
docker service inspect anadolu-atlasi-staging_backend --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}'
docker service inspect anadolu-atlasi-staging_admin-web --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}'
```

Record these tags for emergency rollback.

### Step 2: Trigger CI/CD Pipeline

Option A — GitLab UI:
1. Go to GitLab → CI/CD → Pipelines
2. Find the latest `dev` pipeline for commit `d2b5451`
3. Click `build-backend` and `build-admin-web` (if not auto-triggered)
4. After build passes, click `deploy-staging` (manual trigger)

Option B — GitLab CLI:
```bash
# Trigger from GitLab API or git push (build is auto on dev push)
git push origin dev  # Already pushed — pipeline should exist
```

### Step 3: Verify Deployment

After `deploy-staging` completes (includes 30s wait):

```bash
# Check services
docker stack services anadolu-atlasi-staging
# Expected: backend 1/1, admin-web 1/1, redis 1/1

# Check backend health
curl -s http://localhost:8081/actuator/health | jq '.status'
# Expected: "UP"
```

### Deployment Result

| Check | Status |
|-------|--------|
| CI pipeline triggered | ⏳ Operator action required |
| Build passes | ⏳ Awaiting CI |
| Deploy completes | ⏳ Awaiting CI |
| Backend 1/1 | ⏳ Awaiting verification |
| Admin-web 1/1 | ⏳ Awaiting verification |

---

## 5. Migration Verification — Operator Checklist

After backend starts, Flyway will automatically apply `V4__fp002_scenario_tables.sql`.

### Verify from backend logs:

```bash
docker service logs anadolu-atlasi-staging_backend --tail 100 2>&1 | grep -i "flyway\|V4__\|migrat"
# Expected: "Successfully applied 1 migration" or "Schema is up to date"
# No Flyway errors
```

### Verify from database (connect to PostgreSQL):

```sql
-- 1. Tables exist
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name IN ('scenario_drafts', 'scenario_beats');
-- Expected: 2 rows

-- 2. No raw prompt/payload columns
SELECT column_name FROM information_schema.columns
WHERE table_name = 'scenario_drafts'
AND column_name IN ('raw_prompt', 'raw_payload', 'raw_response', 'api_key');
-- Expected: 0 rows

-- 3. FK constraints exist
SELECT constraint_name FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
AND table_name IN ('scenario_drafts', 'scenario_beats');
-- Expected: fk_scenario_drafts_content_project, fk_scenario_drafts_source_draft, fk_scenario_beats_scenario_draft

-- 4. Indexes exist
SELECT indexname FROM pg_indexes WHERE tablename IN ('scenario_drafts', 'scenario_beats');
-- Expected: 5 indexes (project_id, source_draft_id, status, project_version, draft_id)

-- 5. Flyway history
SELECT version, description, success FROM flyway_schema_history WHERE version = '4';
-- Expected: version=4, description='fp002 scenario tables', success=true
```

| Check | Status |
|-------|--------|
| Flyway V4 applied | ⏳ Awaiting verification |
| `scenario_drafts` table exists | ⏳ |
| `scenario_beats` table exists | ⏳ |
| FK constraints exist | ⏳ |
| Indexes exist | ⏳ |
| No raw_prompt column | ⏳ |
| No raw_payload column | ⏳ |

---

## 6. Backend Health Verification — Operator Checklist

```bash
# 1. Service running
docker service ls | grep anadolu-atlasi-staging_backend
# Expected: 1/1

# 2. Health endpoint
curl -s http://localhost:8081/actuator/health | jq '.status'
# Expected: "UP"

# 3. No startup errors
docker service logs anadolu-atlasi-staging_backend --tail 100 2>&1 | grep -i "error\|exception\|NoSuchBean"
# Expected: No errors

# 4. Scenario gateway active
docker service logs anadolu-atlasi-staging_backend --tail 100 2>&1 | grep -i "ScenarioAgent\|scenario.*gateway"
# Expected: No missing bean errors

# 5. FP-001 endpoints available
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/knowledge-items
# Expected: 200

curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/content-projects
# Expected: 200

# 6. FP-002 endpoints available
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/scenarios/00000000-0000-0000-0000-000000000000
# Expected: 404 (endpoint exists but no data — not 405 or 500)
```

| Check | Status |
|-------|--------|
| Backend 1/1 | ⏳ |
| Health = UP | ⏳ |
| No startup errors | ⏳ |
| No missing bean errors | ⏳ |
| FP-001 endpoints respond | ⏳ |
| FP-002 endpoints respond | ⏳ |

---

## 7. Admin UI Verification — Operator Checklist

Access `http://<server-ip>:5174` in browser.

| # | Check | Expected | Status |
|---|-------|----------|--------|
| 1 | Frontend loads | Dashboard visible | ⏳ |
| 2 | Content Projects page | List renders | ⏳ |
| 3 | Content Project detail | Detail view opens | ⏳ |
| 4 | Scenario section visible | "Senaryo Yönetimi" section in detail | ⏳ |
| 5 | "Senaryo Üret" button | Visible when approved ContentDraft exists | ⏳ |
| 6 | Scenario list table | Renders after generation | ⏳ |
| 7 | Scenario detail view | Opens with 6 beat fields | ⏳ |
| 8 | Edit form | Title, summary, beats editable | ⏳ |
| 9 | Save button | Works, status → NEEDS_REVIEW | ⏳ |
| 10 | Approve button | Visible for AI_GENERATED/NEEDS_REVIEW | ⏳ |
| 11 | Approve confirmation | ConfirmDialog shown | ⏳ |
| 12 | Reject button | Visible for AI_GENERATED/NEEDS_REVIEW | ⏳ |
| 13 | Reject dialog | NoteDialog with required reason | ⏳ |
| 14 | Read-only for APPROVED | Fields not editable | ⏳ |
| 15 | No generic status dropdown | Not present | ⏳ |
| 16 | No raw prompt in UI | Not visible | ⏳ |

---

## 8. FP-001 Regression Check — Operator Checklist

```bash
# 1. Knowledge Items endpoint
curl -s http://localhost:8081/api/admin/knowledge-items | jq '.success'
# Expected: true

# 2. Content Projects endpoint
curl -s http://localhost:8081/api/admin/content-projects | jq '.success'
# Expected: true

# 3. Test FP-001 draft generation (use existing project with READY knowledge item)
PROJECT_ID="<existing-project-id>"
curl -s -X POST "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/generate" \
  -H "Content-Type: application/json" | jq '{success: .data.success, status: .data.status}'
# Expected: success=true (real OpenAI provider)
```

| Check | Status |
|-------|--------|
| Knowledge Item workflow | ⏳ |
| Content Project workflow | ⏳ |
| FP-001 AI draft generation | ⏳ |
| Draft review/edit | ⏳ |
| Approve action explicit | ⏳ |
| Ready for publish explicit | ⏳ |
| No generic status update | ⏳ |
| Real provider still active | ⏳ |

---

## 9. FP-002 Real OpenAI Scenario Smoke — Operator Checklist

### Prerequisites

- [ ] Verified `AI_PROVIDER=openai` in runtime env
- [ ] Identified Content Project with APPROVED ContentDraft
- [ ] Backend is healthy and scenario gateway is active

### Smoke Steps

```bash
# 1. Generate scenario
PROJECT_ID="<project-with-approved-draft>"
curl -s -X POST "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/scenarios/generate" \
  -H "Content-Type: application/json" | jq '{
    success: .data.success,
    status: .data.status,
    scenarioTitle: .data.scenarioTitle,
    version: .data.version,
    scenarioDraftId: .data.scenarioDraftId
  }'
# Expected: success=true, status=AI_GENERATED, version=1

# 2. Get scenario detail
SCENARIO_ID="<id-from-step-1>"
curl -s "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID" | jq '{
    status: .data.status,
    beatCount: (.data.beats | length),
    beatTypes: [.data.beats[].type],
    providerType: .data.providerType,
    modelName: .data.modelName,
    title: .data.title
  }'
# Expected: status=AI_GENERATED, beatCount=6, providerType=OPENAI, modelName=gpt-4o-mini
# Beat types: HOOK, INTRODUCTION, MAIN_NARRATIVE, EMOTIONAL_CLIMAX, CONCLUSION, CALL_TO_ACTION

# 3. Verify beat bodies non-empty
curl -s "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID" | jq '[.data.beats[] | .body | length > 0] | all'
# Expected: true

# 4. Edit scenario
curl -s -X PUT "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID" \
  -H "Content-Type: application/json" \
  -d '{"title": "Düzenlenmiş Senaryo Başlığı"}' | jq '.data.status'
# Expected: "NEEDS_REVIEW"

# 5. Approve scenario
curl -s -X POST "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID/approve" | jq '.data.status'
# Expected: "APPROVED"

# 6. Generate v2
curl -s -X POST "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/scenarios/generate" \
  -H "Content-Type: application/json" | jq '{success: .data.success, version: .data.version, status: .data.status}'
# Expected: success=true, version=2, status=AI_GENERATED

# 7. Try approve v2 (should fail — one-approved rule)
SCENARIO_V2_ID="<id-from-step-6>"
curl -s -X POST "http://localhost:8081/api/admin/scenarios/$SCENARIO_V2_ID/approve" | jq '.'
# Expected: error — already approved scenario exists

# 8. Reject v2
curl -s -X POST "http://localhost:8081/api/admin/scenarios/$SCENARIO_V2_ID/reject" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Kalite yetersiz - staging smoke test"}' | jq '.data.status'
# Expected: "REJECTED"

# 9. List all scenarios
curl -s "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/scenarios" | jq '[.data[] | {version, status}]'
# Expected: [{version:1, status:APPROVED}, {version:2, status:REJECTED}]
```

### Smoke Checklist

| # | Check | Expected | Status |
|---|-------|----------|--------|
| 1 | Generate scenario | success, AI_GENERATED | ⏳ |
| 2 | Status = AI_GENERATED (not APPROVED) | ✅ | ⏳ |
| 3 | providerType = OPENAI | OPENAI | ⏳ |
| 4 | modelName = gpt-4o-mini | gpt-4o-mini | ⏳ |
| 5 | Beat count = 6 | 6 | ⏳ |
| 6 | Beat types correct | All 6 present | ⏳ |
| 7 | Beat bodies non-empty | All true | ⏳ |
| 8 | Scenario in admin UI list | Visible | ⏳ |
| 9 | Detail view opens | 6 beats visible | ⏳ |
| 10 | Edit + save | Status → NEEDS_REVIEW | ⏳ |
| 11 | Save does not approve | Status ≠ APPROVED | ⏳ |
| 12 | Approve (explicit) | Status → APPROVED | ⏳ |
| 13 | Generate v2 | version=2, AI_GENERATED | ⏳ |
| 14 | One-approved rule | v2 approve rejected | ⏳ |
| 15 | Reject v2 | Status → REJECTED | ⏳ |
| 16 | No READY_FOR_PUBLISH | Status never changes | ⏳ |
| 17 | Raw prompt not stored | DB check | ⏳ |
| 18 | Raw payload not stored | DB check | ⏳ |

---

## 10. Governance Verification — Operator Checklist

| # | Rule | How to Verify | Status |
|---|------|---------------|--------|
| 1 | AI output not auto-approved | Check status after generate = AI_GENERATED | ⏳ |
| 2 | AI output not READY_FOR_PUBLISH | Status never becomes READY_FOR_PUBLISH | ⏳ |
| 3 | Edit/save does not approve | PUT → status = NEEDS_REVIEW | ⏳ |
| 4 | Approve is explicit action | POST /approve required | ⏳ |
| 5 | Reject is explicit action | POST /reject required | ⏳ |
| 6 | No generic status endpoint | No PUT/POST /status | ⏳ |
| 7 | No generic status dropdown | Visual UI check | ⏳ |
| 8 | Internal AI DTOs not in REST | Response contains ScenarioDraftResponse, not ScenarioAgentRequest | ⏳ |
| 9 | No raw_prompt in DB | `information_schema.columns` check | ⏳ |
| 10 | No raw_payload in DB | `information_schema.columns` check | ⏳ |
| 11 | API key not in logs | `docker service logs ... \| grep "sk-"` returns empty | ⏳ |
| 12 | FP-001 governance intact | Content approve/ready explicit | ⏳ |

---

## 11. Bugs / Fixes

### Pre-Deployment Phase

No bugs found during pre-deployment checks.

- Backend: 295/295 tests pass
- Frontend: TSC clean, Vite build passes
- Migration: V4 file present and valid
- No secrets committed

### Staging Phase

To be updated by operator after deployment.

---

## 12. Rollback Status

### Current State

No rollback needed — deployment not yet executed.

### Rollback Commands (if needed)

```bash
# Tier 1 — Backend image rollback
docker service update \
  --image <previous-backend-image-tag> \
  anadolu-atlasi-staging_backend

# Tier 2 — Frontend image rollback
docker service update \
  --image <previous-admin-web-image-tag> \
  anadolu-atlasi-staging_admin-web

# Tier 3 — Provider fallback (last resort, affects FP-001 too)
docker service update \
  --env-add AI_PROVIDER=mock \
  anadolu-atlasi-staging_backend
```

### DB Rollback Note

V4 migration is additive only (CREATE TABLE). If rollback required:
- Do NOT drop `scenario_drafts` / `scenario_beats` tables
- Previous backend image tolerates unused tables
- Manual table drop requires explicit operator approval

---

## 13. Evidence Captured

### Pre-Deployment Evidence (captured)

| Evidence | Value |
|----------|-------|
| Git commit | `d2b5451f1e5ef3b7eaa2d5711c983f94bdbec9a9` |
| Git branch | `dev` |
| Git status | Clean (no uncommitted) |
| Backend tests | 295/295, 0 failures |
| Frontend TSC | Clean |
| Frontend Vite build | Built in 757ms |
| Migration file | `V4__fp002_scenario_tables.sql` present |
| Secret scan | Only placeholders, no real keys |
| Backend image tag | `registry.gitlab.com/solostartup/turkuproje/backend:dev` |
| Frontend image tag | `registry.gitlab.com/solostartup/turkuproje/admin-web:dev` |
| Spring Boot version | 3.5.0 |
| Stack name | `anadolu-atlasi-staging` |

### Staging Evidence (to be captured by operator)

| Evidence | How |
|----------|-----|
| Service status | `docker stack services anadolu-atlasi-staging` |
| Health endpoint | `curl http://localhost:8081/actuator/health` |
| Flyway log | `docker service logs ... \| grep flyway` |
| DB table check | SQL queries from section 5 |
| Scenario ID/version/status | From API smoke |
| Beat count | From API smoke |
| providerType/modelName | From API smoke |
| Sanitized logs | Startup messages |
| Endpoint status codes | From smoke checklist |

### Never Captured

- ❌ API key values
- ❌ Raw OpenAI prompt
- ❌ Raw OpenAI response
- ❌ Raw provider payload
- ❌ DB password
- ❌ SSH keys

---

## 14. Final Decision

### **PASSED WITH FOLLOW-UP**

**Rationale:**

All pre-deployment validations pass:
- 295/295 backend tests ✅
- Frontend build clean ✅
- Migration file present ✅
- No secrets committed ✅
- CI/CD pipeline configured correctly ✅

**Follow-up required:**

Operator must:
1. Record current rollback image tags before deploy
2. Trigger `deploy-staging` in GitLab CI/CD pipeline
3. Execute the operator checklists in sections 5–10
4. Report results back as staging verification evidence
5. Update this document with actual staging results

**No stop conditions encountered during pre-deployment phase.**

---

## Appendix: Stop Conditions Reference

If any of these occur during staging, STOP and rollback:

| # | Condition | Action |
|---|-----------|--------|
| 1 | Backend fails to start | Rollback backend image |
| 2 | Flyway V4 fails | Rollback; investigate SQL |
| 3 | Missing `AiScenarioAgentGateway` bean | Rollback; investigate config |
| 4 | FP-001 generation breaks | Rollback immediately |
| 5 | OpenAI auth failure | Check API key; do NOT expose |
| 6 | Secret in logs | Rollback; rotate key |
| 7 | Raw prompt/payload in REST/DB | Rollback; investigate |
| 8 | Scenario auto-approves | Rollback; governance violation |
| 9 | Generic status endpoint | Rollback; investigate |
| 10 | UI blocks FP-001 workflow | Rollback frontend |
