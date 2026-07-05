# FP-009 — Provider Output Retrieval & Real Storage Provider Integration — AI Platform / Media Contract Design

---

## 1. Document Status

| Item | Detail |
|------|--------|
| **Feature** | FP-009 — Provider Output Retrieval & Real Storage Provider Integration |
| **Document type** | AI Platform / Media Contract Design |
| **Status** | **PLANNED** |
| **Task** | FP-009 Task 02 — AI Platform / Media Contract Design |
| **Task scope** | Documentation-only |
| **Product Design** | [63-fp009-provider-output-retrieval-real-storage-product-design.md](../03-product-design/63-fp009-provider-output-retrieval-real-storage-product-design.md) |
| **FP-008 Closure Doc** | [62-fp008-final-closure-operational-baseline.md](../05-engineering/62-fp008-final-closure-operational-baseline.md) |
| **FP-008 AI Platform Doc** | [60-fp008-safe-storage-handoff-preview-proxy-ai-platform-media-contract-design.md](60-fp008-safe-storage-handoff-preview-proxy-ai-platform-media-contract-design.md) |
| **Source baseline** | FP-008 — CLOSED (commit `a36eba8`, 217/217 tests passing) |
| **Closed predecessor chain** | FP-001 → FP-002 → FP-003 → FP-004 → FP-005 → FP-006 → FP-007 → FP-008 |

> **This is a documentation-only task.** No code, configuration, migration, test, DTO, enum, entity, controller, service, or runtime changes are made. No provider SDK integration. No real storage provider code. No endpoint changes.

---

## 2. AI Platform Purpose

### Contract Role in FP-009

This document defines the safe AI platform and media contract boundaries for FP-009.

**FP-007** had metadata-only real provider integration:

- Provider generated image results; metadata was captured.
- Provider output (actual image bytes) was not downloaded, stored, or previewed.
- Human review gate was established.

**FP-008** added safe validation, private/mock storage handoff, and backend preview proxy:

- `ImageMediaValidationService` — MIME sniffing, size validation, dimension extraction, SHA-256 checksum.
- `ImageStorageAdapter` + `InMemoryImageStorageAdapter` — provider-agnostic storage boundary with mock implementation.
- `ImageStorageHandoffService` — validate → store → metadata update flow.
- Backend-controlled preview proxy — image bytes streaming, no URL exposure.
- `ImageStorageErrorNormalizer` — safe error normalization and sanitization.
- Storage handoff endpoint contract — intentionally 501 (provider output retrieval deferred).

**FP-009** introduces:

- Controlled provider output retrieval — safely downloading provider-generated image bytes in the backend.
- Controlled real private storage integration — replacing mock storage with a real object storage provider.
- Storage handoff endpoint activation — transitioning from 501 to real handoff.

### What This Contract Defines

The AI platform must define safe boundaries between:

| Boundary | Purpose |
|----------|---------|
| Provider output reference | What metadata can be used, what must remain transient |
| Backend retrieval | How provider output is downloaded safely |
| Media validation | What checks are required before storage |
| Private storage | How validated bytes are stored and what is exposed |
| Preview proxy | How admins see stored images safely |
| Admin API | What admin-facing surfaces may and may not contain |
| Human review | How review gate is preserved across all boundaries |

### What This Contract Does Not Introduce

- Publish/export contracts
- READY_FOR_PUBLISH status
- COMPLETED status
- Generic status update contracts
- Public/signed URL generation contracts
- Automatic approval contracts
- Video/audio contracts

---

## 3. Provider Output Reference Contract

### What Is a Provider Output Reference

A provider output reference is internal data that identifies or locates a generated image output from the AI provider. It is used by the backend to retrieve image bytes after generation.

### Safe Persisted Provider Metadata

The following concepts may be safely persisted and are already part of the FP-007 baseline or are safe extensions:

