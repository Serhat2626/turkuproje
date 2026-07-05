# FP-011 — Production Storage Enablement & Runtime Operations — Combined Operational Design + Rollout Plan

## 1. Document Status

| Field | Value |
|-------|-------|
| **Feature** | FP-011 — Production Storage Enablement & Runtime Operations |
| **Document Type** | Combined Operational Design + Rollout Plan |
| **Status** | PLANNED |
| **Source Baseline** | [71-fp010-final-closure-operational-baseline.md](71-fp010-final-closure-operational-baseline.md) |
| **FP-010 Test Baseline** | 1471/1471 passing — BUILD SUCCESS |
| **Created** | 2026-07-05 |

> This task is **documentation-only**. No code, tests, migrations, or config were changed.

---

## 2. FP-010 Baseline Summary

FP-011 inherits the following from FP-010:

| Capability | Status |
|-----------|--------|
| Real HTTP provider output download client (`HttpProviderOutputRetrievalClient`) | Exists |
| Provider download config-gated (`imagegen.provider.download.enabled=false`) | Disabled by default |
| S3 private storage adapter (`S3ImageStorageAdapter` + `S3StorageClient`) | Exists |
| S3 config production validation (`S3PrivateStorageProperties.validateForProduction()`) | Exists |
| `InMemoryImageStorageAdapter` fallback via `@ConditionalOnMissingBean` | Active when S3 disabled |
| Immediate handoff service (`ImmediateStorageHandoffService`) | Exists |
| Immediate handoff config-gated (`imagegen.storage.immediate-handoff.enabled=false`) | Disabled by default |
| `generateWithOutputReference()` on gateway interface | Exists |
| Preview proxy backend-controlled byte streaming | Active |
| Operational regression tests | 1471/1471 passing |
| Production credential/bucket/runtime enablement | **Deferred — FP-011 scope** |

---

## 3. FP-011 Purpose

FP-011 moves from **implementation infrastructure** (FP-010) to **production operational readiness**.

FP-011 answers:

| Question | FP-011 Deliverable |
|----------|-------------------|
| What secrets/configs are required for production? | Production configuration model |
| How do we validate config safely? | Startup/env validation design + code |
| How do we provision bucket/container safely? | Provisioning checklist |
| How do we know storage is ready without exposing internals? | Internal readiness check |
| How do we enable runtime flags safely? | Rollout strategy |
| How do we monitor storage operations? | Monitoring/alerting plan |
| How do we roll out gradually? | Staged rollout plan |
| How do we roll back safely? | Rollback strategy |
| How do we preserve FP-009/FP-010 safety boundaries? | Security baseline enforcement |

---

## 4. Product / Operational Value

| Value | Description |
|-------|-------------|
| **Safe production enablement** | Production can enable real storage with confidence |
| **Fail-safe misconfiguration** | Missing/invalid config fails startup safely, no secret leak |
| **Operator visibility** | Operators can verify readiness without seeing secrets |
| **Observable failures** | Failures are observable via safe categories only |
| **Staged rollout** | Rollout can be staged: storage → download → handoff |
| **Defined rollback** | Each stage has explicit rollback steps |
| **Surface safety** | Admin/user surfaces remain safe throughout |

---

## 5. In Scope

- Production credential provisioning plan
- Secret management boundary definition
- Bucket/container provisioning checklist
- Runtime enablement checklist
- Startup/env validation design and implementation
- Internal storage readiness check design and implementation
- Internal provider download readiness check design
- Safe operational health/readiness model
- Monitoring/alerting plan
- Safe logs/metrics plan
- Rollout strategy (staged)
- Rollback strategy
- Production smoke test plan
- Incident response notes
- Regression test plan
- Small coding tasks for validation/health/observability

---

## 6. Out of Scope

| Category | Items |
|----------|-------|
| **Delivery** | CDN/public delivery, signed URL delivery, public URL generation, public-read storage |
| **Workflow** | Publish/export, automatic approval, READY_FOR_PUBLISH, COMPLETED, generic status update endpoint |
| **Media** | Video/audio, user-facing media library |
| **Safety violations** | Provider URL persistence/DTO/API/logging, storage key admin/user exposure/logging, base64 DB/DTO/log/API, raw provider/storage payload exposure, secret/API key/config exposure, public config inspection endpoint, hardcoded credentials, committed secrets |
| **Data** | Destructive migration/backfill |

