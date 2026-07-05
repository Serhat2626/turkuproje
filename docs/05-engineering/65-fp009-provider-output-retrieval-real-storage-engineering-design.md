# FP-009 — Provider Output Retrieval & Real Storage Provider Integration — Engineering Design

---

## 1. Document Status

| Item | Detail |
|------|--------|
| **Feature** | FP-009 — Provider Output Retrieval & Real Storage Provider Integration |
| **Document type** | Engineering Design |
| **Status** | **PLANNED** |
| **Task** | FP-009 Task 03 — Engineering Design |
| **Task scope** | Documentation-only |
| **Product Design** | [63-fp009-provider-output-retrieval-real-storage-product-design.md](../03-product-design/63-fp009-provider-output-retrieval-real-storage-product-design.md) |
| **AI Platform / Media Contract** | [64-fp009-provider-output-retrieval-real-storage-media-contract-design.md](../04-ai-platform/64-fp009-provider-output-retrieval-real-storage-media-contract-design.md) |
| **FP-008 Closure Doc** | [62-fp008-final-closure-operational-baseline.md](./62-fp008-final-closure-operational-baseline.md) |
| **FP-008 Engineering Design** | [61-fp008-safe-storage-handoff-preview-proxy-engineering-design-and-task-breakdown.md](./61-fp008-safe-storage-handoff-preview-proxy-engineering-design-and-task-breakdown.md) |
| **Source baseline** | FP-008 — CLOSED (commit `a36eba8`, 217/217 tests passing) |
| **Closed predecessor chain** | FP-001 → FP-002 → FP-003 → FP-004 → FP-005 → FP-006 → FP-007 → FP-008 |

> **This is a documentation-only task.** No production code, configuration, migration, test, DTO, enum, entity, controller, service, or runtime changes are made. No provider SDK integration. No real storage provider code.

---

## 2. Engineering Objective

| # | Objective |
|---|-----------|
| 1 | Move from provider metadata-only output (FP-007) to controlled backend retrieval of actual image bytes |
| 2 | Download provider image output internally using a safe, sanitized retrieval service |
| 3 | Validate downloaded output before storage is considered successful (reusing FP-008 `ImageMediaValidationService`) |
| 4 | Store validated output in private real object storage (S3-compatible) via a new `ImageStorageAdapter` implementation |
| 5 | Keep preview backend-controlled through existing FP-008 `ImagePreviewProxyService` |
| 6 | Activate the storage handoff endpoint from 501 to real retrieval → validation → storage flow |
| 7 | Keep human review gate mandatory — storage success ≠ approval |
| 8 | Never expose provider URLs, storage object keys, signed/public URLs, base64, raw payloads, or secrets to admin/user |

---

## 3. Current Baseline Assessment

### 3.1 Verified FP-008 Components

The following components exist in the codebase under `com.anadoluatlasi.backend.imagegen`:

#### Entities & Enums (`imagegen.entity`)

| Component | Type | Key Details |
|-----------|------|-------------|
| `ImageGenerationRequest` | Entity | Generation request (FP-007) |
| `ImageGenerationResult` | Entity | Provider result metadata (FP-007) |
| `ImageGeneratedAsset` | Entity | Review-gated asset (FP-007) |
| `ImageMediaAsset` | Entity | Media metadata + storage fields (FP-007 + FP-008) |
| `ImageReviewAuditEntry` | Entity | Review audit record (FP-007) |
| `ImageGenerationRequestStatus` | Enum | REQUEST_CREATED, GENERATION_IN_PROGRESS, GENERATION_COMPLETED, GENERATION_FAILED |
| `GeneratedAssetStatus` | Enum | GENERATED_PENDING_REVIEW, APPROVED, REJECTED |
| `ImageMediaAssetKind` | Enum | GENERATED_IMAGE |
| `ImageMediaAssetReviewStatus` | Enum | REVIEW_APPROVED, REVIEW_REJECTED |
| `ImageStorageHandoffStatus` | Enum | STORAGE_PENDING, STORAGE_VALIDATING, STORAGE_STORED, STORAGE_FAILED |
| `ImagePreviewStatus` | Enum | PREVIEW_UNAVAILABLE, PREVIEW_AVAILABLE |
| `ImageStorageErrorCode` | Enum | 13 values: PROVIDER_OUTPUT_UNAVAILABLE, PROVIDER_OUTPUT_EXPIRED, PROVIDER_OUTPUT_DOWNLOAD_FAILED, MEDIA_VALIDATION_FAILED, UNSUPPORTED_MEDIA_TYPE, MEDIA_TOO_LARGE, MEDIA_DIMENSION_INVALID, CHECKSUM_CALCULATION_FAILED, STORAGE_WRITE_FAILED, STORAGE_READ_FAILED, PREVIEW_NOT_AVAILABLE, PREVIEW_ACCESS_DENIED, UNKNOWN_STORAGE_HANDOFF_FAILURE |
| `ImageGenerationErrorCode` | Enum | PROVIDER_ERROR, TIMEOUT_ERROR, INVALID_REQUEST, CONTENT_POLICY_VIOLATION, RATE_LIMIT_EXCEEDED, NETWORK_ERROR, UNKNOWN_ERROR |

#### ImageMediaAsset Storage Fields (FP-008)

| Field | Type | Column | Notes |
|-------|------|--------|-------|
| `storageHandoffStatus` | `ImageStorageHandoffStatus` | `storage_handoff_status` VARCHAR(40) | Default STORAGE_PENDING |
| `previewStatus` | `ImagePreviewStatus` | `preview_status` VARCHAR(40) | Default PREVIEW_UNAVAILABLE |
| `storageProvider` | `String` | `storage_provider` | Internal-only (e.g., "inmemory") |
| `storageObjectKey` | `String` | `storage_object_key` | Internal-only |
| `storageErrorCode` | `ImageStorageErrorCode` | `storage_error_code` VARCHAR(60) | Safe error code |
| `storageErrorMessage` | `String` | `storage_error_message` TEXT | Safe normalized message |
| `storedAt` | `Instant` | `stored_at` TIMESTAMP | Storage completion time |

#### Services (`imagegen.service`)

| Component | Key Method | Description |
|-----------|-----------|-------------|
| `ImageMediaValidationService` | `validate(byte[] mediaBytes, String correlationId)` → `ImageMediaValidationResult` | MIME sniffing, 10 MB limit, dimensions, SHA-256 |
| `ImageStorageHandoffService` | `executeHandoff(ImageStorageHandoffRequest)` → `ImageStorageHandoffResult` | Validate → store → metadata update |
| `ImagePreviewProxyService` | `getPreview(UUID generatedAssetId)` → `ImagePreviewProxyResult` | Backend-controlled preview byte streaming |
| `ImageStorageErrorNormalizer` | `safeMessage()`, `sanitize()`, `safeLogContext()` | Error normalization and sanitization (static utility class) |
| `ImageMediaAssetMapper` | Maps entity → DTO (static utility class) | Safe field mapping (no storage key/provider URL) |
| `ImageGeneratedAssetHandoffService` | Creates `ImageMediaAsset` + `ImageGeneratedAsset` after generation | FP-007 asset creation; controller endpoint returns 501 |
| `ImageGenerationOrchestrationService` | `executeGeneration()` | Calls provider, creates result/asset entities |

#### Storage Adapter (`imagegen.storage`)

