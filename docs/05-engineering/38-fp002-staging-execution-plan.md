# FP-002 Staging Execution Plan

## 1. Status

**PLANNING** — Bu doküman yalnızca staging deployment planını içerir.

Deployment, runtime değişikliği, Docker/Swarm modifikasyonu veya key rotation bu doküman kapsamında yapılmaz.

---

## 2. Input Documents

| # | Doküman | Yol |
|---|---------|-----|
| 1 | FP-001 Final Closure Operational Baseline | `34-fp001-final-closure-operational-baseline.md` |
| 2 | FP-002 Product Design | `35-fp002-scenario-generation-workflow-product-design.md` |
| 3 | FP-002 AI Platform Contract Design | `36-fp002-ai-platform-contract-design.md` |
| 4 | FP-002 Engineering Design & Task Breakdown | `37-fp002-engineering-design-and-task-breakdown.md` |
| 5 | FP-002 Task 10 — Local Smoke Test Report | Agent artifact: `fp002_task_10_report.md` |

---

## 3. Environment Baseline

### Stack

| Item | Value |
|------|-------|
| Stack Name | `anadolu-atlasi-staging` |
| Stack Config | `docker-stack.staging.yml` |
| Naming Mismatch | Accepted — OPS cleanup deferred |

### Backend

| Item | Value |
|------|-------|
| Framework | **Spring Boot 3.5.0** |
| Java | 21 |
| Build | Maven |
| Image | `registry.gitlab.com/solostartup/turkuproje/backend:staging` |
| Port | 8081 → 8080 (container) |
| Spring Profile | `local` |
| Health | `GET /actuator/health` |

> [!IMPORTANT]
> **Spring Boot Version Correction:** Task 10 report stated "Spring Boot 4.1.0". This was a typo. Actual version per `pom.xml` is **3.5.0** (`spring-boot-starter-parent:3.5.0`). No version upgrade occurred. All tests and builds have been running against 3.5.0 throughout FP-002.

### Frontend

| Item | Value |
|------|-------|
| Framework | React + Vite |
| Image | `registry.gitlab.com/solostartup/turkuproje/admin-web:staging` |
| Port | 5174 → 5173 (container) |

### Database

| Item | Value |
|------|-------|
| Engine | PostgreSQL (host-level service, not container) |
| Access | Via `172.17.0.1:5432` (docker0 bridge) |
| Database | `anadolu_atlasi_staging` |
| User | `anadolu_staging` |
| Password | Runtime env variable (not committed) |
| Flyway | Enabled in staging profile |
| Migration | `V4__fp002_scenario_tables.sql` — new tables: `scenario_drafts`, `scenario_beats` |

### AI Provider

| Item | Value |
|------|-------|
| Provider | OpenAI |
| Runtime Env | `AI_PROVIDER=openai` (from FP-001 production baseline) |
| Model | `gpt-4o-mini` (env: `AI_OPENAI_MODEL`) |
| API Key Source | Runtime env variable `AI_OPENAI_API_KEY` (not committed) |
| FP-001 Status | Active — real OpenAI content generation operational |
| FP-002 Gateway | `OpenAiScenarioAgentGateway` (new, Task 09) |
| Mock Fallback | `MockAiScenarioAgentGateway` via `@ConditionalOnMissingBean` |

---

## 4. Deployment Pre-Checks

Operator must verify before deploying:

- [ ] Git branch: `dev` (or merge to `main` per CI convention)
- [ ] Commit hash recorded (latest FP-002 commit)
- [ ] Backend tests pass: `mvn test` — 295/295
- [ ] Frontend build passes: `npx tsc --noEmit && npx vite build`
- [ ] Migration file present: `V4__fp002_scenario_tables.sql`
- [ ] No secrets committed in source (grep for `sk-proj`, `api-key`, password values)
- [ ] OpenAI API key available in runtime env (`AI_OPENAI_API_KEY`)
- [ ] `AI_PROVIDER=openai` remains set (FP-001 baseline)
- [ ] Rollback image tag identified: current running backend image tag before deploy
- [ ] Mock fallback available: `@ConditionalOnMissingBean` registered
- [ ] Operator has SSH/log access to staging server
- [ ] Current service status healthy before deploy