---

## 7. Production Configuration Model

### Provider Download Config

| Property | Key | Description | Secret |
|----------|-----|-------------|--------|
| Enabled | `imagegen.provider.download.enabled` | Activate real HTTP download | No |
| Connect timeout | `imagegen.provider.download.connect-timeout-ms` | Connection timeout in ms | No |
| Read timeout | `imagegen.provider.download.read-timeout-ms` | Read timeout in ms | No |
| Max bytes | `imagegen.provider.download.max-bytes` | Maximum download size | No |
| Max redirects | `imagegen.provider.download.max-redirects` | Redirect follow limit | No |
| Allowed MIME types | `imagegen.provider.download.allowed-mime-types` | Accepted content types | No |

### Storage Config

| Property | Key | Description | Secret |
|----------|-----|-------------|--------|
| Enabled | `imagegen.storage.s3.enabled` | Activate real S3 adapter | No |
| Endpoint | `imagegen.storage.s3.endpoint` | S3-compatible endpoint URL | No |
| Region | `imagegen.storage.s3.region` | AWS/S3 region | No |
| Bucket | `imagegen.storage.s3.bucket` | Private bucket name | No |
| Path prefix | `imagegen.storage.s3.path-prefix` | Object key prefix | No |
| Access key | `imagegen.storage.s3.access-key` | IAM/service account key | **YES** |
| Secret key | `imagegen.storage.s3.secret-key` | IAM/service account secret | **YES** |

### Immediate Handoff Config

| Property | Key | Description | Secret |
|----------|-----|-------------|--------|
| Enabled | `imagegen.storage.immediate-handoff.enabled` | Activate immediate handoff after generation | No |

### Config Rules

- ❌ No secret committed to repository
- ❌ No secret logged
- ❌ No secret returned in DTO/API
- ❌ No startup error includes secret value
- ✅ Config validation reports safe missing/invalid **categories** only

---

## 8. Secret Management Strategy

| Rule | Description |
|------|-------------|
| **Source** | Secrets provided by runtime environment variables or secret manager only |
| **Repository** | No local/prod secrets in repository |
| **Documentation** | No secrets in docs |
| **Tests** | Fake values only in tests (e.g., `AKIAFAKEKEY`, `fake-secret`) |
| **Logging** | Secret fields excluded from `toString()` / log output |
| **Missing secrets** | Map to safe `INTERNAL_CONFIGURATION_UNAVAILABLE` — no value in message |
| **Rotation** | Rotation support via environment/secret manager restart — no code change needed |
| **Privilege** | Minimal privilege: write + read only, no admin/delete unless cleanup required |
| **Separation** | Separate production/staging credentials — never shared |
| **Validation** | `validateForProduction()` checks presence, never echoes values |

---

## 9. Bucket / Container Provisioning Checklist

| # | Item | Requirement |
|---|------|-------------|
| 1 | Private bucket/container | ✅ Required |
| 2 | Public access blocked | ✅ Block all public access policies |
| 3 | Object ownership/access policy | ✅ Bucket owner enforced |
| 4 | Encryption at rest | ✅ SSE-S3 or SSE-KMS if available |
| 5 | Lifecycle/retention policy | ⚡ Define if needed (e.g., 90-day retention for generated images) |
| 6 | Path prefix strategy | ✅ `imagegen/` or configurable prefix |
| 7 | Service account / access key scope | ✅ Minimal: `PutObject`, `GetObject` on prefix only |
| 8 | No public-read ACL | ✅ Never |
| 9 | No CDN/public binding | ✅ Never in FP-011 |
| 10 | Test object cleanup strategy | ⚡ Define cleanup for smoke test objects |
| 11 | Backup/retention note | ⚡ Document if needed |
| 12 | Versioning | ⚡ Optional — evaluate for audit trail |

---

## 10. Startup / Env Validation Design

### Behavior

| Scenario | Behavior |
|----------|----------|
| Feature disabled | No validation, no failure, no log |
| Feature enabled + config valid | Startup succeeds, INFO log with safe summary |
| Feature enabled + config invalid | Startup fails with safe error code |
| Feature enabled + secret missing | Startup fails with `INTERNAL_CONFIGURATION_UNAVAILABLE` |

### Validation Targets