| Concept | Explanation | Persistence | DTO Visibility |
|---------|-------------|-------------|----------------|
| Provider type/name | Which provider generated the output (e.g., "openai", "mock") | ✅ Safe to persist | ✅ Safe if already in FP-007 contract |
| Provider generation ID / output ID | Provider-side identifier for the generation request, if already safe in FP-007 contract | ✅ Safe to persist (internal) | ⚠️ Internal only — not admin-facing unless already exposed |
| Provider status category | Normalized status of the generation (SUCCESS, FAILED, etc.) | ✅ Safe to persist | ✅ Safe |
| Generated media asset ID relation | Link between generation result and media asset | ✅ Safe to persist | ✅ Safe |
| Retrieval attempt metadata | Timestamps, attempt count, safe failure category — without URLs/secrets/raw payloads | ✅ Safe to persist | ✅ Safe (status/timestamps only) |
| Safe failure category | Normalized error category for retrieval failures | ✅ Safe to persist | ✅ Safe |

### Internal Transient Provider Retrieval Data

The following data may exist transiently during backend retrieval execution but must not be persisted, exposed, logged, or returned in DTOs:

| Concept | Explanation | Persistence | DTO Visibility | Logging |
|---------|-------------|-------------|----------------|---------|
| Provider download URL | Temporary URL from which image bytes are downloaded | ⛔ Never persisted | ⛔ Never | ⛔ Never |
| Temporary signed provider URL | Short-lived access URL provided by the provider | ⛔ Never persisted | ⛔ Never | ⛔ Never |
| Provider response headers | HTTP headers from the provider download response | ⛔ Never persisted | ⛔ Never | ⛔ Never |
| Provider raw response body | The raw HTTP response or API response | ⛔ Never persisted | ⛔ Never | ⛔ Never |
| Provider auth token / bearer | Authentication credentials used for download | ⛔ Never persisted | ⛔ Never | ⛔ Never |
| Provider content-type hint | Content-Type from provider response — used transiently for pre-check, then discarded | ⛔ Not persisted as provider data | ⛔ Never | ⛔ Never |
| Provider content-length hint | Content-Length from provider response — used transiently for pre-check | ⛔ Not persisted as provider data | ⛔ Never | ⛔ Never |

### Forbidden Persisted/Exposed Concepts

The following must **never** be persisted, returned in DTOs, logged, or exposed to admin/user:

| Concept | Reason |
|---------|--------|
| Provider download URL | URL lifecycle risk, security |
| Temporary signed provider URL | URL lifecycle risk, security |
| Raw response JSON from provider | Provider security |
| Request headers to provider | Provider security |
| Auth tokens / bearer tokens | Secret management |
| API keys | Secret management |
| Secret config values | Secret management |
| Base64 image body | Performance and security |
| Provider binary payload in DB | Performance and security |

### Provider Output Reference Rules

| Rule | Constraint |
|------|-----------|
| Provider URL must not be persisted | ⛔ Mandatory |
| Provider URL must not be returned in DTOs | ⛔ Mandatory |
| Provider URL must not be logged | ⛔ Mandatory |
| Provider URL must not be exposed to admin/user | ⛔ Mandatory |
| Raw provider response must not be exposed | ⛔ Mandatory |
| Secrets/API keys/config values must not be exposed | ⛔ Mandatory |
| Provider-specific retrieval details must remain internal | ⛔ Mandatory |

---

## 4. Retrieval Contract

### Conceptual Retrieval Flow

The controlled retrieval contract defines how provider output is downloaded safely:

```
┌──────────────────────────────────────────────────────────────────┐
│  BACKEND RETRIEVAL (server-side only)                            │
│                                                                  │
│  1. Backend receives/discovers provider output reference.        │
│  2. Backend resolves provider output internally                  │
│     (using internal config/credentials).                         │
│  3. Backend may use transient provider URL or provider SDK       │
│     internally if required.                                      │
│  4. Backend downloads image bytes internally.                    │
│  5. Backend does NOT persist provider URL.                       │
│  6. Backend does NOT expose provider URL.                        │
│  7. Backend does NOT log provider URL.                           │
│  8. Backend does NOT expose raw provider payload.                │
│  9. Backend passes bytes/stream to validation.                   │
│ 10. Backend only continues to storage if validation succeeds.    │
│ 11. Provider URL reference is discarded after retrieval.         │
└──────────────────────────────────────────────────────────────────┘
```

### Retrieval Boundaries