| Component | Type | Description |
|-----------|------|-------------|
| `ImageStorageAdapter` | Interface | `write(ImageStorageWriteRequest)` → `ImageStorageWriteResult`, `read(ImageStorageReadRequest)` → `ImageStorageReadResult` |
| `InMemoryImageStorageAdapter` | `@Component`, `@Primary` | `ConcurrentHashMap<String, byte[]>`, provider alias "inmemory", key pattern `imagegen/{mediaAssetId}/{uuid}` |
| `ImageStorageWriteRequest` | `@Data @Builder` | Fields: `mediaAssetId`, `generatedAssetId`, `correlationId`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, `imageBytes`, `requestedAt` |
| `ImageStorageWriteResult` | `@Data @Builder` | Fields: `success`, `storageProviderAlias`, `storageObjectKey`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, `storedAt`, `errorCode`, `errorMessage`, `correlationId` |
| `ImageStorageReadRequest` | `@Data @Builder` | Fields: `storageObjectKey`, `mediaAssetId`, `generatedAssetId`, `correlationId` |
| `ImageStorageReadResult` | `@Data @Builder` | Fields: `success`, `imageBytes`, `mimeType`, `fileSizeBytes`, `errorCode`, `errorMessage`, `correlationId` |

#### Provider (`imagegen.provider`)

| Component | Type | Description |
|-----------|------|-------------|
| `ImageGenerationProviderGateway` | Interface | Provider abstraction — `generate()`, `getProviderAlias()` |
| `ImageGenerationProviderAutoConfiguration` | Config | Auto-configuration for provider bean registration |
| `MockImageGenerationProviderGateway` | Impl | Test/dev mock provider |
| `OpenAiImageGenerationProviderGateway` | Impl | Real OpenAI provider — uses `response_format: "url"` |
| `ImageProviderGenerationResult` | `@Data @Builder` | Safe result — `success`, `providerAlias`, `modelName`, `outputCount`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, etc. |

#### Handoff Service Models (`imagegen.service.model`)

| Component | Fields |
|-----------|--------|
| `ImageStorageHandoffRequest` | `generatedAssetId`, `mediaAssetId`, `imageBytes`, `correlationId` |
| `ImageStorageHandoffResult` | `success`, `mediaAssetId`, `generatedAssetId`, `storageHandoffStatus`, `previewStatus`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, `storedAt`, `errorCode`, `errorMessage`, `correlationId` |
| `ImagePreviewProxyResult` | `available`, `imageBytes`, `mimeType`, `fileSizeBytes`, `errorCode`, `errorMessage` |
| `ImageMediaValidationResult` | `valid`, `detectedMimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, `errorCode`, `errorMessage` |

#### Controller & DTOs (`imagegen.controller`, `imagegen.controller.dto`)

| Component | Description |
|-----------|-------------|
| `AdminImageGenerationController` | All admin endpoints |
| `StorageHandoffResponse` | Safe DTO — `success`, `mediaAssetId`, `generatedAssetId`, `storageHandoffStatus`, `previewStatus`, `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256`, `storedAt`, `errorCode`, `errorMessage`, `correlationId` |
| `ImageMediaAssetResponse` | Safe DTO — includes `storageHandoffStatus`, `previewStatus`, `storageErrorCode`, `storageErrorMessage`, `storedAt` |

### 3.2 Current Endpoint Baseline

| Method | Path | Status |
|--------|------|--------|
| POST | `.../generated-assets/{id}/storage-handoff` | **501** — returns `StorageHandoffResponse` with error message |
| GET | `.../generated-assets/{id}/preview` | **Active** — streams image bytes from `ImagePreviewProxyService` |

### 3.3 Critical Observation: Provider Image URL Gap

**This is the core gap FP-009 must fill.**

The `OpenAiImageGenerationProviderGateway` calls the OpenAI API with `response_format: "url"`. However, the `extractSafeResult()` method only reads `outputCount` from the response data — **the provider image URL is never captured, extracted, or returned**.

`ImageProviderGenerationResult` has **no field** for `providerImageUrl` or any provider output reference. The URL is completely lost after the API call.

FP-009 must:

1. Extend `ImageProviderGenerationResult` (or create a new internal model) to carry the transient provider output URL.
2. Modify `OpenAiImageGenerationProviderGateway.extractSafeResult()` to extract the image URL from the OpenAI response.
3. Pass this URL through to the retrieval service — never persisting, logging, or exposing it.
4. After retrieval, discard the URL.

This is internal engineering only — no admin/user exposure of the URL.

### 3.4 Migration Baseline

| Migration | Feature |
|-----------|---------|
| V9 | FP-007 — Core imagegen tables |
| V10 | FP-007 — Media review status column |
| V11 | FP-008 — Storage handoff fields (5 columns: `storage_handoff_status`, `preview_status`, `storage_error_code`, `storage_error_message`, `stored_at` + 2 indexes) |

> **Note:** `storage_provider`, `storage_bucket`, and `storage_object_key` columns are defined on the `ImageMediaAsset` entity but were **not** added in V11 migration. The table is `fp007_image_media_assets`. A V12 migration is likely required to add these columns.

### 3.5 Configuration Baseline

Current `application.yml` has provider config only:

```yaml
imagegen:
  provider:
    mode: mock  # or openai
    timeout-ms: 60000
    max-output-count: 1
    model-name: dall-e-3
    image-size: 1024x1024
    openai-api-key: ${IMAGEGEN_PROVIDER_OPENAI_API_KEY:}
