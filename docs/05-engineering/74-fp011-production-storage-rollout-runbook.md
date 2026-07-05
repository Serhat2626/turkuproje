# FP-011 — Production Storage Rollout Runbook

## 1. Runbook Status

| Field | Value |
|-------|-------|
| **Feature** | FP-011 — Production Storage Enablement & Runtime Operations |
| **Document Type** | Production Rollout Runbook |
| **Status** | READY_FOR_REVIEW |
| **Source Docs** | [Combined Design](72-fp011-production-storage-enablement-runtime-operations-combined-design.md), [Observability](73-fp011-operational-observability-alerting-hardening.md), [FP-010 Closure](71-fp010-final-closure-operational-baseline.md) |
| **Created** | 2026-07-05 |

> This is **documentation-only** and does not enable production configuration.

---

## 2. Scope

### In Scope
- Credential provisioning checklist
- Bucket/container provisioning checklist
- Runtime flag enablement sequence
- Validation/readiness verification
- Smoke tests
- Monitoring checks
- Rollback steps

### Out of Scope
- CDN/public delivery
- Signed URLs
- Publish/export
- Video/audio
- User-facing media library
- Automatic approval
- Public-read storage

---

## 3. Pre-Rollout Checklist

| # | Item | Status |
|---|------|--------|
| 1 | FP-011 final code deployed to staging | ☐ |
| 2 | Secrets available in secret manager/environment | ☐ |
| 3 | No secrets committed to repository | ☐ |
| 4 | Bucket/container provisioned private-only | ☐ |
| 5 | Public access blocked on bucket | ☐ |
| 6 | Runtime flags currently disabled | ☐ |
| 7 | Monitoring/logging dashboard prepared | ☐ |
| 8 | Rollback owner identified | ☐ |
| 9 | Smoke test plan approved | ☐ |
| 10 | Test baseline known (1577/1577) | ☐ |

---

## 4. Required Runtime Configuration Inventory

### Provider Download

| Config Key | Category | Secret |
|-----------|----------|--------|
| `imagegen.provider.download.enabled` | Feature flag | No |
| `imagegen.provider.download.connect-timeout-ms` | Timeout | No |
| `imagegen.provider.download.read-timeout-ms` | Timeout | No |
| `imagegen.provider.download.max-bytes` | Size limit | No |
| `imagegen.provider.download.max-redirects` | Redirect limit | No |
| `imagegen.provider.download.allowed-content-types` | MIME filter | No |

### Private Storage

| Config Key | Category | Secret |
|-----------|----------|--------|
| `imagegen.storage.s3.enabled` | Feature flag | No |
| `imagegen.storage.s3.endpoint` | Infrastructure | No |
| `imagegen.storage.s3.region` | Infrastructure | No |
| `imagegen.storage.s3.bucket` | Infrastructure | No |
| `imagegen.storage.s3.path-prefix` | Infrastructure | No |
| `imagegen.storage.s3.access-key` | Credential | **YES — secret manager only** |
| `imagegen.storage.s3.secret-key` | Credential | **YES — secret manager only** |

### Immediate Handoff

| Config Key | Category | Secret |
|-----------|----------|--------|
| `imagegen.storage.immediate-handoff.enabled` | Feature flag | No |

> ❌ No real values, secrets, access keys, tokens, provider URLs, or storage keys are included in this document.

---

## 5. Staging Rollout Steps

### Step 1 — Deploy with All Flags Disabled

| Item | Detail |
|------|--------|
| **Action** | Deploy FP-011 build to staging with default config (all flags disabled) |
| **Verification** | App starts, readiness check returns DISABLED, no errors |
| **Rollback** | Redeploy previous version |

### Step 2 — Verify Config Validation Safe

| Item | Detail |
|------|--------|
| **Action** | Invoke `RuntimeStorageConfigValidator.validateAll()` internally or via logs |
| **Verification** | All 3 components return valid=true, readiness=DISABLED |
| **Rollback** | N/A — flags are disabled |

