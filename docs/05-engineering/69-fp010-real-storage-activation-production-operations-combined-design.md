# FP-010 — Real Storage Activation & Production Storage Operations — Combined Design

## 1. Document Status

| Key | Value |
|-----|-------|
| **Feature** | FP-010 — Real Storage Activation & Production Storage Operations |
| **Document Type** | Combined Product / AI Platform / Engineering Design |
| **Status** | PLANNED |
| **Date** | 2026-07-05 |
| **Source Baseline** | FP-009 Final Closure: `docs/05-engineering/68-fp009-final-closure-operational-baseline.md` |
| **Test Baseline** | 1345/1345 passing |

Bu doküman yalnızca planlama/tasarım amaçlıdır. Production kodu, test, migration veya config değişikliği içermez.

---

## 2. FP-009 Baseline Summary

FP-009'dan devralınan operasyonel baseline:

- ✅ Transient provider URL extraction exists (`ProviderOutputReference`)
- ✅ Retrieval contracts exist (`ProviderOutputRetrievalClient`, request/result models)
- ✅ Validation-first orchestration exists (`ProviderOutputRetrievalValidationService`)
- ✅ V12 storage reference columns exist (`storage_provider`, `storage_bucket`, `storage_object_key`)
- ✅ S3/private storage adapter shell exists (`S3ImageStorageAdapter`, `@ConditionalOnProperty` disabled)
- ✅ Controlled storage handoff endpoint exists (`POST /generated-assets/{id}/storage-handoff`)
- ✅ Preview proxy remains backend-controlled (`ImagePreviewProxyService`)
- ✅ DTO/audit/failure hardening exists
- ⏳ Real S3 write activation — deferred
- ⏳ Production credentials / secret management — deferred
- ⏳ Real HTTP download client — deferred (`MockProviderOutputRetrievalClient` active)

---

## 3. Product Objective

FP-010'un amacı:

- Storage adapter shell'den kontrollü gerçek private storage aktivasyonuna geçiş.
- Valide edilmiş generated image output'larının gerçek private storage'a yazılması.
- Preview'un yalnızca backend-controlled proxy üzerinden kalması.
- Admin deneyiminin safe ve status-based kalması.
- Human review'un zorunlu kalması.
- Publish/export'un kapsam dışı kalması.

---

## 4. User / Admin Value

| Value | Description |
|-------|-------------|
| Durable storage | Generated image'lar transient provider output'tan kalıcı private storage'a taşınır |
| Safe admin visibility | Admin storage readiness'ı güvenli status'lar üzerinden görür |
| Preview continuity | Admin stored output'u backend proxy ile preview eder |
| Safety preserved | Admin asla provider URL, storage key, public/signed URL, base64, raw payload veya secret görmez |
| Failure visibility | Hatalar safe category/message olarak görünür |
| Production readiness | Sistem public delivery açılmadan production'a yaklaşır |

---

## 5. In Scope

- Real private storage write activation planning
- Real private storage read readiness for preview proxy
- Production-safe storage configuration model
- Secret management boundary design
- S3/private storage adapter activation strategy
- Storage write/read failure handling
- Real HTTP provider output download client planning
- End-to-end private storage operation tests planning
- Operational readiness checklist
- Safe observability/metrics/logging
- Safe retry policy planning
- Admin-safe status/failure visibility
- Backend-controlled preview preservation
- Human review preservation

---

## 6. Out of Scope

| Item | Status |
|------|--------|
| Public URL generation | ❌ Excluded |
| Signed URL returned to admin/user | ❌ Excluded |
| CDN/public delivery | ❌ Excluded |
| User-facing media library | ❌ Excluded |
| Publish/export | ❌ Excluded |
| Video/audio | ❌ Excluded |
| Automatic approval | ❌ Excluded |
| READY_FOR_PUBLISH | ❌ Excluded |
| COMPLETED | ❌ Excluded |
| Generic status update endpoint | ❌ Excluded |
| Provider URL persistence | ❌ Excluded |
| Provider URL DTO/API exposure | ❌ Excluded |
| Provider URL logging | ❌ Excluded |
| Storage key admin/user exposure | ❌ Excluded |
| Storage key logging | ❌ Excluded |
| Base64 DB/DTO/log/API response | ❌ Excluded |
| Raw provider/storage payload exposure | ❌ Excluded |
| Secret/API key/config exposure | ❌ Excluded |
| Hardcoded credentials | ❌ Excluded |
| Committed secrets | ❌ Excluded |
| Destructive migration/backfill | ❌ Excluded |
| Broad multi-provider redesign | ❌ Excluded |