```

No storage-specific configuration exists. `InMemoryImageStorageAdapter` is registered as `@Component` unconditionally — no config-driven adapter selection. FP-009 will need storage configuration properties and conditional adapter activation.

---

## 4. Proposed Component Architecture

### 4.1 Component Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FP-009 COMPONENT MAP                                │
│                                                                             │
│  ┌────────────────────────┐    ┌────────────────────────────────────────┐   │
│  │ AdminImageGeneration   │    │ ImageGenerationOrchestrationService   │   │
│  │ Controller             │    │ (FP-007 — may wire retrieval trigger) │   │
│  │ (endpoint activation)  │    └──────────────────┬───────────────────┘   │
│  └───────────┬────────────┘                       │                       │
│              │                                     │                       │
│              ▼                                     ▼                       │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ ImageGeneratedAssetHandoffService                                   │   │
│  │ (orchestrates: retrieval → validation → storage → status update)    │   │
│  └──────┬───────────────┬───────────────┬───────────────┬─────────┘   │
│         │               │               │               │             │
│         ▼               ▼               ▼               ▼             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────┐     │
│  │ ProviderOut- │ │ ImageMedia-  │ │ ImageStorage-│ │ ImageMedia│     │
│  │ putRetrieval │ │ Validation-  │ │ Adapter      │ │ AssetMap- │     │
│  │ Service      │ │ Service      │ │ (interface)  │ │ per       │     │
│  │ [NEW]        │ │ [FP-008]     │ │ [FP-008]     │ │ [FP-008]  │     │
│  └──────┬───────┘ └──────────────┘ └──────┬───────┘ └──────────┘     │
│         │                                  │                          │
│         ▼                                  ▼                          │
│  ┌──────────────┐                   ┌──────────────────────────┐     │
│  │ ProviderOut- │                   │ InMemory...Adapter [FP-008] │  │
│  │ putClient    │                   │ S3ImageStorageAdapter [NEW]  │  │
│  │ [NEW]        │                   └──────────────────────────┘     │
│  └──────────────┘                                                     │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ ImagePreviewProxyService [FP-008 — unchanged]                   │   │
│  │ → reads from ImageStorageAdapter (now reads from real storage)   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ ImageStorageErrorNormalizer [FP-008 — extended with new codes]   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Component A — ProviderOutputRetrievalService [NEW]

**Package:** `com.anadoluatlasi.backend.imagegen.service`

**Responsibilities:**

| Responsibility | Detail |
|---------------|--------|
| Accept safe provider output reference | Takes internal provider output identifier (e.g., `providerImageUrl` from `NormalizedImageGenerationResult`) |
| Resolve provider output internally | Uses `ProviderOutputClient` to download image bytes |
| Retrieve image bytes/stream | Downloads from provider temporary URL |
| Never persist provider URL | URL is discarded after retrieval |
| Never expose provider URL | Not in DTO, log, header, or error message |
| Never log provider URL | Sanitized logging only |
| Pass retrieved bytes to validation | Returns bytes for `ImageMediaValidationService` |
| Return safe result object only | `ProviderOutputRetrievalResult` with safe fields |

**Proposed Method Signature:**

```
ProviderOutputRetrievalResult retrieveProviderOutput(ProviderOutputRetrievalRequest request)
```

**ProviderOutputRetrievalRequest — Proposed Fields:**

| Field | Type | Visibility |
|-------|------|------------|
| `generatedAssetId` | `UUID` | Safe |
| `mediaAssetId` | `UUID` | Safe |
| `providerImageUrl` | `String` | **Internal-only — never logged, never persisted, never in DTO** |
| `correlationId` | `String` | Safe |

**ProviderOutputRetrievalResult — Proposed Fields:**

| Field | Type | Visibility |
|-------|------|------------|
| `success` | `boolean` | Safe |
| `imageBytes` | `byte[]` | **Internal-only — passed to validation, never in DTO** |
| `detectedContentType` | `String` | Internal (validation input) |
| `contentLength` | `long` | Internal (validation input) |
| `errorCode` | `ImageStorageErrorCode` | Safe |
| `errorMessage` | `String` | Safe (sanitized) |
| `correlationId` | `String` | Safe |

**ProviderOutputRetrievalResult Must Not Contain:**

- Provider URL
- Provider response headers
- Raw provider response body
- Auth tokens / API keys
- Base64 image data
- Raw provider SDK error details

### 4.3 Component B — ProviderOutputClient [NEW]

**Package:** `com.anadoluatlasi.backend.imagegen.provider`

**Responsibilities:**

| Responsibility | Detail |
|---------------|--------|
| Provider-specific HTTP retrieval | Downloads image bytes from transient provider URL |
| Uses internal credentials/config | No credential exposure |
| Configurable timeout | Connection and read timeouts via application properties |
| Configurable retry | Transient failure retry with backoff |
| Max download size enforcement | Pre-check Content-Length, streaming cutoff at limit |
| Error sanitization | Raw HTTP/provider errors → safe `ImageStorageErrorCode` |
| Must not return raw provider payload | Only returns bytes + safe metadata |

**Proposed Method Signature:**

```
ProviderOutputDownloadResult download(String providerImageUrl, String correlationId)
```

**Configuration Properties (proposed for `application.yml`):**

```yaml
imagegen:
  retrieval:
    connect-timeout-ms: 10000
    read-timeout-ms: 30000
    max-download-size-bytes: 10485760  # 10 MB
    retry:
      max-attempts: 3
      backoff-ms: 1000