| Boundary | Rule |
|----------|------|
| Backend-only | Retrieval is performed exclusively by the backend; no frontend or admin-initiated direct download |
| Image scope only | Retrieval is explicitly scoped to image outputs — no video, no audio |
| No bulk retrieval | Single-item controlled retrieval only |
| No user-facing retrieval endpoint | Admin/user cannot trigger a direct download from provider |
| No public redirect to provider | Backend does not redirect clients to provider URL |
| No frontend direct download | Frontend never downloads from provider directly |
| No provider URL passthrough | Provider URL is never forwarded, returned, or proxied to admin/user |
| No provider URL caching | Provider URL is not cached — transient use only |

### Retrieval Request Conceptual Fields

| Field | Description | Visibility |
|-------|-------------|------------|
| `generatedAssetId` | Target generated asset for retrieval | Safe |
| `mediaAssetId` | Target media asset for retrieval | Safe |
| `providerOutputReference` | Internal provider output locator | **Internal-only — never admin-facing** |
| `correlationId` | End-to-end trace identifier | Safe |
| `requestedAt` | Retrieval request timestamp | Safe |
| `requestedBy` | System actor or admin identifier | Safe |

### Retrieval Result Conceptual Fields

| Field | Description | Visibility |
|-------|-------------|------------|
| `retrievalStatus` | Success/failure category | Safe |
| `retrievedBytes` | Downloaded image bytes/stream | **Internal-only — passed to validation, never to DTO** |
| `retrievedContentType` | Detected content type from download | **Internal-only — validation input** |
| `retrievedContentLength` | Byte count of downloaded content | **Internal-only — validation input** |
| `safeFailureCategory` | Normalized failure category if failed | Safe |
| `safeErrorMessage` | Safe product-level error message | Safe |
| `correlationId` | End-to-end trace identifier | Safe |
| `retrievedAt` | Retrieval completion timestamp | Safe |

### Retrieval Result Must Not Contain

- Provider URL
- Provider response headers
- Provider raw response body
- Auth tokens / API keys
- Base64 image data
- Raw provider SDK error details

---

## 5. Media Validation Contract

### Validation Purpose

Validation ensures that only safe, valid image content enters private storage. Validation must happen **before** storage is considered successful.

### Validation Checks

| Check | Description | Requirement |
|-------|-------------|-------------|
| Content type validation | Verify content is an allowed image type | Mandatory |
| Image-only scope | Only `image/png`, `image/jpeg`, `image/webp` allowed | Mandatory |
| Content sniffing | Validate actual media type via magic bytes — do not trust provider Content-Type alone | Mandatory |
| File size limit | Reject files exceeding the configured maximum size | Mandatory |
| Zero-byte rejection | Reject empty files | Mandatory |
| Byte integrity / readability | Basic check that image bytes are readable/valid | Mandatory |
| Corrupt image rejection | Reject images that cannot be decoded or have invalid structure | Mandatory |
| Dimension extraction | Extract width/height from valid image | Mandatory |
| Checksum calculation | Calculate SHA-256 hash for integrity tracking | Mandatory |

> **Note:** The exact file size limit is defined in FP-008 validation baseline as **10 MB**. If this limit needs adjustment, it must be defined or confirmed in Engineering Design. The contract does not change the existing limit.

### Validation Result Conceptual Fields

| Field | Description | Visibility |
|-------|-------------|------------|
| `validationStatus` | PASSED / FAILED | Safe |
| `detectedMimeType` | Actual MIME type from content sniffing | Safe |
| `fileSizeBytes` | Actual file size in bytes | Safe |
| `width` | Image width in pixels | Safe |
| `height` | Image height in pixels | Safe |
| `checksumSha256` | SHA-256 hash (64-char hex) | Safe |
| `validationErrors` | List of safe validation error categories | Safe |
| `normalizedErrorCode` | Normalized failure code | Safe |
| `safeErrorMessage` | Safe product-level error description | Safe |
| `validatedAt` | Validation completion timestamp | Safe |

### Validation Failure Rules

| Rule | Constraint |
|------|-----------|
| Validation failure must be represented as safe failure category | No raw detail leakage |
| Validation failure must not expose raw bytes | ⛔ Mandatory |
| Validation failure must not expose base64 | ⛔ Mandatory |
| Validation failure must not expose provider payload | ⛔ Mandatory |
| Validation logs must not include sensitive URLs/tokens/base64 | ⛔ Mandatory |
| Validation failure does not auto-reject the asset | Human review gate preserved |
| Validation failure does not auto-approve the asset | Human review gate preserved |
| Validation failure sets storage handoff to failure status | Storage status update only |