| Config Group | Validated When | Required Fields |
|-------------|----------------|-----------------|
| S3 storage | `imagegen.storage.s3.enabled=true` | bucket, region, accessKey, secretKey |
| Provider download | `imagegen.provider.download.enabled=true` | timeout values positive, max bytes positive |
| Immediate handoff | `imagegen.storage.immediate-handoff.enabled=true` | No additional — depends on storage + download |

### Safe Failure Codes (Design — not created yet)

| Code | Meaning |
|------|---------|
| `INTERNAL_CONFIGURATION_UNAVAILABLE` | Required config/secret missing |
| `STORAGE_CONFIGURATION_INVALID` | Storage config present but invalid |
| `PROVIDER_DOWNLOAD_CONFIGURATION_INVALID` | Download config present but invalid |

### Rules

- ❌ No secret/config values in exception messages
- ❌ No secret/config values in log output
- ✅ Safe category codes only
- ✅ Report **which** config group failed, not **what** value was missing

---

## 11. Internal Operational Readiness Check Design

### Model

| Status | Meaning |
|--------|---------|
| `READY` | Config valid, adapter active, operational |
| `NOT_READY` | Config valid but not yet verified |
| `DISABLED` | Feature explicitly disabled |
| `MISCONFIGURED` | Config present but invalid/incomplete |
| `DEGRADED` | Partially operational (e.g., storage ok but download failing) |

### Check Targets

| Check | Description |
|-------|-------------|
| Config present | Required properties loaded and non-blank |
| Provider download client enabled | `ProviderOutputDownloadProperties.enabled` |
| Storage adapter enabled | `S3PrivateStorageProperties.enabled` |
| Storage client connectivity | Optional: safe minimal check (e.g., HeadBucket) |
| Test object write/read/delete | Optional: dedicated `__health_check__` object, configured explicitly |

### Rules

- ✅ Internal-only — not public/admin-facing unless explicitly protected
- ❌ Must not expose bucket/key/secrets/provider URLs
- ❌ Must not reveal object key or bucket details in response
- ❌ Must not create public object
- ❌ Must not use real provider URL
- ✅ Returns safe status enum only

---

## 12. Monitoring / Alerting Plan

### Safe Metrics

| Metric | Labels |
|--------|--------|
| `imagegen.provider.download.attempts` | `provider_alias`, `result` (success/failure) |
| `imagegen.provider.download.failures` | `provider_alias`, `error_category` |
| `imagegen.storage.write.attempts` | `storage_provider`, `result` |
| `imagegen.storage.write.failures` | `storage_provider`, `error_category` |
| `imagegen.storage.read.failures` | `storage_provider`, `error_category` |
| `imagegen.preview.read.failures` | `error_category` |
| `imagegen.validation.failures` | `validation_category` |
| `imagegen.immediate_handoff.attempts` | `result` |
| `imagegen.immediate_handoff.failures` | `error_category` |
| `imagegen.config.readiness` | `config_group`, `status` |

### Forbidden Metric Labels

- ❌ provider URL
- ❌ storage object key
- ❌ bucket path
- ❌ access key / secret key / token
- ❌ raw error message
- ❌ base64 / raw payload

### Alert Suggestions

| Alert | Condition |
|-------|-----------|
| Storage write failure spike | > 3 failures in 5 minutes |
| Provider download failure spike | > 3 failures in 5 minutes |
| Storage readiness check failing | Status != READY for > 5 minutes |
| Preview read failure spike | > 5 failures in 10 minutes |
| Config validation failing after deployment | Any MISCONFIGURED status post-deploy |
| Immediate handoff failure spike | > 3 failures in 5 minutes |

---

## 13. Rollout Strategy

### Stage 0 — Documentation & Config Inventory

| Item | Detail |
|------|--------|
| **Prerequisite** | FP-011 design accepted |
| **Action** | Inventory all required config keys and secret keys |
| **Verification** | Checklist reviewed by ops team |
| **Rollback** | N/A — documentation only |

### Stage 1 — Staging Secrets Configured

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 0 complete, staging secret manager access |
| **Action** | Provision staging S3 access key / secret key in secret manager |
| **Verification** | Secrets present in staging env, not in repository |
| **Rollback** | Remove staging secrets from secret manager |

### Stage 2 — Staging Bucket Provisioned

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 1 complete |
| **Action** | Create staging bucket with private-only ACL, encryption, lifecycle policy |
| **Verification** | Bucket exists, public access blocked, service account can write/read |
| **Rollback** | Delete staging bucket, remove credentials |