```

**Error Mapping:**

| HTTP / Network Condition | Mapped Error Code |
|--------------------------|-------------------|
| Connection timeout | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (existing) |
| Read timeout | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (existing) |
| HTTP 404 / 410 (TTL expired) | `PROVIDER_OUTPUT_EXPIRED` (existing) |
| HTTP 4xx (other) | `PROVIDER_OUTPUT_UNAVAILABLE` (existing) |
| HTTP 5xx | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (existing) |
| Connection refused / DNS error | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (existing) |
| Content-Length exceeds limit | `MEDIA_TOO_LARGE` (existing) |
| IO exception during download | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (existing) |

### 4.4 Component C — ImageMediaValidationService [FP-008 — Reused]

**Package:** `com.anadoluatlasi.backend.imagegen.service` (existing)

**No changes to the existing service.** FP-009 reuses the existing `validate(byte[] imageBytes)` method.

| Check | Existing | FP-009 |
|-------|----------|--------|
| MIME sniffing (PNG/JPEG/WebP) | ✅ | Reused |
| 10 MB size limit | ✅ | Reused |
| Zero-byte rejection | ✅ | Reused |
| Corrupt image rejection | ✅ | Reused |
| Width/height extraction | ✅ | Reused |
| SHA-256 checksum | ✅ | Reused |

### 4.5 Component D — S3ImageStorageAdapter [NEW]

**Package:** `com.anadoluatlasi.backend.imagegen.storage`

**Implements:** `ImageStorageAdapter` (existing interface)

**Responsibilities:**

| Responsibility | Detail |
|---------------|--------|
| Write validated image to S3-compatible storage | `write(ImageStorageWriteRequest)` → `ImageStorageWriteResult` |
| Read image bytes from S3-compatible storage | `read(ImageStorageReadRequest)` → `ImageStorageReadResult` |
| Internal storage reference only | Storage key stays internal |
| Never return public URL | Not generated |
| Never return signed URL | Not generated for admin/user |
| Sanitize storage errors | Raw S3 SDK errors → safe `ImageStorageErrorCode` |
| Keep raw storage response internal | SDK response not exposed |
| Provider alias | e.g., `"s3"` or `"minio"` |

**Configuration Properties (proposed for `application.yml`):**

```yaml
imagegen:
  storage:
    provider: inmemory  # inmemory | s3
    s3:
      endpoint: ${STORAGE_S3_ENDPOINT:http://localhost:9000}
      bucket: ${STORAGE_S3_BUCKET:imagegen-assets}
      access-key: ${STORAGE_S3_ACCESS_KEY:minioadmin}
      secret-key: ${STORAGE_S3_SECRET_KEY:minioadmin}
      region: ${STORAGE_S3_REGION:us-east-1}
```

**Storage Key Pattern:** Reuses existing FP-008 pattern: `imagegen/{mediaAssetId}/{randomUUID}`

**Adapter Selection Strategy:**

- `imagegen.storage.provider=inmemory` → `InMemoryImageStorageAdapter` (existing, for test/dev)
- `imagegen.storage.provider=s3` → `S3ImageStorageAdapter` (new, for staging/production)

The `@Primary` annotation on `InMemoryImageStorageAdapter` should be replaced with conditional activation based on the `imagegen.storage.provider` property.

### 4.6 Component E — ImageGeneratedAssetHandoffService / ImageStorageHandoffService [FP-008 — Enhanced]

**Package:** `com.anadoluatlasi.backend.imagegen.service` (existing)

**Current State:**

- `ImageGeneratedAssetHandoffService` currently creates `ImageMediaAsset` + `ImageGeneratedAsset` records after generation (FP-007 asset creation service). The controller storage-handoff endpoint throws `ResponseStatusException(HttpStatus.NOT_IMPLEMENTED)` directly.
- `ImageStorageHandoffService.executeHandoff(ImageStorageHandoffRequest)` accepts `imageBytes` directly and runs validate → store → metadata update.
- `ImageStorageHandoffRequest` already has `imageBytes` field, plus `contentTypeHint`, `correlationId`, `requestedBy`, `requestedAt`.

**FP-009 Enhancement:**

The handoff flow must be extended to include provider output retrieval before validation:

```
FP-008:  imageBytes (given) → validate → store → metadata update
FP-009:  providerOutputUrl (internal) → retrieve → validate → store → metadata update
```

**Proposed Orchestration:**

A new orchestration layer (either extending `ImageGeneratedAssetHandoffService` or creating a new `StorageHandoffOrchestrationService`) will:

1. Load `ImageGeneratedAsset` and linked `ImageMediaAsset`.
2. Resolve provider output reference (transient provider URL — see Section 3.3 for the gap).
3. Call `ProviderOutputRetrievalService.retrieveProviderOutput()` → get bytes.
4. On retrieval failure → set `STORAGE_FAILED` + safe error → return.
5. Call `ImageStorageHandoffService.executeHandoff()` with retrieved bytes → validate → store → metadata update.
6. Return `ImageStorageHandoffResult` (safe DTO).

**Important:** The existing `ImageStorageHandoffRequest` already accepts `imageBytes`. The retrieval step produces the bytes that are then passed to the existing handoff flow.

**Provider Image URL Resolution:**

As documented in Section 3.3, `ImageProviderGenerationResult` currently has **no field** for the provider output URL. FP-009 engineering must decide how to obtain this URL at handoff time:

**Option A — Immediate handoff after generation (preferred):**
- Extend `ImageProviderGenerationResult` with a transient `providerOutputUrl` field (internal-only).
- Modify `OpenAiImageGenerationProviderGateway.extractSafeResult()` to extract the image URL from the OpenAI API response.
- After `ImageGenerationOrchestrationService.executeGeneration()` completes with a successful result, the transient URL is immediately passed to the handoff/retrieval service.
- No persistence of the URL to the database.
- Tight coupling to generation timing — handoff must happen before URL TTL expires.

**Option B — Transient persistence of provider output reference:**
- After generation, the provider output URL is temporarily stored in a new internal-only field on `ImageGenerationResult` entity (e.g., `providerOutputUrl`, new DB column, never in DTO).
- At handoff time, the URL is read, used for retrieval, and then **cleared/nulled** in the database.
- Requires V12 migration for the new column, careful DTO exclusion guardrails, and cleanup logic.
- More complex but allows delayed handoff (within provider URL TTL).

**Recommendation:** Option A is safer (no persistence risk). If timing constraints require Option B, the field must be:
- Internal-only (never in DTO/log/header).
- Cleared immediately after successful retrieval.
- Subject to TTL expiry handling.

The final decision is deferred to the coding task phase.

### 4.7 Component F — ImagePreviewProxyService [FP-008 — Unchanged]

**Package:** `com.anadoluatlasi.backend.imagegen.service` (existing)

**No code changes required.** The existing preview proxy reads from `ImageStorageAdapter`. When the adapter switches from `InMemoryImageStorageAdapter` to `S3ImageStorageAdapter`, the preview proxy automatically reads from real storage through the same interface.

| Aspect | FP-008 | FP-009 |
|--------|--------|--------|
| Read source | InMemoryImageStorageAdapter | S3ImageStorageAdapter (or InMemory in dev) |
| Response format | Image bytes stream | Image bytes stream (unchanged) |
| Storage key exposure | ⛔ Never | ⛔ Never (unchanged) |
| Signed/public URL | ⛔ Never | ⛔ Never (unchanged) |
| Provider URL | ⛔ Never | ⛔ Never (unchanged) |
| Base64 | ⛔ Never | ⛔ Never (unchanged) |

### 4.8 Component G — Admin DTO Mapper / Safe Response Boundary [FP-008 — Minimal Change]

**Package:** `com.anadoluatlasi.backend.imagegen.service` (existing mapper), `com.anadoluatlasi.backend.imagegen.controller.dto` (existing DTOs)

**Existing Safe Mapping Rules — Preserved:**

| DTO Field | Source | Admin Visible |
|-----------|--------|---------------|
| `storageHandoffStatus` | `ImageMediaAsset.storageHandoffStatus` | ✅ |
| `previewStatus` | `ImageMediaAsset.previewStatus` | ✅ |
| `storageErrorCode` | `ImageMediaAsset.storageErrorCode` | ✅ |
| `storageErrorMessage` | `ImageMediaAsset.storageErrorMessage` | ✅ (sanitized) |
| `storedAt` | `ImageMediaAsset.storedAt` | ✅ |
| `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256` | `ImageMediaAsset` | ✅ |

**Forbidden DTO Fields — Preserved:**

- `storageObjectKey` — ⛔ Never in DTO
- `storageProvider` — ⛔ Never in DTO
- `providerImageUrl` — ⛔ Never in DTO
- `signedUrl` / `publicUrl` / `cdnUrl` — ⛔ Never
- `base64` — ⛔ Never
- `rawProviderPayload` / `rawStoragePayload` — ⛔ Never
- `apiKey` / `secret` — ⛔ Never

**Potential DTO Extension (if needed):**

If new retrieval-specific error codes are added to `ImageStorageErrorCode`, they are automatically supported by the existing DTO mapping since `storageErrorCode` already maps the enum. No new DTO fields should be required.

---

## 5. Data Flow Design

### Step-by-Step Flow

#### Step 1 — Image Generation Metadata Exists (FP-007 Baseline)

| Aspect | Detail |
|--------|--------|
| **Input** | Generation request from admin |
| **Output** | `ImageGenerationResult` with `NormalizedImageGenerationResult` containing `providerImageUrl` (transient) |
| **Sensitive data** | `providerImageUrl` — internal, not persisted, not logged |
| **Failure** | Generation failure → `GENERATION_FAILED` status |

#### Step 2 — Admin/Backend Triggers Controlled Storage Handoff

| Aspect | Detail |
|--------|--------|
| **Input** | `POST .../generated-assets/{id}/storage-handoff` (admin action) |
| **Output** | Handoff process initiated |
| **Sensitive data** | None at this boundary — admin sends only asset ID |
| **Failure** | Asset not found → 404, asset not in correct state → 409/400 |

#### Step 3 — Handoff Service Loads Media Asset Context

| Aspect | Detail |
|--------|--------|
| **Input** | `generatedAssetId` |
| **Output** | Loaded `ImageGeneratedAsset` + linked `ImageMediaAsset` + provider output reference |
| **Sensitive data** | `providerImageUrl` resolved internally |
| **Failure** | Asset not found / media not linked → safe error code |

#### Step 4 — Provider Output Retrieval Service Resolves Output

| Aspect | Detail |
|--------|--------|
| **Input** | `ProviderOutputRetrievalRequest` with `providerImageUrl` (internal) |
| **Output** | `ProviderOutputRetrievalResult` with `imageBytes` (internal) |
| **Sensitive data that must not cross boundary** | `providerImageUrl` must not appear in result, logs, DTOs, or error messages |
| **Failure** | Timeout → `PROVIDER_RETRIEVAL_FAILED`, 404/410 → `PROVIDER_OUTPUT_EXPIRED`, other → safe error |

#### Step 5 — Provider Client Downloads Image Bytes

| Aspect | Detail |
|--------|--------|
| **Input** | Transient provider URL, HTTP client with timeout/retry config |
| **Output** | Raw image bytes + HTTP metadata (content-type, content-length) |
| **Sensitive data that must not cross boundary** | Provider URL, response headers, raw HTTP error detail |
| **Failure** | Network/HTTP failure → retry (up to max attempts) → safe error code |

#### Step 6 — Validation Service Validates Image

| Aspect | Detail |
|--------|--------|
| **Input** | `byte[] imageBytes` from retrieval |
| **Output** | `ImageMediaValidationResult` (valid/invalid, MIME, size, dimensions, checksum) |
| **Sensitive data that must not cross boundary** | Raw bytes must not appear in logs or error messages |
| **Failure** | Invalid MIME → `UNSUPPORTED_MEDIA_TYPE`, too large → `MEDIA_TOO_LARGE`, corrupt → `MEDIA_VALIDATION_FAILED` |

#### Step 7 — Private Storage Adapter Writes Object

| Aspect | Detail |
|--------|--------|
| **Input** | `ImageStorageWriteRequest` with validated `imageBytes`, generated `storageKey`, `mimeType` |
| **Output** | `ImageStorageWriteResult` with `success`, `storageKey` (internal), `providerAlias` (internal), `storedAt` |
| **Sensitive data that must not cross boundary** | `storageKey` stays internal — never in DTO/header/log |
| **Failure** | S3 SDK error → `STORAGE_WRITE_FAILED` (sanitized) |

#### Step 8 — Domain Updates Safe Storage/Preview Statuses

| Aspect | Detail |
|--------|--------|
| **Input** | Handoff result from step 7 |
| **Output** | `ImageMediaAsset` updated: `storageHandoffStatus=STORAGE_STORED`, `previewStatus=PREVIEW_AVAILABLE`, safe metadata fields (mimeType, width, height, fileSizeBytes, checksumSha256, storedAt, storageProvider, storageObjectKey — last two internal only) |
| **Sensitive data** | `storageProvider` and `storageObjectKey` persisted internally but never exposed in DTO |
| **Failure** | On any prior failure: `STORAGE_FAILED` + `PREVIEW_UNAVAILABLE` + safe error code/message |

#### Step 9 — Admin Receives Safe Status Response

| Aspect | Detail |
|--------|--------|
| **Input** | `ImageStorageHandoffResult` from handoff service |
| **Output** | `StorageHandoffResponse` safe DTO to admin |
| **Sensitive data that must not cross boundary** | Storage key, provider URL, bucket name, raw errors — never in response |
| **Failure** | Safe error code + sanitized message |

#### Step 10 — Admin Previews Through Backend Preview Proxy

| Aspect | Detail |
|--------|--------|
| **Input** | `GET .../generated-assets/{id}/preview` |
| **Output** | Image bytes stream (Content-Type + binary body) |
| **Sensitive data that must not cross boundary** | Storage key, signed URL, public URL, provider URL, base64 |
| **Failure** | Preview unavailable → safe error response |

#### Step 11 — Human Review Remains Separate and Mandatory

| Aspect | Detail |
|--------|--------|
| **Input** | Admin explicit approve/reject action |
| **Output** | Review audit record, updated review status |
| **Rule** | Storage success ≠ approval. Preview access ≠ approval. No automatic approval. |

---

## 6. Persistence and Schema Strategy

### Reuse FP-008 Fields

The existing `ImageMediaAsset` storage fields from FP-008 are **sufficient** for FP-009:

| Field | Present | Used By |
|-------|---------|---------|
| `storageHandoffStatus` | ✅ V11 | Storage status tracking |
| `previewStatus` | ✅ V11 | Preview availability |
| `storageErrorCode` | ✅ V11 | Safe error code |
| `storageErrorMessage` | ✅ V11 | Safe error message |
| `storedAt` | ✅ V11 | Storage timestamp |
| `storageProvider` | ✅ Entity | Internal provider alias (e.g., "s3") |
| `storageObjectKey` | ✅ Entity | Internal object key |
| `mimeType`, `width`, `height`, `fileSizeBytes`, `checksumSha256` | ✅ Entity | Validated media metadata |

### V12 Migration Candidates

If `storage_provider` and `storage_object_key` columns are not present in the database (they are on the entity but not in V11 migration), a V12 migration is needed:

```sql
-- Proposed V12 (if columns are missing from fp007_image_media_assets)
ALTER TABLE fp007_image_media_assets ADD COLUMN storage_provider VARCHAR(50);
ALTER TABLE fp007_image_media_assets ADD COLUMN storage_bucket VARCHAR(255);
ALTER TABLE fp007_image_media_assets ADD COLUMN storage_object_key TEXT;
```

This must be verified during coding task phase and created only if needed.

### Error Code Assessment

The existing `ImageStorageErrorCode` enum already contains most retrieval-specific codes:

| Existing Value | Trigger | Status |
|----------------|---------|--------|
| `PROVIDER_OUTPUT_UNAVAILABLE` | Provider output reference invalid or missing | ✅ Already exists |
| `PROVIDER_OUTPUT_EXPIRED` | Provider URL TTL expired | ✅ Already exists |
| `PROVIDER_OUTPUT_DOWNLOAD_FAILED` | Network/HTTP download failure | ✅ Already exists |
| `MEDIA_VALIDATION_FAILED` | General validation failure | ✅ Already exists |
| `UNSUPPORTED_MEDIA_TYPE` | MIME check failed | ✅ Already exists |
| `MEDIA_TOO_LARGE` | Size limit exceeded | ✅ Already exists |
| `STORAGE_WRITE_FAILED` | Storage write error | ✅ Already exists |

Potential new value:

| Proposed New Value | Trigger | Status |
|--------------------|---------|--------|
| `INTERNAL_CONFIGURATION_ERROR` | Storage provider config missing/invalid | ⬜ New — proposed |

Enum modifications are made during coding tasks, not in this document.

### Safe Persisted Concepts

| Concept | Allowed |
|---------|---------|
| Retrieval/validation/storage status category | ✅ Via existing `ImageStorageHandoffStatus` and `ImageStorageErrorCode` |
| Preview availability/status | ✅ Via existing `ImagePreviewStatus` |
| Safe failure category/code | ✅ Via existing `ImageStorageErrorCode` + `storageErrorMessage` |
| Internal storage reference (key, provider) | ✅ Internal only — never in DTO |
| Content type, byte size, dimensions, checksum | ✅ Safe metadata |
| Timestamps (storedAt, etc.) | ✅ Safe |

### Forbidden Persisted Concepts

| Concept | Status |
|---------|--------|
| Provider URL / temporary provider URL | ⛔ Never persisted |
| Signed provider URL | ⛔ Never persisted |
| Storage signed URL | ⛔ Not generated |
| Public URL | ⛔ Not generated |
| Base64 body | ⛔ Never persisted |
| Raw provider response JSON | ⛔ Never persisted |
| Raw storage response JSON | ⛔ Never persisted |
| Auth headers / tokens / API keys / config secrets | ⛔ Never persisted in domain tables |

---

## 7. Endpoint / API Design

### Endpoint Strategy

No endpoints are implemented in this task. The following defines the intended direction for coding tasks:

#### Storage Handoff Endpoint — 501 → Active

| Aspect | Current (FP-008) | Future (FP-009 Coding) |
|--------|-------------------|------------------------|
| Path | `POST .../generated-assets/{id}/storage-handoff` | Same path |
| Status | 501 Not Implemented | Active — triggers retrieval → validation → storage |
| Precondition | N/A | Asset in `GENERATED_PENDING_REVIEW` state |
| Response | `StorageHandoffResponse` (error) | `StorageHandoffResponse` (success or safe error) |
| Side effect | None | Downloads, validates, stores image; updates metadata |
| Automatic approval | ❌ | ❌ — storage success ≠ approval |

#### Preview Endpoint — Unchanged

| Aspect | Current (FP-008) | Future (FP-009) |
|--------|-------------------|-----------------|
| Path | `GET .../generated-assets/{id}/preview` | Same path |
| Status | Active | Active (unchanged) |
| Response | Image bytes stream | Image bytes stream (unchanged) |
| Storage source | `InMemoryImageStorageAdapter` | `S3ImageStorageAdapter` or `InMemoryImageStorageAdapter` |

#### Forbidden Endpoints

| Endpoint | Status |
|----------|--------|
| Generic status update (`PATCH /status`) | ⛔ Not introduced |
| READY_FOR_PUBLISH | ⛔ Not introduced |
| COMPLETED | ⛔ Not introduced |
| Publish/export | ⛔ Not introduced |
| `GET .../signed-url` | ⛔ Not introduced |
| `GET .../public-url` | ⛔ Not introduced |
| `GET .../storage-key` | ⛔ Not introduced |
| `POST .../base64` | ⛔ Not introduced |
| Bulk retrieval | ⛔ Not introduced |

### Forbidden API Outputs

All API responses must never contain:

- Provider URL / provider download URL
- Storage object key
- Public URL / signed URL / CDN URL
- Base64 image data
- Raw provider payload / response
- Raw storage payload / response
- Secrets / API keys / config values
- Internal stack traces

---

## 8. DTO and Response Safety Design

### Allowed Admin DTO Fields

| Field | Source | Safe |
|-------|--------|------|
| `generatedAssetId` | Path parameter / entity | ✅ |
| `mediaAssetId` | Entity | ✅ |
| `storageHandoffStatus` | `ImageMediaAsset.storageHandoffStatus` | ✅ |
| `previewStatus` | `ImageMediaAsset.previewStatus` | ✅ |
| `storageErrorCode` | `ImageMediaAsset.storageErrorCode` | ✅ |
| `storageErrorMessage` | `ImageMediaAsset.storageErrorMessage` (sanitized) | ✅ |
| `mimeType` | Validated | ✅ |
| `width` / `height` | Validated | ✅ |
| `fileSizeBytes` | Validated | ✅ |
| `checksumSha256` | Validated | ✅ |
| `storedAt` | Storage timestamp | ✅ |
| `correlationId` | Trace ID | ✅ |
| `success` | Boolean | ✅ |
| `reviewStatus` | Entity | ✅ |

### Forbidden Admin DTO Fields

| Field | Status | Reason |
|-------|--------|--------|
| `providerImageUrl` / `providerUrl` / `providerDownloadUrl` | ⛔ | Security |
| `providerRawResponse` | ⛔ | Provider security |
| `providerHeaders` / `providerRequestData` | ⛔ | Provider security |
| `storageObjectKey` / `storageReference` | ⛔ | Internal infrastructure |
| `storageBucket` / `storageProvider` | ⛔ | Internal infrastructure |
| `signedUrl` / `publicUrl` / `cdnUrl` | ⛔ | Not generated |
| `base64` / `imageBase64` / `imageBinary` | ⛔ | Performance + security |
| `rawStorageResponse` | ⛔ | Infrastructure security |
| `apiKey` / `secret` / `internalPrompt` | ⛔ | Secret management |
| Stack trace | ⛔ | Security |

### Status Independence Rules

| Rule | Constraint |
|------|-----------|
| Stored does not mean approved | ⛔ Independent concerns |
| Preview available does not mean approved | ⛔ Independent concerns |
| Retrieved does not mean validated | Separate steps |
| Validated does not mean stored | Separate steps |
| AI output must remain review-required until explicit human approval | ⛔ Mandatory |

---

## 9. Status / Error Mapping Design

### Status Categories (Contract-Level)

Based on FP-009 Task 02 contract categories, mapped to existing and proposed enums:

#### Retrieval Status → Mapped to `ImageStorageHandoffStatus`

| Conceptual | Engineering Mapping | Notes |
|------------|---------------------|-------|
| Retrieval Pending | `STORAGE_PENDING` | Combined with storage pending |
| Retrieval In Progress | `STORAGE_VALIDATING` | Reuse validating state |
| Retrieval Failed | `STORAGE_FAILED` + error code | Failure with retrieval-specific error |
| Retrieved | (transient) | Not a persisted state — bytes immediately proceed to validation |

#### Validation Status → Existing `ImageMediaValidationResult`

| Conceptual | Engineering Mapping | Notes |
|------------|---------------------|-------|
| Validation Pending | (transient) | Not persisted |
| Validation Failed | `STORAGE_FAILED` + `MEDIA_VALIDATION_FAILED` / `UNSUPPORTED_MEDIA_TYPE` / `MEDIA_TOO_LARGE` | Existing codes |
| Validated | (transient) | Bytes proceed to storage |

#### Storage Status → Existing `ImageStorageHandoffStatus`

| Conceptual | Engineering Mapping | Notes |
|------------|---------------------|-------|
| Storage Pending | `STORAGE_PENDING` | Existing |
| Storage Failed | `STORAGE_FAILED` | Existing |
| Stored | `STORAGE_STORED` | Existing |

#### Preview Status → Existing `ImagePreviewStatus`

| Conceptual | Engineering Mapping | Notes |
|------------|---------------------|-------|
| Preview Unavailable | `PREVIEW_UNAVAILABLE` | Existing |
| Preview Available | `PREVIEW_AVAILABLE` | Existing |

#### Review Status → Existing `GeneratedAssetStatus` + `ImageMediaAssetReviewStatus`

| Conceptual | Engineering Mapping | Notes |
|------------|---------------------|-------|
| Review Required | `GENERATED_PENDING_REVIEW` | Existing |
| Approved | `APPROVED` / `REVIEW_APPROVED` | Existing |
| Rejected | `REJECTED` / `REVIEW_REJECTED` | Existing |

### Failure Error Code Mapping

| Contract Category | `ImageStorageErrorCode` Mapping | Notes |
|-------------------|--------------------------------|-------|
| PROVIDER_OUTPUT_UNAVAILABLE | `PROVIDER_OUTPUT_UNAVAILABLE` (**existing**) | Provider ref invalid/missing |
| PROVIDER_RETRIEVAL_FAILED | `PROVIDER_OUTPUT_DOWNLOAD_FAILED` (**existing**) | Network/HTTP failure |
| Provider TTL expired | `PROVIDER_OUTPUT_EXPIRED` (**existing**) | HTTP 404/410 from provider |
| UNSUPPORTED_MEDIA_TYPE | `UNSUPPORTED_MEDIA_TYPE` (**existing**) | MIME check failed |
| FILE_TOO_LARGE | `MEDIA_TOO_LARGE` (**existing**) | Size limit exceeded |
| VALIDATION_FAILED | `MEDIA_VALIDATION_FAILED` (**existing**) | General validation failure |
| STORAGE_WRITE_FAILED | `STORAGE_WRITE_FAILED` (**existing**) | Storage write error |
| PREVIEW_UNAVAILABLE | `PREVIEW_NOT_AVAILABLE` (**existing**) | Preview not ready |
| INTERNAL_CONFIGURATION_UNAVAILABLE | `INTERNAL_CONFIGURATION_ERROR` (**new — proposed**) | Config missing/invalid |

### Rules

- Most error codes already exist in `ImageStorageErrorCode` — only `INTERNAL_CONFIGURATION_ERROR` is a proposed new value.
- Enums are not created/modified in this task — documented as proposed extensions only.
- Safe admin messages only — via `ImageStorageErrorNormalizer.safeMessage()`.
- Internal technical details must not be admin-facing.
- Logs must be sanitized via `ImageStorageErrorNormalizer.sanitize()`.

---

## 10. Logging, Audit, and Observability Design

### Safe Logging Rules

| Action | What to Log | What Must Not Be Logged |
|--------|-------------|-------------------------|
| Retrieval attempt | `generatedAssetId`, `mediaAssetId`, `correlationId`, timestamp | Provider URL, auth token, raw HTTP response |
| Retrieval success | `correlationId`, content length, detected content type | Provider URL |
| Retrieval failure | `correlationId`, safe error code, sanitized error message | Provider URL, raw HTTP error detail |
| Validation result | `correlationId`, valid/invalid, detected MIME, size, dimensions | Raw bytes, base64, provider URL |
| Storage write | `correlationId`, success/failure, safe error code | Storage key, bucket name, raw SDK error, credentials |
| Preview access | `generatedAssetId`, `correlationId`, access timestamp | Storage key, signed URL, raw storage read details |

### Sanitization Requirements

All error messages crossing service/API boundary must be sanitized via `ImageStorageErrorNormalizer`:

| Pattern | Mask |
|---------|------|
| HTTP/HTTPS URLs | `[URL_REDACTED]` |
| S3/GS/AZ URIs | `[STORAGE_REDACTED]` |
| Storage key patterns | `[STORAGE_KEY_REDACTED]` |
| API keys, secrets, tokens, bearer | `[REDACTED]` |
| Base64 data | `[DATA_REDACTED]` |
| Long alphanumeric strings (64+ chars) | `[LONG_DATA_REDACTED]` |
| Stack trace lines | `[STACK_REDACTED]` |

### Observability

| Metric | Description |
|--------|-------------|
| Retrieval duration | Time from retrieval start to bytes received |
| Retrieval success rate | Success vs failure ratio |
| Validation duration | Validation processing time |
| Storage write duration | Time to write to object store |
| Storage read duration | Preview proxy read time |
| Failure category distribution | Which error codes occur most |

---

## 11. Security Design

### 11.1 Provider Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Provider output reference (URL) from `NormalizedImageGenerationResult` |
| **Data exiting** | Image bytes (internal, to validation) |
| **Must never exit** | Provider URL, auth tokens, raw response, API keys |
| **Owning component** | `ProviderOutputClient`, `ProviderOutputRetrievalService` |

### 11.2 Backend Retrieval Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Provider output reference (internal), HTTP client config |
| **Data exiting** | Downloaded bytes (to validation), safe retrieval status/error |
| **Must never exit to admin** | Provider URL, response headers, raw HTTP errors, download URL |
| **Owning component** | `ProviderOutputRetrievalService` |

### 11.3 Validation Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Image bytes from retrieval |
| **Data exiting** | Validation result (MIME, size, dimensions, checksum, valid/invalid) |
| **Must never exit to admin** | Raw bytes, base64, internal validation errors beyond safe category |
| **Owning component** | `ImageMediaValidationService` (existing) |

### 11.4 Private Storage Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Validated bytes, server-side generated storage key, MIME type |
| **Data exiting** | Storage status (success/failure), stored timestamp, safe error |
| **Must never exit to admin** | Storage key, bucket name, credentials, raw SDK response, signed/public URL |
| **Owning component** | `S3ImageStorageAdapter` (new), `InMemoryImageStorageAdapter` (existing) |

### 11.5 Preview Proxy Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Asset ID (from admin request), authorization context |
| **Data exiting to admin** | Image bytes stream (Content-Type + binary body) |
| **Must never exit** | Storage key, signed URL, public URL, provider URL, base64, storage metadata |
| **Owning component** | `ImagePreviewProxyService` (existing, unchanged) |

### 11.6 Admin API Boundary

| Aspect | Detail |
|--------|--------|
| **Data exiting to admin** | Safe status categories, safe metadata, safe error summaries, asset IDs, correlation IDs |
| **Must never exit** | Provider URL, storage key, signed/public URL, base64, raw payloads, secrets, stack traces |
| **Owning component** | `AdminImageGenerationController`, `ImageMediaAssetMapper`, `StorageHandoffResponse` DTO |

### 11.7 Human Review Boundary

| Aspect | Detail |
|--------|--------|
| **Data entering** | Admin explicit approve/reject, reject reason |
| **Data exiting** | Review audit record, updated review status |
| **Preservation** | No boundary produces automatic approval |
| **Independence** | Review is independent from retrieval, validation, and storage |
| **Owning component** | `ImageGeneratedAssetReviewService` (existing, unchanged) |

---

## 12. Testing Strategy

### Test Categories for Future Coding Tasks

All tests are defined here for planning. They are created during coding tasks, not in this document.

#### 12.1 Retrieval Service Tests

| Test | Description |
|------|-------------|
| Retrieval success — returns image bytes | Happy path with mock provider URL |
| Retrieval failure — timeout | Network timeout → `PROVIDER_RETRIEVAL_FAILED` |
| Retrieval failure — HTTP 404 | Provider URL expired → `PROVIDER_OUTPUT_EXPIRED` |
| Retrieval failure — HTTP 500 | Provider server error → `PROVIDER_RETRIEVAL_FAILED` |
| Retrieval failure — connection refused | Network unavailable → `PROVIDER_RETRIEVAL_FAILED` |
| Retrieval result does not contain provider URL | Field absence guardrail |
| Retrieval result does not contain raw provider response | Field absence guardrail |
| Retrieval result does not contain auth tokens | Field absence guardrail |
| Retrieval error message sanitized | No URL/token leakage |

#### 12.2 Provider Client Tests

| Test | Description |
|------|-------------|
| Download success | HTTP 200 → bytes returned |
| Download timeout | Read timeout → safe error |
| Download max size exceeded | Content-Length > 10 MB → `MEDIA_TOO_LARGE` |
| Retry on transient failure | 5xx → retry → eventual success or failure |
| Error sanitization | Raw HTTP error → sanitized message |

#### 12.3 Validation-Before-Storage Orchestration Tests

| Test | Description |
|------|-------------|
| Valid image → storage success | Retrieval OK → validation pass → storage write OK |
| Invalid MIME → storage failure | Retrieval OK → validation fail (MIME) → `STORAGE_FAILED` |
| Oversized file → storage failure | Retrieval OK → validation fail (size) → `STORAGE_FAILED` |
| Corrupt image → storage failure | Retrieval OK → validation fail (corrupt) → `STORAGE_FAILED` |
| Storage write failure | Retrieval OK → validation pass → write fail → `STORAGE_FAILED` |
| Validation failure does not auto-reject | Assert `GeneratedAssetStatus` unchanged |

#### 12.4 Storage Adapter Tests

| Test | Description |
|------|-------------|
| S3 adapter write success | Write bytes → success |
| S3 adapter read success | Write then read → bytes match |
| S3 adapter write failure | Simulated SDK error → safe error code |
| S3 adapter does not generate public URL | No `publicUrl` field in result |
| S3 adapter does not generate signed URL | No `signedUrl` field in result |
| InMemory adapter still works | Existing tests continue passing |

#### 12.5 DTO Mapper / Safety Guardrail Tests

| Test | Description |
|------|-------------|
| `StorageHandoffResponse` does not contain `storageObjectKey` | Field absence |
| `StorageHandoffResponse` does not contain `providerUrl` | Field absence |
| `StorageHandoffResponse` does not contain `signedUrl` / `publicUrl` | Field absence |
| `StorageHandoffResponse` does not contain `base64` | Field absence |
| `ImageMediaAssetResponse` does not contain `storageObjectKey` | Field absence |
| `ImageMediaAssetResponse` does not contain `providerUrl` | Field absence |

#### 12.6 Preview Proxy Safety Tests

| Test | Description |
|------|-------------|
| Preview reads from real storage adapter | Integration with S3 adapter |
| Preview does not expose storage key | Header/body check |
| Preview does not expose signed URL | Header/body check |
| Preview does not change asset status | Idempotent |

#### 12.7 Integration Tests

| Test | Description |
|------|-------------|
| End-to-end: retrieval → validation → storage → preview | Full flow with mock provider output |
| End-to-end: retrieval failure → safe error response | Failure path |
| End-to-end: validation failure → safe error response | Failure path |
| End-to-end: storage failure → safe error response | Failure path |

#### 12.8 Negative / Regression Tests

| Test | Description |
|------|-------------|
| No READY_FOR_PUBLISH status | Enum/code absence check |
| No COMPLETED status | Enum/code absence check |
| No generic status update endpoint | Endpoint absence check |
| No publish/export endpoint | Endpoint absence check |
| No automatic approval after storage success | Status unchanged after handoff |
| No automatic approval after preview access | Status unchanged after preview |
| AI output remains review-required | `GENERATED_PENDING_REVIEW` after handoff |

### Existing Baseline Requirement

- **Existing 217/217 test baseline must remain passing during all future coding tasks.**
- Each coding task should run the full test suite and report results.
- No test changes are made in this documentation task.

---

## 13. Phased Implementation Plan

### Proposed Future Coding Tasks

| Task | Title | Scope |
|------|-------|-------|
| **Task 04** | Coding Task Breakdown Document | Documentation — detailed coding task descriptions |
| **Task 05** | Provider Output Retrieval Interfaces & Models | New `ProviderOutputRetrievalRequest`, `ProviderOutputRetrievalResult`, `ProviderOutputClient` interface. Mock client implementation only. New `ImageStorageErrorCode` values for retrieval failures. |
| **Task 06** | Provider Output Retrieval Service Implementation | `ProviderOutputRetrievalService` with real HTTP client (`ProviderOutputClient` impl). Timeout, retry, error sanitization. Unit tests for retrieval success/failure paths. |
| **Task 07** | S3-Compatible Storage Adapter | `S3ImageStorageAdapter` implementing `ImageStorageAdapter`. Configuration properties for S3/MinIO. Conditional adapter activation. V12 migration if `storage_provider`/`storage_object_key` columns missing. Docker Compose MinIO for local dev. |
| **Task 08** | Storage Handoff Endpoint Activation | `ImageGeneratedAssetHandoffService` orchestration: retrieve → validate → store → metadata update. Controller endpoint activation from 501 to real handoff. Provider URL resolution strategy implementation. |
| **Task 09** | Preview Proxy Regression Hardening | Verify preview proxy works with real S3 storage adapter. Preview safety guardrails maintained. Integration tests. |
| **Task 10** | Admin Safe DTO/Status/Failure Response Hardening | Extend `ImageStorageErrorNormalizer` with new retrieval error codes. Admin DTO guardrail tests for new fields. Safe error message mapping for all new failure categories. |
| **Task 11** | Comprehensive Tests & Regression Suite | Full test suite validation. All guardrail tests. All negative tests. All safety tests. Ensure no READY_FOR_PUBLISH, COMPLETED, generic status, auto-approval leakage. |
| **Task 12** | Final Closure / Operational Baseline | Documentation — closure doc, final test count, baseline snapshot. |

### Task Dependencies

```
Task 04 (doc) ─┐
               ├─▶ Task 05 (interfaces) ─▶ Task 06 (retrieval impl)
               │                                      │
               │                                      ▼
               ├─▶ Task 07 (S3 adapter) ──────▶ Task 08 (handoff activation)
               │                                      │
               │                                      ▼
               └─▶ Task 09 (preview) ──────────▶ Task 10 (DTO hardening)
                                                      │
                                                      ▼
                                                Task 11 (tests)
                                                      │
                                                      ▼
                                                Task 12 (closure)
```

### Task Sizing Principle

- Each task is small, safe, and independently reviewable.
- Each task produces passing tests.
- Each task preserves the existing 217/217 baseline.
- No task introduces forbidden concepts (public URL, auto-approval, publish, etc.).

---

## 14. Non-Goals and Hard Prohibitions

### Explicit Non-Goals

| # | Non-Goal |
|---|----------|
| 1 | Provider URL persistence |
| 2 | Provider URL DTO exposure |
| 3 | Provider URL logging |
| 4 | Storage key admin/user exposure |
| 5 | Public URL generation |
| 6 | Signed URL returned to admin/user |
| 7 | Base64 in DB/DTO/logs |
| 8 | Raw provider/storage payload exposure |
| 9 | Secret/API key/config exposure |
| 10 | Automatic approval |
| 11 | READY_FOR_PUBLISH status |
| 12 | COMPLETED status |
| 13 | Generic status update endpoint |
| 14 | Publish/export |
| 15 | Video/audio |
| 16 | Bulk retrieval |
| 17 | CDN / public delivery |
| 18 | User-facing media library |
| 19 | Destructive migration / data backfill |
| 20 | Broad multi-provider redesign beyond controlled image retrieval/storage |
| 21 | Background worker / orchestration redesign |
| 22 | Image editing / cropping / resizing |
| 23 | Regeneration / variation flow |
| 24 | Multi-provider failover |
| 25 | Provider cost optimization |

---

## 15. Acceptance Criteria

| # | Criterion | Status |
|---|-----------|--------|
| 1 | Engineering Design document exists | ✅ |
| 2 | References FP-009 Product Design | ✅ |
| 3 | References FP-009 AI Platform / Media Contract Design | ✅ |
| 4 | References FP-008 closure baseline (commit `a36eba8`, 217/217 tests) | ✅ |
| 5 | Defines proposed backend components (retrieval service, client, S3 adapter, handoff enhancement) | ✅ |
| 6 | Defines retrieval → validation → private storage → preview flow | ✅ |
| 7 | Defines persistence/schema strategy without implementing migrations | ✅ |
| 8 | Defines endpoint/API strategy without introducing generic status update | ✅ |
| 9 | Defines safe DTO boundaries | ✅ |
| 10 | Defines status/error mapping | ✅ |
| 11 | Defines logging/audit/security boundaries | ✅ |
| 12 | Defines test strategy for future coding tasks | ✅ |
| 13 | Defines phased implementation plan | ✅ |
| 14 | Prohibits provider URL persistence/exposure/logging | ✅ |
| 15 | Prohibits storage key exposure | ✅ |
| 16 | Prohibits public/signed URL exposure | ✅ |
| 17 | Prohibits base64 in DB/DTO/logs | ✅ |
| 18 | Prohibits raw provider/storage payload exposure | ✅ |
| 19 | Prohibits secrets/API key/config exposure | ✅ |
| 20 | Preserves backend-controlled preview proxy | ✅ |
| 21 | Preserves human review gate | ✅ |
| 22 | Confirms AI output is never automatically APPROVED | ✅ |
| 23 | Confirms storage success does not equal approval | ✅ |
| 24 | Confirms preview availability does not equal approval | ✅ |
| 25 | Confirms READY_FOR_PUBLISH is out of scope | ✅ |
| 26 | Confirms COMPLETED is out of scope | ✅ |
| 27 | Confirms generic status update endpoint is out of scope | ✅ |
| 28 | Confirms publish/export is out of scope | ✅ |
| 29 | Confirms video/audio is out of scope | ✅ |
| 30 | Contains no code implementation | ✅ |

---

## 16. Next Document

### FP-009 Task 04 — Coding Task Breakdown

Task 04 should convert this engineering design into small, safe, reviewable coding tasks, preserving the documentation-first workflow and all FP-009 safety constraints.

Each coding task should:

- Be independently implementable and testable.
- Maintain the existing 217/217 test baseline.
- Produce incremental passing test count.
- Never introduce forbidden concepts.
- Be reviewed before proceeding to the next task.

### Workflow Status

1. ✅ **Product Design** — FP-009 Task 01 (completed)
2. ✅ **AI Platform / Media Contract Design** — FP-009 Task 02 (completed)
3. ✅ **Engineering Design** — FP-009 Task 03 ← this document
4. ⬜ **Coding Task Breakdown** — FP-009 Task 04
5. ⬜ **Code Implementation** — FP-009 Task 05+

---

*Bu doküman FP-009 Task 03 kapsamında oluşturulmuştur. Documentation-only — kod, config, migration, test, DTO, enum, entity, controller, service veya runtime değişikliği yoktur.*