---

## 7. Product Lifecycle

```
Provider Generation → Transient Output URL
       ↓
Backend Internal Flow → Receives ProviderOutputReference
       ↓
Real HTTP Download Client → Retrieves bytes internally
       ↓
Media Validation → Validates before storage
       ↓
Real Private Storage Adapter → Writes validated bytes
       ↓
Internal Storage Reference → Persisted (key/bucket/provider)
       ↓
Safe Status Update → storageHandoffStatus, previewStatus
       ↓
Admin → Sees safe status/failure only
       ↓
Preview Proxy → Backend-controlled byte streaming
       ↓
Human Review → Separate, required
       ↓
No publish/export
```

**Explicit state boundaries:**

- Retrieved ≠ validated
- Validated ≠ stored
- Stored ≠ approved
- Preview available ≠ approved

---

## 8. AI Platform / Media Contract

| Contract | Rule |
|----------|------|
| Provider output URL | Transient backend-only execution data |
| Provider URL persistence | **NEVER** — not persisted, logged, or exposed |
| Downloaded bytes | Backend-internal → directly to validation |
| Base64 | **NEVER** in DB/DTO/log/API response |
| Validation gate | Must pass before storage success |
| Storage adapter return | Backend-internal storage reference only |
| Public URL from storage | **NEVER** generated |
| Signed URL to admin/user | **NEVER** returned |
| Preview delivery | Backend-controlled proxy only |
| Raw payloads | **NEVER** exposed |

---

## 9. Storage Provider Contract

| Rule | Detail |
|------|--------|
| Storage type | Private only |
| Object key/reference | Internal only — never admin/user-facing |
| Bucket/container | Internal unless explicitly safe |
| Public-read objects | **NEVER** |
| Public URL generation | **NEVER** |
| Signed URL to admin/user | **NEVER** |
| Raw storage response | **NEVER** exposed |
| Config/secrets | Server-side only |
| Adapter operations | Write + read for backend services |
| Failure handling | Sanitized — safe category/message only |
| Unit testability | Mocked — no real external network |
| Integration tests | Opt-in, disabled by default |

---

## 10. Secret / Config Strategy

| Rule | Detail |
|------|--------|
| Hardcoded credentials | **NEVER** |
| Committed secrets | **NEVER** |
| Credential source | Environment / secret manager / runtime config only |
| toString/logging | Secret fields excluded (`@ToString.Exclude`) |
| Config validation | Fail safely on missing/invalid |
| Missing config error | `INTERNAL_CONFIGURATION_UNAVAILABLE` |
| Test values | Fake values only — never real |
| Production enablement | Explicit environment configuration required |
| Default behavior | Safe/disabled unless configured |

---

## 11. Real Provider Download Client Strategy

| Rule | Detail |
|------|--------|
| Provider URL | Transient method parameter only |
| Persistence | **NEVER** |
| Logging | **NEVER** |
| DTO/API exposure | **NEVER** |
| Timeouts | Required — connect + read |
| Size limits | Required — max download size |
| Content type validation | Required — image types only |
| Redirect handling | Safe and bounded (max redirects) |
| Error messages | Sanitized — no URL/header in error |
| Downloaded bytes | Go to validation service |
| Base64 | **NEVER** |
| Raw HTTP response | **NEVER** exposed |
| Auth/token/header | **NEVER** exposed |

---

## 12. Backend Component Plan

### 12.1 HttpProviderOutputRetrievalClient

| Aspect | Detail |
|--------|--------|
| Responsibility | Real HTTP GET to transient provider URL, return bytes |
| Inputs | Provider URL (transient), correlation ID |
| Outputs | `ProviderOutputRetrievalResult` (bytes + metadata) |
| Forbidden | URL persistence/logging/exposure, raw HTTP response, auth headers |
| Tests | Mocked HTTP client; timeout, oversize, bad MIME, redirect tests |

### 12.2 S3ImageStorageAdapter Activation

| Aspect | Detail |
|--------|--------|
| Responsibility | Real S3 write/read via AWS SDK |
| Inputs | `ImageStorageWriteRequest` (bytes, metadata) / `ImageStorageReadRequest` (key) |
| Outputs | `ImageStorageWriteResult` / `ImageStorageReadResult` |
| Forbidden | Public URL, signed URL to admin, storage key exposure, raw SDK response |
| Tests | Mocked S3 client; write success/failure, read success/failure, config missing |