### Record Before Deploy

```bash
# Record current image tags for rollback
docker service inspect anadolu-atlasi-staging_backend --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}'
docker service inspect anadolu-atlasi-staging_admin-web --format '{{.Spec.TaskTemplate.ContainerSpec.Image}}'
```

---

## 5. Database Migration Checks

### Expected Migration: V4

Flyway will automatically apply `V4__fp002_scenario_tables.sql` on first startup.

### Post-Migration Validation

```sql
-- 1. Verify tables exist
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name IN ('scenario_drafts', 'scenario_beats');
-- Expected: 2 rows

-- 2. Verify scenario_drafts columns (no raw prompt/payload columns)
SELECT column_name FROM information_schema.columns
WHERE table_name = 'scenario_drafts' ORDER BY ordinal_position;
-- Expected: id, content_project_id, source_draft_id, version, status, title,
--   summary, target_audience, cultural_objective, tone, estimated_duration_seconds,
--   setting, characters, review_notes, generated_by, provider_type, model_name,
--   warnings_json, created_at, updated_at, approved_at, rejected_at, edited_at,
--   created_by, approved_by, rejected_by, edited_by
-- MUST NOT contain: raw_prompt, raw_payload, raw_response, api_key

-- 3. Verify scenario_beats columns
SELECT column_name FROM information_schema.columns
WHERE table_name = 'scenario_beats' ORDER BY ordinal_position;
-- Expected: id, scenario_draft_id, type, label, body, order_index, estimated_duration_seconds

-- 4. Verify FK constraints
SELECT constraint_name, table_name FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
AND table_name IN ('scenario_drafts', 'scenario_beats');
-- Expected: fk_scenario_drafts_content_project, fk_scenario_drafts_source_draft,
--   fk_scenario_beats_scenario_draft

-- 5. Verify indexes
SELECT indexname FROM pg_indexes
WHERE tablename IN ('scenario_drafts', 'scenario_beats');
-- Expected: idx_scenario_drafts_project_id, idx_scenario_drafts_source_draft_id,
--   idx_scenario_drafts_status, idx_scenario_drafts_project_version,
--   idx_scenario_beats_draft_id

-- 6. Verify migration applied
SELECT version, description, success FROM flyway_schema_history
WHERE version = '4';
-- Expected: version=4, success=true
```

### Safety Rules

- Migration is additive only (CREATE TABLE, CREATE INDEX)
- No ALTER or DROP on existing FP-001 tables
- No destructive operations
- Idempotent: Flyway tracks applied migrations, will not re-apply

---

## 6. Backend Health Checks

After deployment, verify:

- [ ] Container starts and stays running

```bash
docker service ls | grep anadolu-atlasi-staging_backend
# Expected: 1/1 replicas
```

- [ ] Health endpoint responds

```bash
curl -s http://localhost:8081/actuator/health | jq '.status'
# Expected: "UP"
```

- [ ] Startup logs clean

```bash
docker service logs anadolu-atlasi-staging_backend --tail 50 2>&1 | grep -i "started\|error\|exception\|flyway"
# Expected: "Started AnadoluAtlasiBackendApplication" present
# Expected: No startup exceptions
# Expected: "AI Scenario Provider: OPENAI" present
```

- [ ] No missing bean errors

```bash
docker service logs anadolu-atlasi-staging_backend --tail 100 2>&1 | grep -i "AiScenarioAgentGateway\|NoSuchBean"
# Expected: No missing bean errors
```

- [ ] Flyway migration successful

```bash
docker service logs anadolu-atlasi-staging_backend --tail 100 2>&1 | grep -i "flyway\|V4__"
# Expected: "Successfully applied 1 migration" or "Schema is up to date"
```

- [ ] FP-001 endpoints still available

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/knowledge-items
# Expected: 200

curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/content-projects
# Expected: 200
```

- [ ] FP-002 endpoints available

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/admin/scenarios/00000000-0000-0000-0000-000000000000
# Expected: 404 (not found, but endpoint exists — not 405/500)
```