### Step 3 — Configure Staging Storage Secrets

| Item | Detail |
|------|--------|
| **Action** | Set staging S3 access-key/secret-key in secret manager |
| **Verification** | Secrets present in staging env, not in repo |
| **Rollback** | Remove staging secrets |

### Step 4 — Provision Private Staging Bucket

| Item | Detail |
|------|--------|
| **Action** | Create staging bucket: private ACL, encryption, lifecycle |
| **Verification** | Bucket exists, public access blocked |
| **Rollback** | Delete staging bucket |

### Step 5 — Enable Private Storage Adapter

| Item | Detail |
|------|--------|
| **Action** | Set `imagegen.storage.s3.enabled=true` in staging |
| **Verification** | Readiness returns PRIVATE_STORAGE=READY |
| **Rollback** | Set `imagegen.storage.s3.enabled=false` |

### Step 6 — Run Internal Readiness Check

| Item | Detail |
|------|--------|
| **Action** | Verify `InternalStorageReadinessService.checkReadiness()` |
| **Verification** | PRIVATE_STORAGE=READY, others as expected |
| **Rollback** | Disable storage flag |

### Step 7 — Enable Provider Download

| Item | Detail |
|------|--------|
| **Action** | Set `imagegen.provider.download.enabled=true` in staging |
| **Verification** | Config validation returns PROVIDER_DOWNLOAD=READY |
| **Rollback** | Set `imagegen.provider.download.enabled=false` |

### Step 8 — Run Provider Download Validation

| Item | Detail |
|------|--------|
| **Action** | Verify `validateProviderDownload()` returns READY |
| **Verification** | Valid config, no errors |
| **Rollback** | Disable provider download |

### Step 9 — Enable Immediate Handoff

| Item | Detail |
|------|--------|
| **Action** | Set `imagegen.storage.immediate-handoff.enabled=true` in staging |
| **Verification** | Readiness returns IMMEDIATE_HANDOFF=READY, overall=READY |
| **Rollback** | Set `imagegen.storage.immediate-handoff.enabled=false` |

### Step 10 — Run Staging Smoke Tests

| Item | Detail |
|------|--------|
| **Action** | Execute smoke test plan (Section 7) |
| **Verification** | All smoke tests pass |
| **Rollback** | Disable all flags, revert to defaults |

### Step 11 — Verify Preview Through Backend Proxy

| Item | Detail |
|------|--------|
| **Action** | Access preview endpoint for a stored asset |
| **Verification** | Bytes stream through backend, no redirect, no URL exposure |
| **Rollback** | N/A — preview is always backend-controlled |

### Step 12 — Verify No Exposure

| Item | Detail |
|------|--------|
| **Action** | Inspect API responses, logs for any URL/key/base64/secret |
| **Verification** | None found |
| **Rollback** | IMMEDIATE: disable all flags + incident if exposure found |

### Step 13 — Monitor Alerts/Logs

| Item | Detail |
|------|--------|
| **Action** | Monitor for 4+ hours in staging |
| **Verification** | No failure spikes, no secret leaks |
| **Rollback** | Disable flags per rollback plan |

---

## 6. Production Rollout Steps

### Step 1 — Deploy with Flags Disabled

| Item | Detail |
|------|--------|
| **Prerequisite** | Staging rollout completed successfully |
| **Action** | Deploy to production with all flags disabled |
| **Verification** | App starts, readiness=DISABLED |
| **Rollback Trigger** | Startup failure |

### Step 2 — Confirm Runtime Validation Safe

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 1 verified |
| **Action** | Check logs for `[CONFIG_VALIDATION]` — all DISABLED |
| **Verification** | No ERROR/MISCONFIGURED in logs |
| **Rollback Trigger** | Validation errors post-deploy |