### Stage 3 — Enable Storage Adapter in Staging

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 2 complete |
| **Action** | Set `imagegen.storage.s3.enabled=true` in staging profile |
| **Verification** | Startup validation passes, readiness check returns READY |
| **Rollback** | Set `imagegen.storage.s3.enabled=false` → InMemory fallback |

### Stage 4 — Enable Provider Download in Staging

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 3 verified |
| **Action** | Set `imagegen.provider.download.enabled=true` in staging profile |
| **Verification** | Download client active, smoke test download works with test URL |
| **Rollback** | Set `imagegen.provider.download.enabled=false` → Mock fallback |

### Stage 5 — Enable Immediate Handoff in Staging

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 3 + Stage 4 verified |
| **Action** | Set `imagegen.storage.immediate-handoff.enabled=true` in staging |
| **Verification** | Generation → download → validation → storage → preview works end-to-end |
| **Rollback** | Set `imagegen.storage.immediate-handoff.enabled=false` → no handoff |

### Stage 6 — Staging Smoke / Regression

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 5 verified |
| **Action** | Run full smoke test suite, verify logs/DTOs/preview safety |
| **Verification** | All tests pass, no secret/URL/key in logs or responses |
| **Rollback** | Disable all flags, revert to mock/InMemory |

### Stage 7 — Production Secrets Configured

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 6 passed |
| **Action** | Provision production S3 access key / secret key in production secret manager |
| **Verification** | Secrets present in production env, not in repository, separate from staging |
| **Rollback** | Remove production secrets from secret manager |

### Stage 8 — Production Bucket Provisioned

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 7 complete |
| **Action** | Create production bucket with private-only ACL, encryption, lifecycle |
| **Verification** | Bucket exists, public access blocked, production service account can write/read |
| **Rollback** | Delete production bucket (only if empty / no real data) |

### Stage 9 — Enable Storage Adapter in Production

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 8 complete |
| **Action** | Set `imagegen.storage.s3.enabled=true` in production profile |
| **Verification** | Startup validation passes, readiness check READY, no InMemory fallback warning |
| **Rollback** | Set `imagegen.storage.s3.enabled=false` → InMemory fallback |

### Stage 10 — Enable Provider Download in Production

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 9 verified |
| **Action** | Set `imagegen.provider.download.enabled=true` in production |
| **Verification** | Download client active, first real generation download succeeds |
| **Rollback** | Set `imagegen.provider.download.enabled=false` → Mock fallback |

### Stage 11 — Enable Immediate Handoff in Production

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 9 + Stage 10 verified |
| **Action** | Set `imagegen.storage.immediate-handoff.enabled=true` in production |
| **Verification** | End-to-end: generation → download → validation → storage → preview works |
| **Rollback** | Set `imagegen.storage.immediate-handoff.enabled=false` |

### Stage 12 — Monitor & Expand

| Item | Detail |
|------|--------|
| **Prerequisite** | Stage 11 verified, monitoring active |
| **Action** | Monitor metrics/alerts for 48+ hours, expand to full traffic |
| **Verification** | No failure spikes, no secret leaks, preview stable |
| **Rollback** | Disable immediate handoff first, then download, then storage adapter |

---

## 14. Rollback Strategy

| Priority | Action | Effect |
|----------|--------|--------|
| 1 | Disable immediate handoff flag | Generation continues without storage handoff |
| 2 | Disable provider download flag | Mock retrieval fallback activates |
| 3 | Disable real S3/private storage flag | InMemory fallback activates |

### Rollback Rules

- In-memory fallback is for non-production only; in production, fallback behavior must be explicitly defined and documented.
- Existing generated assets remain safe — no data loss.
- Stored objects are **not** automatically deleted on rollback.
- Storage keys remain internal-only.
- Preview behavior for already-stored assets is preserved if storage adapter is still accessible.
- Manual cleanup procedure documented if needed.

---

## 15. Production Smoke Test Plan