---

## 6. Private Storage Contract

### Storage Purpose

The private storage contract defines how validated image bytes are written to real object storage and what boundaries must be maintained.

### Storage Rules

| Rule | Constraint |
|------|-----------|
| Storage must be private by default | ⛔ Mandatory — no public access |
| Stored object key may exist internally only | Internal backend reference |
| Storage object key must not be admin-facing | ⛔ Mandatory |
| Storage object key must not be user-facing | ⛔ Mandatory |
| Public URL generation is prohibited | ⛔ Mandatory |
| Signed URL returned to admin/user is prohibited | ⛔ Mandatory |
| Raw storage provider response must not be exposed | ⛔ Mandatory |
| Storage provider credentials/config must not be exposed | ⛔ Mandatory |
| Storage success can update safe storage/preview state only | Safe metadata allowlist |
| Preview must continue through backend-controlled preview proxy | ⛔ Mandatory |
| Storage provider implementation is a future engineering/code task | Not implemented here |

### Safe Persisted Storage Concepts

The following concepts may be safely persisted as part of the storage result:

| Concept | Description | Persistence | Admin DTO |
|---------|-------------|-------------|-----------|
| Storage provider type/category | Which storage backend was used (e.g., "s3", "minio", "inmemory") | ✅ Safe (internal) | ⛔ Internal only |
| Internal storage object reference/key | Bucket-relative object path | ✅ Safe (internal) | ⛔ Internal only |
| Storage bucket/container name | Where the object is stored | ✅ Safe (internal) | ⛔ Internal only |
| Storage status | STORAGE_PENDING / STORED / FAILED | ✅ Safe | ✅ Safe |
| Media content type | Validated MIME type | ✅ Safe | ✅ Safe |
| Media size | Validated file size in bytes | ✅ Safe | ✅ Safe |
| Media dimensions | Validated width/height | ✅ Safe | ✅ Safe |
| Media checksum | SHA-256 hash | ✅ Safe | ✅ Safe |
| Validation status | Whether media passed validation | ✅ Safe | ✅ Safe |
| Preview availability | Whether preview can be served | ✅ Safe | ✅ Safe |
| Stored timestamp | When storage completed | ✅ Safe | ✅ Safe |

### Forbidden Exposed Storage Concepts

The following must never appear in admin/user-facing DTOs, response headers, or error messages:

| Concept | Reason |
|---------|--------|
| Object key in admin/user DTOs | Internal infrastructure detail |
| Bucket/container name in DTOs | Internal infrastructure detail |
| Raw storage SDK response | Infrastructure security |
| Signed URL | Security — backend-controlled preview only |
| Public URL | Security — private storage only |
| Storage credentials | Secret management |
| Storage region/account identifiers | Infrastructure security |
| Storage request/response headers | Infrastructure security |
| Storage auth metadata | Secret management |

---

## 7. Preview Contract

### Preview After Real Storage

The existing FP-008 backend-controlled preview proxy remains the **only** preview path. Real storage integration does not change the preview contract — it only changes the underlying storage read source.

### Preview Rules

| Rule | Constraint |
|------|-----------|
| Existing backend-controlled preview proxy is the only preview path | ⛔ Mandatory |
| Preview endpoint streams image bytes from backend | Byte stream, not URL |
| Preview must not return storage key | ⛔ Mandatory — not in body or headers |
| Preview must not return signed URL | ⛔ Mandatory — not in body or headers |
| Preview must not return public URL | ⛔ Mandatory — not in body or headers |
| Preview must not return provider URL | ⛔ Mandatory — not in body or headers |
| Preview must not return base64 | ⛔ Mandatory — not in body |
| Preview must not expose raw provider/storage payload | ⛔ Mandatory |
| Preview access rules must remain backend-controlled | Authorization required |
| Frontend/admin receives only rendered image bytes via backend endpoint | No URL-based access |

### Preview Availability

| Condition | Preview Availability |
|-----------|---------------------|
| Storage not started | Preview unavailable |
| Storage in progress | Preview unavailable |
| Storage failed | Preview unavailable |
| Storage stored successfully | Preview available |
| Asset approved | Preview remains available |
| Asset rejected | Preview remains available |