### Step 3 — Provision Production Private Bucket

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 2 verified |
| **Action** | Create production bucket: private ACL, encryption, lifecycle |
| **Verification** | Bucket exists, public access blocked, service account scoped |
| **Rollback Trigger** | Bucket misconfigured |

### Step 4 — Configure Production Secrets

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 3 verified |
| **Action** | Set production S3 credentials in production secret manager |
| **Verification** | Secrets in env, separate from staging, not in repo |
| **Rollback Trigger** | Secret provisioning failure |

### Step 5 — Enable Private Storage Adapter

| Item | Detail |
|------|--------|
| **Prerequisite** | Steps 3+4 verified |
| **Action** | Set `imagegen.storage.s3.enabled=true` |
| **Verification** | Readiness=READY for PRIVATE_STORAGE, no InMemory fallback warning |
| **Rollback Trigger** | MISCONFIGURED status or startup failure |

### Step 6 — Verify Readiness

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 5 verified |
| **Action** | Check readiness logs |
| **Verification** | PRIVATE_STORAGE=READY |
| **Rollback Trigger** | NOT_READY or MISCONFIGURED |

### Step 7 — Enable Provider Download

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 6 verified |
| **Action** | Set `imagegen.provider.download.enabled=true` |
| **Verification** | PROVIDER_DOWNLOAD=READY |
| **Rollback Trigger** | Download failures > 3 in 5 min |

### Step 8 — Verify Readiness

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 7 verified |
| **Action** | Check readiness — PROVIDER_DOWNLOAD=READY, PRIVATE_STORAGE=READY |
| **Verification** | Overall readiness is DEGRADED (handoff disabled) or READY |
| **Rollback Trigger** | MISCONFIGURED |

### Step 9 — Enable Immediate Handoff

| Item | Detail |
|------|--------|
| **Prerequisite** | Steps 7+8 verified |
| **Action** | Set `imagegen.storage.immediate-handoff.enabled=true` |
| **Verification** | Overall readiness=READY, generation → handoff working |
| **Rollback Trigger** | Handoff failures > 3 in 5 min |

### Step 10 — Monitor Failure Rates

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 9 verified |
| **Action** | Monitor storage write/read/download/handoff metrics for 48+ hours |
| **Verification** | No failure spikes, no secret leaks |
| **Rollback Trigger** | Any alert threshold exceeded |

### Step 11 — Expand if Stable

| Item | Detail |
|------|--------|
| **Prerequisite** | Step 10 stable for 48+ hours |
| **Action** | Full traffic enablement |
| **Verification** | Continued stable metrics |
| **Rollback Trigger** | Degradation after expansion |

---

## 7. Smoke Test Plan

| # | Test | Verification |
|---|------|-------------|
| 1 | Config validation check | Returns valid/DISABLED or valid/READY per component |
| 2 | Readiness check | Returns overall DISABLED, READY, or DEGRADED as expected |
| 3 | Storage adapter enabled check | S3 adapter active when enabled, InMemory when disabled |
| 4 | Provider download enabled check | Real download client active when enabled, mock when disabled |
| 5 | Controlled generation test (staging) | Generate → asset created |
| 6 | Immediate handoff test (staging) | Generate → transient URL → download → validate → store |
| 7 | Preview proxy test | Preview returns bytes through backend, no redirect |
| 8 | DTO/API safety check | No storage key/provider URL/base64/secret in responses |
| 9 | Log/metric sensitivity check | No secrets/URLs/keys in application logs |
| 10 | Approval boundary check | Stored ≠ approved, no automatic approval |

---

## 8. Monitoring & Alert Checks

