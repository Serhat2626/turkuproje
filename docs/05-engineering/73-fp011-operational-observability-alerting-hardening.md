# FP-011 — Operational Observability & Alerting Hardening

## 1. Document Status

| Field | Value |
|-------|-------|
| **Feature** | FP-011 — Production Storage Enablement & Runtime Operations |
| **Document Type** | Operational Observability & Alerting Hardening |
| **Task** | FP-011 Task 04 |
| **Status** | ACTIVE |
| **Source** | [72-fp011-production-storage-enablement-runtime-operations-combined-design.md](72-fp011-production-storage-enablement-runtime-operations-combined-design.md) |
| **Created** | 2026-07-05 |

---

## 2. Safe Metric Suggestions

### Recommended Metrics

| Metric Name | Type | Description |
|------------|------|-------------|
| `image_storage_config_validation_total` | Counter | Config validation runs |
| `image_storage_readiness_status` | Gauge | Current readiness status per component |
| `image_storage_provider_download_failures_total` | Counter | Provider download failures |
| `image_storage_write_failures_total` | Counter | Storage write failures |
| `image_storage_read_failures_total` | Counter | Storage read failures |
| `image_storage_immediate_handoff_failures_total` | Counter | Immediate handoff failures |
| `image_storage_preview_read_failures_total` | Counter | Preview read failures |

### Safe Metric Labels

| Label | Example Values | Safe |
|-------|---------------|------|
| `component` | PROVIDER_DOWNLOAD, PRIVATE_STORAGE, IMMEDIATE_HANDOFF | ✅ |
| `status` | READY, DISABLED, NOT_READY, MISCONFIGURED, DEGRADED | ✅ |
| `severity` | INFO, WARNING, ERROR | ✅ |
| `failure_category` | STORAGE_WRITE_FAILED, PROVIDER_OUTPUT_DOWNLOAD_FAILED | ✅ |
| `phase` | config_validation, readiness_check, storage_write, preview_read | ✅ |
| `provider_alias` | OPENAI, MOCK | ✅ |
| `storage_provider_alias` | S3, mock | ✅ |

### Forbidden Metric Labels

| Label | Reason |
|-------|--------|
| `provider_url` | Transient secret — never persisted/logged |
| `storage_key` / `object_key` | Internal-only |
| `bucket` | Internal infrastructure |
| `access_key` / `secret_key` / `token` | Credential — never exposed |
| `raw_error` | May contain sensitive data |
| `base64` / `payload` | Raw content — never in metrics |
| `endpoint` | Internal infrastructure URL |
| `raw_config_value` | May contain embedded secrets |

---

## 3. Safe Log Fields

### Allowed Log Fields

| Field | Example | Safe |
|-------|---------|------|
| `component` | PROVIDER_DOWNLOAD | ✅ |
| `readiness` | READY | ✅ |
| `severity` | ERROR | ✅ |
| `safeErrorCode` | STORAGE_CONFIGURATION_INVALID | ✅ |
| `safeMessage` | "Private storage configuration is incomplete" | ✅ |
| `phase` | config_validation | ✅ |
| `correlationId` | UUID | ✅ |
| `generatedAssetId` | UUID | ✅ |
| `mediaAssetId` | UUID | ✅ |

### Forbidden Log Fields

| Field | Reason |
|-------|--------|
| Provider URL | Transient — never logged |
| Storage object key | Internal-only |
| Bucket name/path | Internal infrastructure |
| Access key / secret key / token | Credential |
| Raw exception message | May contain URLs/keys/secrets |
| Stack trace (in admin/API response) | Internal detail |
| Base64 / raw payload | Content data |
| Raw config dump | May embed secrets |

---

## 4. Alert Candidates

| # | Alert | Condition | Severity |
|---|-------|-----------|----------|
| 1 | Config validation failure after deployment | Any MISCONFIGURED status post-deploy | CRITICAL |
| 2 | Readiness NOT_READY in production | Readiness != READY for > 5 min after deploy | HIGH |
| 3 | Storage write failure spike | > 3 write failures in 5 minutes | HIGH |
| 4 | Storage read failure spike | > 3 read failures in 5 minutes | HIGH |
| 5 | Provider download failure spike | > 3 download failures in 5 minutes | HIGH |
| 6 | Immediate handoff failure spike | > 3 handoff failures in 5 minutes | MEDIUM |
| 7 | Preview read failure spike | > 5 preview failures in 10 minutes | MEDIUM |

### Alert Content Rules

- ✅ Include: component, status, failure_category, severity, correlation ID
- ❌ Never include: provider URL, storage key, bucket, access key, secret key, raw error, base64

---

## 5. Triage Steps

### Config Validation Failure
1. Check which component reported MISCONFIGURED or NOT_READY (safe log fields only)
2. Verify environment variables are set for the reported component
3. Do NOT inspect raw config values in logs — they should not be there
4. If credentials missing: check secret manager provisioning
5. If bucket missing: check infrastructure provisioning
6. Rollback: disable the failing component's feature flag

### Storage Write Failure
1. Check failure_category (e.g., STORAGE_WRITE_FAILED)
2. Verify storage adapter is enabled and bucket is provisioned
3. Verify service account permissions (PutObject)
4. Do NOT search logs for storage keys or bucket paths
5. Rollback: disable immediate handoff first, then storage adapter

### Provider Download Failure
1. Check failure_category (e.g., PROVIDER_OUTPUT_DOWNLOAD_FAILED)
2. Verify provider download is enabled
3. Check timeout/size limits against provider response times
4. Do NOT search logs for provider URLs
5. Rollback: disable provider download

---

## 6. Rollback Triggers

| Trigger | Action |
|---------|--------|
| Readiness MISCONFIGURED after deploy | Disable failing component flag |
| > 10 storage write failures in 10 min | Disable immediate handoff |
| > 10 download failures in 10 min | Disable provider download |
| Any secret/URL/key found in logs | IMMEDIATE: disable all flags + incident |
| Preview failures after storage change | Disable immediate handoff, verify InMemory fallback |

---

## 7. Dashboard Safety Rules

### What Must NEVER Appear in Dashboards/Logs/Alerts

- ❌ Provider URLs (transient, never persisted)
- ❌ Storage object keys (internal-only)
- ❌ Bucket names/paths (internal infrastructure)
- ❌ Access keys / secret keys / tokens
- ❌ Raw exception messages (may contain sensitive data)
- ❌ Raw config dumps
- ❌ Base64 / raw binary payload
- ❌ Stack traces (in admin/API responses or alert bodies)
- ❌ Signed/public URLs (do not exist, must not be created)

---

## 8. Out of Scope

- CDN/public delivery
- Signed URL delivery
- Publish/export monitoring
- Video/audio monitoring
- User-facing media library metrics
- Automatic approval alerts
- READY_FOR_PUBLISH status monitoring
- COMPLETED status monitoring