### Preview vs. Review Separation

```
Preview = "View the image"     → Information action (read-only, idempotent)
Approve = "Approve the output" → Decision action (explicit, audited)
Reject  = "Reject + reason"   → Decision action (explicit, audited, reason required)

Preview ≠ Approve
Preview ≠ Reject
Preview ≠ Status change
Preview access does NOT trigger approval
Preview access does NOT trigger rejection
Preview access does NOT create review decision
```

---

## 8. Admin DTO / API Contract Boundaries

### Allowed Safe Admin-Facing Concepts

| Concept | Admin DTO / API | Example |
|---------|----------------|---------|
| Retrieval status category | ✅ Allowed | "retrieval pending", "retrieval failed", "retrieved" |
| Validation status category | ✅ Allowed | "validation failed", "validated" |
| Storage status category | ✅ Allowed | "storage pending", "storage failed", "stored" |
| Preview availability | ✅ Allowed | "preview available", "preview unavailable" |
| Safe failure category | ✅ Allowed | "provider output unavailable", "unsupported media type" |
| Safe failure summary | ✅ Allowed | Product-level message, no technical details |
| Review required flag/status | ✅ Allowed | "review required", "approved", "rejected" |
| Timestamps | ✅ Allowed | storedAt, retrievedAt, validatedAt, reviewedAt |
| Asset ID | ✅ Allowed | generatedAssetId, mediaAssetId |
| Project/storyboard relation | ✅ Allowed | If already existing and safe in FP-007/FP-008 contract |
| Media metadata | ✅ Allowed | mimeType, width, height, fileSizeBytes, checksumSha256 |
| CorrelationId | ✅ Allowed | Trace identifier for support |
| Safe error code | ✅ Allowed | Normalized error category |
| Safe error message | ✅ Allowed | Product-level, sanitized |

### Forbidden Admin-Facing Concepts

| Concept | Admin DTO / API | Reason |
|---------|----------------|--------|
| Provider URL | ⛔ Forbidden | Security — transient internal use only |
| Provider raw output | ⛔ Forbidden | Provider security |
| Provider headers | ⛔ Forbidden | Provider security |
| Provider request/response JSON | ⛔ Forbidden | Provider security |
| Storage object key | ⛔ Forbidden | Internal infrastructure |
| Storage bucket/container name | ⛔ Forbidden | Internal infrastructure |
| Storage raw response | ⛔ Forbidden | Infrastructure security |
| Public URL | ⛔ Forbidden | Not generated |
| Signed URL | ⛔ Forbidden | Not returned to admin |
| CDN URL | ⛔ Forbidden | Not generated |
| Base64 image data | ⛔ Forbidden | Performance and security |
| Secrets / API keys / config values | ⛔ Forbidden | Secret management |
| Internal stack traces | ⛔ Forbidden | Security |
| Low-level provider error details | ⛔ Forbidden | Provider security |
| Low-level storage error details | ⛔ Forbidden | Infrastructure security |
| Internal prompt | ⛔ Forbidden | Prompt security |

### Explicit API Boundary Rules

| Rule | Constraint |
|------|-----------|
| No generic status update endpoint | ⛔ Explicit actions only |
| No READY_FOR_PUBLISH status or endpoint | ⛔ Not introduced |
| No COMPLETED status or endpoint | ⛔ Not introduced |
| No automatic approval mechanism | ⛔ Human review gate mandatory |
| No publish/export API | ⛔ Out of scope |

---

## 9. Status and Error Contract

### Conceptual Status Categories

The following are contract-level categories defining the conceptual states. These are **not** final enum implementations — exact enum names and persistence strategy are deferred to Engineering Design.

#### Retrieval Status

| Category | Meaning |
|----------|---------|
| Retrieval Pending | Provider output retrieval has not started |
| Retrieval In Progress | Backend is downloading provider output |
| Retrieval Failed | Download failed (timeout, HTTP error, TTL expired, etc.) |
| Retrieved | Provider output bytes successfully downloaded |

#### Validation Status

| Category | Meaning |
|----------|---------|
| Validation Pending | Validation has not started |
| Validation Failed | One or more validation checks failed |
| Validated | All validation checks passed |

#### Storage Status