---

## 7. Admin UI Checks

After frontend deployment:

- [ ] Frontend loads at `http://<server>:5174`
- [ ] Content Projects page loads
- [ ] Content Project detail page opens
- [ ] **Scenario Workflow section visible** in project detail
- [ ] "Senaryo Üret" button visible when approved ContentDraft exists
- [ ] Scenario list table renders (initially empty)
- [ ] After generation: scenario appears in list with status badge
- [ ] Scenario detail opens with 6 beat fields
- [ ] Edit form allows modifying title, summary, beats
- [ ] Save button works → status changes to NEEDS_REVIEW
- [ ] Approve button visible for AI_GENERATED and NEEDS_REVIEW
- [ ] Approve triggers confirmation dialog
- [ ] Reject button visible for AI_GENERATED and NEEDS_REVIEW
- [ ] Reject triggers note dialog with required reason
- [ ] APPROVED/REJECTED/FAILED drafts show read-only fields
- [ ] No generic status dropdown exists
- [ ] No raw prompt/provider payload visible in UI

---

## 8. API Smoke Checklist

### Endpoints

| # | Method | Endpoint | Expected Success | Expected Response |
|---|--------|----------|------------------|-------------------|
| 1 | POST | `/api/admin/content-projects/{projectId}/scenarios/generate` | 201 Created | `{success: true, scenarioTitle, version, status: "AI_GENERATED"}` |
| 2 | GET | `/api/admin/content-projects/{projectId}/scenarios` | 200 OK | Array of ScenarioDraftSummary |
| 3 | GET | `/api/admin/scenarios/{scenarioId}` | 200 OK | ScenarioDraftResponse with beats[] |
| 4 | PUT | `/api/admin/scenarios/{scenarioId}` | 200 OK | Updated ScenarioDraftResponse, status → NEEDS_REVIEW |
| 5 | POST | `/api/admin/scenarios/{scenarioId}/approve` | 200 OK | `{status: "APPROVED"}` |
| 6 | POST | `/api/admin/scenarios/{scenarioId}/reject` | 200 OK | `{status: "REJECTED"}` |

### Smoke Sequence

```bash
# Prerequisite: Get a projectId with approved ContentDraft
PROJECT_ID="<your-project-id>"

# 1. Generate scenario
curl -s -X POST "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/scenarios/generate" \
  -H "Content-Type: application/json" | jq '{success: .data.success, status: .data.status, version: .data.version}'

# 2. List scenarios
curl -s "http://localhost:8081/api/admin/content-projects/$PROJECT_ID/scenarios" | jq '.data | length'

# 3. Get scenario detail (use scenarioId from step 1)
SCENARIO_ID="<scenario-id-from-step-1>"
curl -s "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID" | jq '{status: .data.status, beatCount: (.data.beats | length), providerType: .data.providerType, modelName: .data.modelName}'

# 4. Edit scenario
curl -s -X PUT "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID" \
  -H "Content-Type: application/json" \
  -d '{"title": "Düzenlenmiş Başlık"}' | jq '.data.status'
# Expected: "NEEDS_REVIEW"

# 5. Approve scenario
curl -s -X POST "http://localhost:8081/api/admin/scenarios/$SCENARIO_ID/approve" | jq '.data.status'
# Expected: "APPROVED"
```

---

## 9. Real OpenAI Scenario Smoke Checklist

### Step-by-Step Validation