| # | Test | Verification |
|---|------|-------------|
| 1 | Config validation check | Startup succeeds with safe validation log |
| 2 | Storage readiness check | Returns READY (not MISCONFIGURED) |
| 3 | Test object write/read/delete | Dedicated `__smoke_test__` object, cleanup after |
| 4 | Provider download readiness | Download client reports enabled (no real URL needed) |
| 5 | Generate test image (staging only) | Full end-to-end if allowed |
| 6 | Preview via backend proxy | Byte streaming works, no redirect |
| 7 | DTO safety | No URL/key/base64 in response |
| 8 | Approval safety | No automatic approval after storage |
| 9 | Log safety | No secrets/URLs/keys in application logs |

---

## 16. Security & Safety Baseline

| # | Constraint | Status |
|---|-----------|--------|
| 1 | No provider URL persistence | ✅ Enforced |
| 2 | No provider URL DTO/API exposure | ✅ Enforced |
| 3 | No provider URL logging | ✅ Enforced |
| 4 | No storage key admin/user exposure | ✅ Enforced |
| 5 | No storage key logging | ✅ Enforced |
| 6 | No public URL generation | ✅ Enforced |
| 7 | No signed URL returned to admin/user | ✅ Enforced |
| 8 | No base64 DB/DTO/log/API response | ✅ Enforced |
| 9 | No raw provider/storage payload exposure | ✅ Enforced |
| 10 | No secret/API key/config exposure | ✅ Enforced |
| 11 | No hardcoded credentials | ✅ Enforced |
| 12 | No committed secrets | ✅ Enforced |
| 13 | No public-read storage | ✅ Enforced |
| 14 | Preview backend-controlled | ✅ Enforced |
| 15 | Human review required | ✅ Enforced |
| 16 | No automatic approval | ✅ Enforced |
| 17 | No READY_FOR_PUBLISH | ✅ Enforced |
| 18 | No COMPLETED | ✅ Enforced |
| 19 | No generic status update | ✅ Enforced |
| 20 | No publish/export | ✅ Enforced |
| 21 | No video/audio | ✅ Enforced |

---

## 17. Proposed FP-011 Task Sequence

### Task 02 — Runtime Config Validation & Startup Safety

- Add startup/runtime validation for provider download, S3 storage, immediate handoff configs.
- Safe validation messages only — no secret values in errors/logs.
- Tests for missing/invalid config scenarios.
- No secrets exposed.

### Task 03 — Internal Storage Readiness Check

- Add internal readiness service/check.
- Optionally integrate with existing health/actuator pattern if present.
- Internal-safe response only: `READY` / `NOT_READY` / `DISABLED` / `MISCONFIGURED`.
- No bucket/key/secret exposure.
- Tests for all readiness states.

### Task 04 — Operational Observability & Alerting Hardening

- Add safe metrics/log hooks if project has patterns.
- Add tests for no sensitive labels/loggable strings.
- Document alert thresholds or operational notes.

### Task 05 — Production Rollout Runbook + Regression

- Create production rollout runbook.
- Add regression tests for startup validation / readiness / observability.
- Full test baseline.
- Closure candidate.

### Task 06 — Final Closure / Operational Baseline

- Documentation-only final closure if Task 05 accepted.

---

## 18. Acceptance Criteria

| # | Criterion | Status |
|---|----------|--------|
| 1 | Combined operational design document exists | ✅ |
| 2 | FP-010 closure baseline is referenced | ✅ |
| 3 | FP-011 scope defined as hybrid operational-readiness | ✅ |
| 4 | Production config model documented | ✅ |
| 5 | Secret management strategy documented | ✅ |
| 6 | Bucket/container provisioning checklist documented | ✅ |
| 7 | Startup validation design documented | ✅ |
| 8 | Internal readiness check design documented | ✅ |
| 9 | Monitoring/alerting plan documented | ✅ |
| 10 | Rollout strategy documented (12 stages) | ✅ |
| 11 | Rollback strategy documented | ✅ |
| 12 | Smoke test plan documented | ✅ |
| 13 | Safety baseline documented (21 constraints) | ✅ |
| 14 | Dense task sequence documented (Tasks 02–06) | ✅ |
| 15 | No code/config/migration/test changes made | ✅ |
| 16 | FP-011 remains PLANNED | ✅ |

---

## 19. Next Step

**Next step:** FP-011 Task 02 — Runtime Config Validation & Startup Safety

Task 02 should implement small runtime/startup config validation with safe messages and no secret exposure. Startup validation activates only when the respective feature flag is enabled. Safe failure codes are introduced for missing/invalid configuration. All tests use fake values — no real credentials.