| Category | Meaning |
|----------|---------|
| Storage Pending | Storage write has not started |
| Storage Failed | Storage write failed |
| Stored | Image successfully written to private storage |

#### Preview Status

| Category | Meaning |
|----------|---------|
| Preview Unavailable | Storage not complete or failed |
| Preview Available | Image can be previewed via backend proxy |

#### Review Status

| Category | Meaning |
|----------|---------|
| Review Required | Output awaiting explicit human review |
| Approved | Admin explicitly approved (explicit action) |
| Rejected | Admin explicitly rejected (explicit action, reason required) |

### Failure Categories

| Category | Trigger | Admin-Visible Summary |
|----------|---------|----------------------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider output reference does not exist or is invalid | "Sağlayıcı çıktısı mevcut değil" |
| `PROVIDER_RETRIEVAL_FAILED` | HTTP download failed (timeout, connection error, HTTP error, TTL expired) | "Sağlayıcı çıktısı alınamadı" |
| `UNSUPPORTED_MEDIA_TYPE` | Downloaded content is not PNG, JPEG, or WebP | "Desteklenmeyen dosya türü" |
| `FILE_TOO_LARGE` | Downloaded content exceeds size limit | "Dosya boyutu çok büyük" |
| `VALIDATION_FAILED` | General validation failure (corrupt, unreadable, dimension error, checksum error) | "Dosya doğrulaması başarısız" |
| `STORAGE_WRITE_FAILED` | Writing validated image to object storage failed | "Depolama hatası" |
| `PREVIEW_UNAVAILABLE` | Storage not complete, failed, or storage read failed | "Önizleme mevcut değil" |
| `INTERNAL_CONFIGURATION_UNAVAILABLE` | Storage provider configuration missing or invalid | "Sistem yapılandırma hatası" |

### Status and Error Rules

| Rule | Constraint |
|------|-----------|
| These are contract-level categories, not necessarily final enum implementation | Engineering decides exact enums |
| Do not create enums in this task | Documentation-only |
| Do not require schema changes in this task | Documentation-only |
| Do not introduce publish lifecycle statuses | ⛔ Out of scope |
| Do not collapse review/approval into storage success | Independent concerns |
| Stored does not mean approved | ⛔ Mandatory separation |
| Preview available does not mean approved | ⛔ Mandatory separation |
| Retrieved does not mean validated | Separate steps |
| Validated does not mean stored | Separate steps |
| AI output must remain review-required until explicit human review | ⛔ Mandatory |

---

## 10. Logging and Audit Contract

### Safe Audit Rules

| Action | Audit Requirement | What Is Audited | What Must Not Be Audited |
|--------|-------------------|-----------------|--------------------------|
| Retrieval attempt | ✅ Audit | Asset ID, correlation ID, timestamp, status category, safe failure category | Provider URL, auth token, raw provider response |
| Validation result | ✅ Audit | Asset ID, correlation ID, validation status, detected MIME type, size, dimensions, checksum, safe error category | Raw bytes, base64, provider payload |
| Storage result | ✅ Audit | Asset ID, correlation ID, storage status, safe error category, stored timestamp | Storage object key, raw storage response, bucket name, credentials |
| Preview access | ✅ Audit | Asset ID, correlation ID, requester identity, access timestamp | Object key, signed URL, storage reference |

### Logging Rules

| Rule | Constraint |
|------|-----------|
| Log safe correlation IDs / asset IDs | ✅ Allowed and recommended |
| Log safe status categories and timestamps | ✅ Allowed |
| Do not log provider URLs | ⛔ Mandatory |
| Do not log signed URLs | ⛔ Mandatory |
| Do not log storage keys | ⛔ Mandatory |
| Do not log secrets / API keys / config values | ⛔ Mandatory |
| Do not log base64 | ⛔ Mandatory |
| Do not log raw provider payloads | ⛔ Mandatory |
| Do not log raw storage payloads | ⛔ Mandatory |
| Internal stack traces should not be admin-facing | ⛔ Server-side only |
| Error messages in logs must be sanitized | Use `ImageStorageErrorNormalizer` pattern |

### Sanitization Contract

The `ImageStorageErrorNormalizer` sanitization patterns established in FP-008 must be extended for FP-009:

| Pattern | Mask |
|---------|------|
| `https://...`, `http://...` | `[URL_REDACTED]` |
| `s3://...`, `gs://...`, `az://...` | `[STORAGE_REDACTED]` |
| `imagegen/{uuid}/{uuid}` | `[STORAGE_KEY_REDACTED]` |
| `api_key=...`, `secret:...`, `token=...`, `bearer ...` | `[REDACTED]` |
| `base64,...` | `[DATA_REDACTED]` |
| 64+ char alphanumeric strings | `[LONG_DATA_REDACTED]` |
| Stack trace lines (`at com.xxx.yyy(...)`) | `[STACK_REDACTED]` |

---

## 11. Security Boundaries

### Boundary Map

```
┌─────────────────────────────────────────────────────────────┐
│                    EXTERNAL BOUNDARY                         │
│                                                              │
│  ┌─────────────┐        ┌──────────────────────────┐        │
│  │   Provider   │───────▶│   Backend Retrieval       │        │
│  │   (OpenAI/   │  HTTP  │   Boundary                │        │
│  │   etc.)      │◀───────│                           │        │
│  └─────────────┘        └──────────┬───────────────┘        │
│                                     │                        │
│                                     ▼                        │
│                          ┌──────────────────────────┐        │
│                          │   Validation Boundary     │        │
│                          └──────────┬───────────────┘        │
│                                     │                        │
│                                     ▼                        │
│                          ┌──────────────────────────┐        │
│                          │   Private Storage         │        │
│                          │   Boundary                │        │
│                          └──────────┬───────────────┘        │
│                                     │                        │
│                                     ▼                        │
│                          ┌──────────────────────────┐        │
│                          │   Preview Proxy           │        │
│                          │   Boundary                │        │
│                          └──────────┬───────────────┘        │
│                                     │                        │
│                                     ▼                        │
│                          ┌──────────────────────────┐        │
│                          │   Admin API Boundary      │        │
│                          └──────────┬───────────────┘        │
│                                     │                        │
│                                     ▼                        │
│                          ┌──────────────────────────┐        │
│                          │   Human Review            │        │
│                          │   Boundary                │        │
│                          └──────────────────────────┘        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

### 11.1 Provider Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses** | Normalized generation result with internal output reference (from FP-007 provider gateway) |
| **What must never cross outward** | Provider URL to admin/user, raw provider response to admin/user, auth tokens, API keys |
| **Direction** | Provider → Backend only |
| **Admin visibility** | None — provider boundary is fully internal |

---

### 11.2 Backend Retrieval Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses inward** | Provider output reference (internal), provider config/credentials (internal) |
| **What crosses outward** | Downloaded image bytes (to validation), retrieval status (safe), safe failure category |
| **What must never cross outward to admin** | Provider URL, provider response headers, auth tokens, raw HTTP response, download URL |
| **Transient data** | Provider URL used for download is discarded after retrieval |

---

### 11.3 Validation Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses inward** | Image bytes from retrieval |
| **What crosses outward** | Validated bytes (to storage), validation result (safe metadata: MIME, size, dimensions, checksum) |
| **What must never cross outward to admin** | Raw bytes, base64, provider payload, validation internals beyond safe category |
| **Rejection** | Invalid content is rejected — safe error category produced |

---

### 11.4 Private Storage Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses inward** | Validated image bytes, server-side generated storage key |
| **What crosses outward** | Storage status (safe), stored timestamp, safe error category |
| **What must never cross outward to admin** | Storage object key, bucket name, storage credentials, raw SDK response, signed URL, public URL |
| **Read path** | Storage key used internally by preview proxy — never exposed |

---

### 11.5 Preview Proxy Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses inward** | Asset ID from admin request, authorization context |
| **What crosses outward to admin** | Image bytes stream (Content-Type + binary body) |
| **What must never cross outward** | Storage object key, signed URL, public URL, provider URL, base64, raw storage response |
| **Side effects** | None — preview is read-only, idempotent, does not change status or create review decision |

---

### 11.6 Admin API Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses outward to admin** | Safe status categories, safe metadata (MIME, size, dimensions, checksum, timestamps), safe error summaries, asset IDs, correlation IDs |
| **What must never cross outward** | Provider URL, storage key, signed URL, public URL, base64, raw payloads, secrets, stack traces, internal error details |
| **Actions available** | View status, preview (via proxy), approve (explicit), reject (explicit + reason) |
| **Actions not available** | Generic status update, READY_FOR_PUBLISH, publish/export, automatic approval |

---

### 11.7 Human Review Boundary

| Aspect | Detail |
|--------|--------|
| **What crosses inward** | Admin explicit approve/reject action, reject reason |
| **What crosses outward** | Review audit record (safe), updated review status |
| **Preservation** | Human review gate is mandatory — no bypass from any other boundary |
| **Independence** | Review is independent from retrieval, validation, and storage — storage success ≠ approval |
| **No automatic crossing** | No boundary produces automatic approval |

---

## 12. Acceptance Criteria

| # | Criterion | Status |
|---|-----------|--------|
| 1 | AI Platform / Media Contract Design document exists | ✅ |
| 2 | Document references FP-009 Product Design | ✅ |
| 3 | Document references FP-008 closure baseline (commit `a36eba8`, 217/217 tests) | ✅ |
| 4 | Document defines safe provider output reference contract | ✅ |
| 5 | Document defines controlled backend-only retrieval contract | ✅ |
| 6 | Document defines validation-before-storage contract | ✅ |
| 7 | Document defines private storage contract | ✅ |
| 8 | Document preserves backend-controlled preview proxy | ✅ |
| 9 | Document prohibits provider URL persistence | ✅ |
| 10 | Document prohibits provider URL exposure | ✅ |
| 11 | Document prohibits provider URL logging | ✅ |
| 12 | Document prohibits storage key exposure | ✅ |
| 13 | Document prohibits public/signed URL exposure | ✅ |
| 14 | Document prohibits base64 in DB/DTO/logs | ✅ |
| 15 | Document prohibits raw provider/storage payload exposure | ✅ |
| 16 | Document prohibits secrets/API key/config exposure | ✅ |
| 17 | Document preserves human review gate | ✅ |
| 18 | Document confirms AI output is never automatically APPROVED | ✅ |
| 19 | Document confirms storage success does not equal approval | ✅ |
| 20 | Document confirms preview availability does not equal approval | ✅ |
| 21 | Document confirms READY_FOR_PUBLISH is out of scope | ✅ |
| 22 | Document confirms COMPLETED is out of scope | ✅ |
| 23 | Document confirms generic status update endpoint is out of scope | ✅ |
| 24 | Document confirms publish/export is out of scope | ✅ |
| 25 | Document confirms video/audio are out of scope | ✅ |
| 26 | Document contains no code implementation | ✅ |
| 27 | Document contains no enum/DTO/entity/migration/config changes | ✅ |

---

## 13. Next Document

### FP-009 Task 03 — Engineering Design

Task 03 should translate this AI Platform / Media Contract into concrete backend components, interfaces, services, DTO boundaries, tests, and phased coding tasks while preserving all safety constraints defined in this document.

Engineering Design should address:

| Topic | Description |
|-------|-------------|
| Provider output retrieval service | Concrete service for safe provider download with HTTP client, timeout, retry |
| Real storage adapter implementation | S3/MinIO adapter implementing `ImageStorageAdapter` interface |
| Storage handoff endpoint activation | Transitioning from 501 to real handoff flow |
| New error codes | Extending `ImageStorageErrorCode` for retrieval-specific failures |
| Configuration model | Application properties for storage provider, bucket, credentials, timeouts |
| Migration plan | V12+ migration if new fields are needed (e.g., retrieval status) |
| DTO boundary enforcement | Ensuring all new fields follow safe metadata allowlist |
| Test strategy | Unit tests, integration tests, guardrail tests for new components |
| Phased coding tasks | Task breakdown for incremental implementation |

### Workflow Status

1. ✅ **Product Design** — FP-009 Task 01 (completed)
2. ✅ **AI Platform / Media Contract Design** — FP-009 Task 02 ← this document
3. ⬜ **Engineering Design** — FP-009 Task 03
4. ⬜ **Coding Tasks** — FP-009 Task 04+
5. ⬜ **Code Implementation**

---

*Bu doküman FP-009 Task 02 kapsamında oluşturulmuştur. Documentation-only — kod, config, migration, test, DTO, enum, entity, controller, service veya runtime değişikliği yoktur.*