| # | Step | Expected Result | Record |
|---|------|-----------------|--------|
| 1 | Verify `AI_PROVIDER=openai` in env | Active | ✅/❌ |
| 2 | Verify `OpenAiScenarioAgentGateway` in logs | "AI Scenario Provider: OPENAI" | ✅/❌ |
| 3 | Identify Content Project with approved draft | projectId available | ID |
| 4 | POST generate scenario | 201, success=true | scenarioId, version |
| 5 | Verify status = AI_GENERATED | Not APPROVED, not READY_FOR_PUBLISH | status |
| 6 | Verify beat count = 6 | Exactly 6 beats | count |
| 7 | Verify beat types | HOOK, INTRO, MAIN, CLIMAX, CONCLUSION, CTA | list |
| 8 | Verify each beat has non-empty body | All bodies present | ✅/❌ |
| 9 | Verify providerType = "OPENAI" | Safe metadata only | value |
| 10 | Verify modelName = "gpt-4o-mini" | Matches config | value |
| 11 | Verify no raw_prompt column in DB | Column does not exist | ✅/❌ |
| 12 | Verify no raw_payload column in DB | Column does not exist | ✅/❌ |
| 13 | Verify title is meaningful Turkish text | Quality check | ✅/❌ |
| 14 | Verify beat quality at high level | Relevant, non-trivial content | ✅/❌ |
| 15 | Edit a beat body and save | Status → NEEDS_REVIEW | ✅/❌ |
| 16 | Approve scenario | Status → APPROVED | ✅/❌ |
| 17 | Verify approvedAt is set | Timestamp present | ✅/❌ |
| 18 | Generate scenario v2 | version=2, AI_GENERATED | ✅/❌ |
| 19 | Try approve v2 | Should fail (one-approved rule) | ✅/❌ |
| 20 | Reject v2 with reason | Status → REJECTED | ✅/❌ |

### Safety Rules During OpenAI Smoke

- **DO NOT** print raw prompt text
- **DO NOT** print raw OpenAI API response body
- **DO NOT** print API key values
- **DO NOT** screenshot prompt/response content
- Record only: scenarioId, version, status, beatCount, providerType, modelName, endpoint status codes

---

## 10. Governance Checks

| # | Rule | Validation Method | Expected |
|---|------|-------------------|----------|
| 1 | AI output not auto-approved | Check status after generate | AI_GENERATED |
| 2 | AI output not READY_FOR_PUBLISH | Check status enum | No such status |
| 3 | Approval requires explicit action | POST /approve endpoint only | ✅ |
| 4 | Rejection requires explicit action | POST /reject endpoint only | ✅ |
| 5 | Save/edit does not approve | PUT /scenarios/{id} → NEEDS_REVIEW | ✅ |
| 6 | No generic status update endpoint | No PUT/POST /status exists | ✅ |
| 7 | No generic status dropdown in UI | Visual check | ✅ |
| 8 | Raw prompt not stored in DB | `information_schema.columns` check | No column |
| 9 | Raw provider payload not stored | `information_schema.columns` check | No column |
| 10 | API key not exposed via REST | Check all endpoint responses | No key |
| 11 | Internal AI DTOs not in REST | ScenarioAgentRequest/Response not returned | ✅ |
| 12 | FP-001 governance still holds | Content generation still works | ✅ |

---

## 11. Rollback Plan

### Tier 1 — Backend Image Rollback

If backend fails to start or critical issue found:

```bash
# Roll back to previous image
docker service update \
  --image <previous-backend-image-tag> \
  anadolu-atlasi-staging_backend
```

### Tier 2 — Frontend Image Rollback

If UI regression occurs:

```bash
docker service update \
  --image <previous-admin-web-image-tag> \
  anadolu-atlasi-staging_admin-web
```

### Tier 3 — Provider Fallback

If OpenAI scenario provider fails but backend is otherwise healthy:

```bash
# Switch to mock provider (affects both FP-001 content and FP-002 scenario)
docker service update \
  --env-add AI_PROVIDER=mock \
  anadolu-atlasi-staging_backend
```

> [!WARNING]
> This also switches FP-001 content generation to mock. Only use if OpenAI is the root cause of failure.

### Database Considerations

- V4 migration is **additive only** (CREATE TABLE, CREATE INDEX)
- **Do NOT drop `scenario_drafts` or `scenario_beats`** tables during emergency rollback
- Previous backend image will tolerate unused scenario tables (no entity references in old code)
- If migration caused a critical issue, investigate before manual intervention
- Manual table drop requires explicit operator approval

### Key Rotation

- Do NOT rotate keys unless there is a confirmed secret exposure incident
- Key rotation is a separate operational task

---

## 12. Failure Handling / Stop Conditions

### 🔴 STOP — Do Not Continue If:

| # | Condition | Action |
|---|-----------|--------|
| 1 | Backend fails to start | Rollback to previous image |
| 2 | Flyway migration V4 fails | Rollback; investigate migration SQL |
| 3 | Missing `AiScenarioAgentGateway` bean error | Rollback; investigate bean config |
| 4 | FP-001 content generation breaks | Rollback immediately |
| 5 | OpenAI auth/config failure on scenario | Check `AI_OPENAI_API_KEY`; do NOT expose key in logs |
| 6 | Raw secret appears in logs | Rollback; rotate key if exposed |
| 7 | Raw prompt/provider payload in REST response | Rollback; investigate DTO mapping |
| 8 | Raw prompt/provider payload column in DB | Rollback; investigate migration |
| 9 | Scenario auto-approves (status APPROVED without admin action) | Rollback; critical governance violation |
| 10 | Generic status endpoint appears | Rollback; investigate controller |
| 11 | Admin UI blocks FP-001 content workflow | Rollback frontend; investigate |
| 12 | Health endpoint returns DOWN after deploy | Rollback; check logs |

### 🟡 INVESTIGATE — Continue With Caution If:

| Condition | Action |
|-----------|--------|
| OpenAI scenario returns low quality content | Acceptable — mock fallback available |
| OpenAI rate limit hit during smoke | Retry after cooldown |
| Single beat body too short | Note for prompt tuning; not a blocker |

---

## 13. Evidence to Capture

### ✅ Safe to Record

| Evidence | Example |
|----------|---------|
| Git commit hash | `7501b1e` |
| Backend image tag | `registry.gitlab.com/.../backend:staging` |
| Frontend image tag | `registry.gitlab.com/.../admin-web:staging` |
| Test/build command results | `295/295 tests, 0 failures` |
| Service status | `1/1 replicas` |
| Health endpoint response | `{"status": "UP"}` |
| Flyway migration confirmation | `V4 applied, success=true` |
| Endpoint status codes | `201`, `200`, `404` |
| Scenario ID | UUID |
| Scenario version | `1`, `2` |
| Scenario status | `AI_GENERATED`, `APPROVED`, `REJECTED` |
| Beat count | `6` |
| providerType | `OPENAI` |
| modelName | `gpt-4o-mini` |
| Sanitized log excerpts | Startup messages, gateway activation |
| Admin UI screenshots | Without secrets/raw provider payload |
| Table/column verification query results | Schema checks |

### ❌ NEVER Record

| Evidence | Reason |
|----------|--------|
| API key values | Secret |
| Raw OpenAI prompt text | Internal |
| Raw OpenAI response body | Internal |
| Full provider payload | Internal |
| Database passwords | Secret |
| SSH keys or credentials | Secret |
| Token usage details beyond safe metadata | Unnecessary |

---

## 14. Acceptance Criteria

This document is accepted if all sections are present and complete:

- [x] Status: PLANNING
- [x] Input documents referenced
- [x] Environment baseline documented
- [x] Actual Spring Boot version verified: **3.5.0**
- [x] Task 10 version typo corrected
- [x] Deployment pre-checks documented
- [x] Database migration checks documented
- [x] Backend health checks documented
- [x] Admin UI checks documented
- [x] API smoke checklist documented
- [x] Real OpenAI smoke checklist documented
- [x] Governance checks documented
- [x] Rollback plan documented
- [x] Stop conditions documented
- [x] Safe evidence rules documented
- [x] No code/config/deployment changes made
- [x] No secrets/raw prompt/raw provider payload documented

---

## Flow A — Mock Provider Staging Note

Mock provider staging smoke is **not planned** for this execution.

**Reason:** Staging runtime currently runs with `AI_PROVIDER=openai` (FP-001 production baseline). Switching to `AI_PROVIDER=mock` would temporarily disable FP-001 real OpenAI content generation. Since FP-002 does not require mock staging validation (Task 10 local smoke already validated mock flow programmatically with 295/295 tests), mock staging smoke is skipped to avoid disrupting FP-001 baseline.

FP-002 staging validation focuses exclusively on **Flow B — Real OpenAI Scenario Provider**.