### 12.3 S3PrivateStorageProperties Validation

| Aspect | Detail |
|--------|--------|
| Responsibility | Validate S3 config at startup, fail safely |
| Inputs | Application properties |
| Outputs | Valid config or safe failure |
| Forbidden | Secret logging, config value exposure |
| Tests | Missing bucket, missing region, missing credentials → safe error |

### 12.4 StorageHandoffService / Orchestration Enhancement

| Aspect | Detail |
|--------|--------|
| Responsibility | Route to real adapter when enabled |
| Inputs | Validated bytes from retrieval-validation pipeline |
| Outputs | `ImageStorageHandoffResult` |
| Forbidden | Provider URL, storage key exposure, automatic approval |
| Tests | Real adapter mock wiring, fallback to mock |

### 12.5 Preview Proxy Read-Path Compatibility

| Aspect | Detail |
|--------|--------|
| Responsibility | Read from real storage via adapter for preview |
| Inputs | Storage object key (internal) |
| Outputs | Image bytes for streaming |
| Forbidden | Public/signed URL, storage key in response, redirect |
| Tests | Real adapter mock read, preview success/failure |

### 12.6 Safe Failure Mapping

| Aspect | Detail |
|--------|--------|
| Responsibility | Map SDK/HTTP errors to safe error codes |
| Inputs | Raw exception/error from SDK/HTTP |
| Outputs | `ImageStorageErrorCode` + safe message |
| Forbidden | Raw exception text, URL/key/secret in message |
| Tests | SDK exception → safe code mapping |

### 12.7 DTO Mapper Regression Protection

| Aspect | Detail |
|--------|--------|
| Responsibility | Ensure no new forbidden fields leak into DTOs |
| Tests | Existing `DtoAuditFailureMessageHardeningTest` extended |

### 12.8 Logging / Audit / Metrics Hooks

| Aspect | Detail |
|--------|--------|
| Responsibility | Safe operational observability |
| Allowed | Asset ID, provider alias, storage alias, failure category, correlation ID |
| Forbidden | Provider URL, storage key, bucket path, signed URL, base64, raw payload, secrets |

---

## 13. Endpoint / API Strategy

| Rule | Detail |
|------|--------|
| Existing handoff endpoint | Remains command-specific |
| Generic status update | Does not exist |
| Arbitrary status transitions | Not allowed |
| Approval endpoint | No changes |
| Publish/export endpoint | Does not exist |
| Response format | Safe DTO only |
| Admin sends provider URL | **NEVER** |
| Backend generation flow | May invoke immediate handoff with transient URL |
| Preview endpoint | Remains backend-controlled byte streaming |

---

## 14. Persistence / Schema Strategy

| Rule | Detail |
|------|--------|
| New migration | Prefer none — use V12 fields from FP-009 |
| V12 fields | `storage_provider`, `storage_bucket`, `storage_object_key` |
| Provider URL column | **NEVER** add |
| Signed URL column | **NEVER** add |
| Public URL column | **NEVER** add |
| Base64 column | **NEVER** add |
| Raw payload columns | **NEVER** add |
| Secret/config columns | **NEVER** add |
| Additional operational fields | Defer to separate migration task with explicit safety criteria |

---

## 15. Failure / Error Model

| Error Code | Description | Retryable |
|------------|-------------|-----------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider URL null/blank/missing | No |
| `PROVIDER_OUTPUT_EXPIRED` | Provider URL expired/invalid | No |
| `PROVIDER_OUTPUT_DOWNLOAD_FAILED` | HTTP download failure | Maybe |
| `UNSUPPORTED_MEDIA_TYPE` | Not image/png, image/jpeg, image/webp | No |
| `FILE_TOO_LARGE` | Exceeds size limit | No |
| `VALIDATION_FAILED` | Validation check failed | No |
| `STORAGE_WRITE_FAILED` | Storage write operation failed | Maybe |
| `STORAGE_READ_FAILED` | Storage read operation failed | Maybe |
| `PREVIEW_UNAVAILABLE` | Preview not available | No |
| `INTERNAL_CONFIGURATION_UNAVAILABLE` | Storage config missing/invalid | No |

**Rules:**
- Admin gets safe category/message only
- Internal details never cross DTO/API boundary
- No URL/key/secret/raw payload/stack trace in admin response
- Retryable flag is internal/safe