| # | Check | Alert Condition | Severity |
|---|-------|----------------|----------|
| 1 | Config validation failure | MISCONFIGURED post-deploy | CRITICAL |
| 2 | Readiness NOT_READY | Status != READY for > 5 min | HIGH |
| 3 | Provider download failure spike | > 3 failures in 5 min | HIGH |
| 4 | Storage write failure spike | > 3 failures in 5 min | HIGH |
| 5 | Storage read failure spike | > 3 failures in 5 min | HIGH |
| 6 | Immediate handoff failure spike | > 3 failures in 5 min | MEDIUM |
| 7 | Preview read failure spike | > 5 failures in 10 min | MEDIUM |
| 8 | Unexpected approval/publish | Should be impossible — regression tested | CRITICAL |

---

## 9. Rollback Plan

### Disable Order

| Priority | Action | Effect |
|----------|--------|--------|
| 1 | `imagegen.storage.immediate-handoff.enabled=false` | Generation continues without storage handoff |
| 2 | `imagegen.provider.download.enabled=false` | Mock retrieval fallback activates |
| 3 | `imagegen.storage.s3.enabled=false` | InMemory fallback activates (non-prod) |

### Rollback Rules

- ❌ Do NOT delete stored objects automatically
- ❌ Do NOT expose storage keys during rollback
- ✅ Keep preview backend-controlled
- ✅ Keep human review unchanged
- ✅ Already-stored assets remain accessible if storage adapter stays enabled
- ✅ Manual cleanup is operator-owned process only

---

## 10. Incident Response Notes

### Storage Write Failures
1. Check `failure_category` in safe logs (e.g., STORAGE_WRITE_FAILED)
2. Verify bucket exists and service account has PutObject permission
3. Do NOT search logs for storage keys or bucket paths — they should not be there
4. Rollback: disable immediate handoff first

### Provider Download Failures
1. Check `failure_category` (e.g., PROVIDER_OUTPUT_DOWNLOAD_FAILED)
2. Verify timeout/size limits are appropriate
3. Do NOT search logs for provider URLs — they should not be there
4. Rollback: disable provider download

### Preview Failures
1. Check if storage adapter is enabled and accessible
2. Verify InMemory fallback if S3 is disabled
3. Preview should always stream bytes — never redirect
4. Rollback: N/A — preview is always backend-controlled

### Config/Secret Failures
1. Check `[CONFIG_VALIDATION]` log entries for safe error codes
2. Verify secrets are present in secret manager
3. Do NOT share actual secret values in incident reports
4. Rollback: disable the failing component's flag

### Suspected Leak Response
1. **IMMEDIATE**: disable all flags
2. Rotate affected credentials
3. Audit logs for exposed values
4. Investigate root cause
5. Do NOT re-enable until root cause resolved

### Evidence Collection Rules
- ✅ Collect: safe error codes, component names, status values, correlation IDs, timestamps
- ❌ Never collect: provider URLs, storage keys, access keys, secret keys, bucket paths, raw payloads

---

## 11. Security Checklist

| # | Constraint | Required |
|---|-----------|----------|
| 1 | No provider URL persistence | ✅ |
| 2 | No provider URL in logs | ✅ |
| 3 | No storage key exposure/logging | ✅ |
| 4 | No public/signed URLs | ✅ |
| 5 | No secrets in logs/config/docs | ✅ |
| 6 | Bucket private-only | ✅ |
| 7 | Preview backend-controlled | ✅ |
| 8 | Human review required | ✅ |
| 9 | No automatic approval | ✅ |
| 10 | No publish/export | ✅ |

---

## 12. Runbook Acceptance Criteria

| # | Criterion | Status |
|---|----------|--------|
| 1 | Runbook exists | ✅ |
| 2 | Staging rollout steps documented (13 steps) | ✅ |
| 3 | Production rollout steps documented (11 steps) | ✅ |
| 4 | Rollback documented (3-priority disable) | ✅ |
| 5 | Smoke tests documented (10 tests) | ✅ |
| 6 | Monitoring checks documented (8 checks) | ✅ |
| 7 | Incident response documented | ✅ |
| 8 | Safety/security checklist documented | ✅ |
| 9 | No secrets/real config values included | ✅ |