---

## 16. Observability / Audit / Logging

### Safe Metrics

- Retrieval attempts / failures by category
- Validation failures by category
- Storage write attempts / failures
- Preview read failures
- Download latency / storage latency

### Safe Log Fields

- Asset ID, generated asset ID
- Provider alias, storage provider alias
- Safe failure category
- Correlation ID
- Operation phase (retrieval, validation, storage, preview)

### Forbidden Log Fields

- Provider URL
- Storage object key
- Bucket path (if sensitive)
- Signed/public URL
- Base64
- Raw provider/storage payload
- Access key / secret / token / config value

---

## 17. Testing Strategy

| Test Area | Approach |
|-----------|----------|
| Real download client | Mocked HTTP; timeout, oversize, bad MIME, redirect, header redaction |
| S3 adapter | Mocked S3 client; write/read success/failure, config missing |
| Config validation | Missing bucket/region/credentials → safe error |
| End-to-end pipeline | Mocked retrieval → validation → storage → preview with real adapter mock |
| DTO regression | Extend `DtoAuditFailureMessageHardeningTest` |
| Failure sanitization | SDK/HTTP exception → safe message |
| Approval/publish regression | No side effects |
| Full suite | `mvn test` — preserve baseline |

No real external provider/storage calls in unit tests. Integration tests opt-in and disabled by default.

---

## 18. Proposed Dense Implementation Task Sequence

### Task 02 — Real Provider Download Client + Storage Config Hardening

- Real HTTP provider output download client (`HttpProviderOutputRetrievalClient`)
- Timeout, size limit, content-type safety
- S3 config validation hardening
- No endpoint changes, no real external calls in tests

### Task 03 — Real Private Storage Adapter Activation

- Activate `S3ImageStorageAdapter` behind config
- Implement write/read with mocked S3 client
- Preview proxy read-path compatibility
- No public/signed URL, no storage key exposure

### Task 04 — End-to-End Immediate Handoff Wiring

- Wire generation flow: transient `ProviderOutputReference` → download → validation → real private storage → safe status update
- Admin endpoint remains command-specific
- No provider URL persistence/exposure/logging
- No automatic approval

### Task 05 — Operational Regression, Observability & Closure Candidate

- End-to-end regression tests
- Safe metrics/logging hooks
- Operational readiness documentation
- Full test baseline
- Closure candidate

### Task 06 — Final Closure / Operational Baseline

- Documentation-only final closure (if Task 05 accepted)

---

## 19. Safety Checklist (Every FP-010 Coding Task)

- [ ] No provider URL persistence
- [ ] No provider URL DTO/API exposure
- [ ] No provider URL logging
- [ ] No storage key admin/user exposure
- [ ] No storage key logging
- [ ] No public URL generation
- [ ] No signed URL returned to admin/user
- [ ] No base64 DB/DTO/log/API response
- [ ] No raw provider/storage payload exposure
- [ ] No secret/API key/config exposure
- [ ] No stack trace in admin/API response
- [ ] No hardcoded credentials
- [ ] No committed secrets
- [ ] No real external provider/storage call in unit tests
- [ ] Preview backend-controlled
- [ ] Human review unchanged
- [ ] No automatic approval
- [ ] No READY_FOR_PUBLISH
- [ ] No COMPLETED
- [ ] No generic status update
- [ ] No publish/export
- [ ] No video/audio

---

## 20. Acceptance Criteria

- [x] Combined design document exists
- [x] FP-009 final closure baseline referenced
- [x] FP-010 product objective defined
- [x] AI/media/storage contracts defined
- [x] Real storage activation strategy defined
- [x] Secret/config strategy defined
- [x] Real provider download strategy defined
- [x] Endpoint/API strategy preserves command-specific safe behavior
- [x] Persistence strategy avoids unsafe columns
- [x] Failure/error model defined
- [x] Observability/logging safety defined
- [x] Dense implementation task sequence defined (5 tasks)
- [x] Safety checklist included
- [x] No code/config/migration/test changes made
- [x] FP-010 remains PLANNED

---

## 21. Next Step

**FP-010 Task 02 — Real Provider Download Client + Storage Config Hardening**

Task 02, FP-010'un ilk implementation task'ıdır. FP-009 micro-task'larından daha yoğun olacaktır ancak tüm safety boundary'leri koruyacaktır.
