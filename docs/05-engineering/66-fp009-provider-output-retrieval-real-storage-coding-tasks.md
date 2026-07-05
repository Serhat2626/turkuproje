# FP-009 — Provider Output Retrieval & Real Storage Provider Integration — Coding Task Breakdown

---

## 1. Document Status

| Item | Detail |
|------|--------|
| **Feature** | FP-009 — Provider Output Retrieval & Real Storage Provider Integration |
| **Document type** | Coding Task Breakdown |
| **Status** | **PLANNED** |
| **Task** | FP-009 Task 04 — Coding Task Breakdown |
| **Task scope** | Documentation-only — creates the plan for future coding tasks |
| **Product Design** | [63-fp009-provider-output-retrieval-real-storage-product-design.md](../03-product-design/63-fp009-provider-output-retrieval-real-storage-product-design.md) |
| **AI Platform / Media Contract** | [64-fp009-provider-output-retrieval-real-storage-media-contract-design.md](../04-ai-platform/64-fp009-provider-output-retrieval-real-storage-media-contract-design.md) |
| **Engineering Design** | [65-fp009-provider-output-retrieval-real-storage-engineering-design.md](./65-fp009-provider-output-retrieval-real-storage-engineering-design.md) |
| **FP-008 Closure Doc** | [62-fp008-final-closure-operational-baseline.md](./62-fp008-final-closure-operational-baseline.md) |
| **Source baseline** | FP-008 — CLOSED (commit `a36eba8`, 217/217 tests passing) |
| **Closed predecessor chain** | FP-001 → FP-002 → FP-003 → FP-004 → FP-005 → FP-006 → FP-007 → FP-008 |

> **This is a documentation-only task.** No production code, configuration, migration, test, DTO, enum, entity, controller, service, or runtime changes are made. This document defines the plan for future coding tasks.

---

## 2. Baseline Summary

### 2.1 Existing FP-008 Baseline

| Component | Status |
|-----------|--------|
| Storage/preview status model (`ImageStorageHandoffStatus`, `ImagePreviewStatus`) | ✅ Exists |
| `ImageMediaAsset` storage fields (V11 migration) | ✅ Exists (5 columns + 2 indexes) |
| `ImageMediaValidationService` (MIME sniffing, 10 MB, dimensions, SHA-256) | ✅ Exists |
| `InMemoryImageStorageAdapter` (mock/private storage) | ✅ Exists |
| `ImageStorageHandoffService` (validate → store → metadata update) | ✅ Exists |
| `ImagePreviewProxyService` (backend-controlled byte streaming) | ✅ Exists |
| `ImageStorageErrorNormalizer` (sanitization) | ✅ Exists |
| `ImageStorageErrorCode` (13 values, most retrieval codes already present) | ✅ Exists |
| Storage handoff endpoint `POST .../storage-handoff` | ⏸️ Returns **501** |
| Real storage provider (S3-compatible) | ⬜ Deferred |
| Provider output download (image bytes retrieval) | ⬜ Deferred |
| Human review gate (`GENERATED_PENDING_REVIEW`, explicit approve/reject) | ✅ Preserved |
| Backend test baseline | ✅ **217/217 passing** |

### 2.2 Critical Provider URL Gap

**From Engineering Design Section 3.3:**

`OpenAiImageGenerationProviderGateway` calls the OpenAI API with `response_format: "url"`, but `extractSafeResult()` only reads `outputCount` from the response data — **the provider image URL is never captured, extracted, or returned**.

`ImageProviderGenerationResult` has **no field** for `providerImageUrl` or any provider output reference. The URL is completely lost after the API call.

### 2.3 Entity/Migration Gap

`ImageMediaAsset` entity defines `storageProvider`, `storageBucket`, and `storageObjectKey` fields, but these columns were **not** added in V11 migration. Table name is `fp007_image_media_assets`. A V12 migration is likely required.

### 2.4 Configuration Gap

No storage-specific configuration exists. `InMemoryImageStorageAdapter` is registered as `@Component` unconditionally with no config-driven adapter selection.

---

## 3. Task Breakdown Principles

| # | Principle |
|---|-----------|
| 1 | **Small and reviewable** — one task must be small enough to review in a single session |
| 2 | **Clear acceptance criteria** — each task must define what "done" means |
| 3 | **Compiling baseline preserved** — every task must leave the project compiling and tests passing |
| 4 | **No broad refactors** — avoid touching unrelated code or restructuring modules |
| 5 | **Interface/contract first** — add interfaces and models before real behavior |
| 6 | **Mock/test-safe first** — mock flows before real provider/storage integration |
| 7 | **Safety boundaries before activation** — do not activate external behavior until tests cover safety boundaries |
| 8 | **Never expose sensitive fields** — provider URL, storage key, signed/public URL, base64, secrets must never appear in DTOs, logs, or error messages |
| 9 | **Never conflate storage with approval** — storage success ≠ approval, preview ≠ approval |
| 10 | **Report after each task** — changed files, tests run, safety confirmations |
| 11 | **Incremental test growth** — each task should add or maintain passing tests |
| 12 | **Explicit gap resolution** — the provider URL gap must be addressed with the safest minimal approach |

---

## 4. Provider URL Gap Resolution Strategy

### Analysis

The Engineering Design (Section 4.6) documents two options:

**Option A — Immediate handoff after generation (recommended):**
- Extend `ImageProviderGenerationResult` with a transient `providerOutputUrl` field (internal-only).
- Modify `OpenAiImageGenerationProviderGateway.extractSafeResult()` to extract the image URL.
- After generation completes, immediately pass the transient URL to the retrieval service.
- No persistence of the URL to the database.
- Tight coupling to generation timing — handoff must happen before URL TTL expires.

**Option B — Transient persistence of provider output reference:**
- Store URL temporarily in a new internal-only DB field on `ImageGenerationResult`.
- Read, use, and clear the URL at handoff time.
- Requires V12 migration, DTO exclusion guardrails, and cleanup logic.

### Decision

**Option A is selected for the coding task sequence.**

Rationale:
- No persistence risk — URL never touches the database.
- No migration required for the URL field.
- No DTO/log exposure risk from persisted data.
- Simpler implementation with fewer guardrails.
- Fits the existing flow: generation → immediate post-processing.

The trade-off (tight coupling to generation timing) is acceptable because:
- OpenAI image URLs have a TTL of ~60 minutes, which is long enough for immediate handoff.
- The handoff service runs synchronously after generation in the same request/transaction context.
- If delayed handoff becomes a requirement in the future, Option B can be evaluated later.

### Implementation Approach

1. Add a transient `providerOutputUrl` field to `ImageProviderGenerationResult` (internal model, no DB mapping, no DTO mapping).
2. Modify `OpenAiImageGenerationProviderGateway.extractSafeResult()` to extract the URL from the OpenAI response `data[0].url`.
3. In `ImageGenerationOrchestrationService.executeGeneration()`, after successful generation, pass the transient URL to the retrieval/handoff service.
4. The retrieval service downloads the image bytes from the URL.
5. After download, the URL is discarded (garbage collected with the result object).
6. The URL is never persisted, logged, or exposed in any DTO.

---

## 5. Recommended Task Sequence

### Task Overview

```
Task 05 ── Retrieval Contracts & Safe Result Models
   │
   ▼
Task 06 ── Mock Provider Retrieval + Validation-First Orchestration
   │
   ▼
Task 07 ── Provider URL Gap Resolution (OpenAI Gateway Fix)
   │
   ▼
Task 08 ── V12 Migration + S3 Storage Adapter Shell
   │
   ▼
Task 09 ── Retrieval → Validation → Storage Handoff Activation
   │
   ▼
Task 10 ── Preview Proxy Regression Hardening
   │
   ▼
Task 11 ── Admin DTO, Failure Message, Logging & Audit Hardening
   │
   ▼
Task 12 ── End-to-End Regression Tests & FP-009 Closure Candidate
```

---

### Task 05 — Provider Output Retrieval Contracts & Safe Result Model

**Purpose:**
Add internal contracts, interfaces, and result models for provider output retrieval. No real provider download yet. No endpoint activation. No storage integration. No provider URL persistence/exposure/logging.

**Expected Scope:**

| Area | Change |
|------|--------|
| `ProviderOutputRetrievalRequest` | New internal request model — `generatedAssetId`, `mediaAssetId`, `providerOutputUrl` (internal-only), `correlationId` |
| `ProviderOutputRetrievalResult` | New internal result model — `success`, `imageBytes` (internal), `detectedContentType`, `contentLength`, `errorCode`, `errorMessage`, `correlationId` |
| `ProviderOutputClient` interface | New internal interface — `download(String providerOutputUrl, String correlationId)` returning download result |
| `MockProviderOutputClient` | Mock implementation returning test image bytes |
| `ProviderOutputRetrievalService` | Service shell accepting request, delegating to client, returning safe result |
| Unit tests | Contract tests, sanitization tests, mock client tests |

**Files Likely to Inspect:**
- `com.anadoluatlasi.backend.imagegen.service` — existing services
- `com.anadoluatlasi.backend.imagegen.storage` — existing storage models
- `com.anadoluatlasi.backend.imagegen.provider` — existing provider models
- `com.anadoluatlasi.backend.imagegen.entity.ImageStorageErrorCode` — existing error codes

**Allowed Changes:**
- New internal service/model/interface classes in `imagegen.service` or `imagegen.provider`
- New test classes
- Compile verification

**Forbidden Changes:**
- ❌ Persist provider URL
- ❌ Expose provider URL in DTO/API/header
- ❌ Log provider URL
- ❌ Add real provider SDK calls
- ❌ Activate handoff endpoint
- ❌ Change existing entities/DTOs/controllers
- ❌ Create migrations
- ❌ Modify application config

**Expected Tests:**

| Test | Description |
|------|-------------|
| `ProviderOutputRetrievalResult` does not contain provider URL field | Guardrail — no `providerUrl` field on safe result |
| `MockProviderOutputClient` returns image bytes | Happy path |
| `MockProviderOutputClient` returns failure on null URL | Null input safety |
| `ProviderOutputRetrievalService` returns safe result on success | Orchestration happy path |
| `ProviderOutputRetrievalService` returns safe error on failure | Failure path |
| Error messages are sanitized | No URL leakage in error messages |
| Compile and existing 217 tests pass | Baseline preservation |

---

### Task 06 — Mock Provider Retrieval Flow + Validation-First Orchestration

**Purpose:**
Wire mock provider retrieval into the validation flow. Implement the retrieval → validation orchestration with mock provider output. Storage remains existing mock/InMemory baseline. Validate before storage success.

**Expected Scope:**

| Area | Change |
|------|--------|
| `ProviderOutputRetrievalService` | Full implementation with mock client — retrieve bytes → pass to validation |
| Orchestration logic | New orchestration method: retrieve → validate → (existing handoff if validation passes) |
| Validation integration | `ImageMediaValidationService.validate()` called with retrieved bytes |
| Unit tests | Validation success/failure, retrieval failure, validation-before-storage assertion |

**Files Likely to Inspect:**
- `ImageStorageHandoffService` — existing handoff flow
- `ImageMediaValidationService` — existing validation
- `ImageStorageHandoffRequest` — existing request model with `imageBytes`

**Allowed Changes:**
- New orchestration service/method
- Extend retrieval service implementation
- New test classes

**Forbidden Changes:**
- ❌ Use real provider URL
- ❌ Use real storage provider
- ❌ Expose sensitive fields in DTOs
- ❌ Mark output as approved
- ❌ Activate handoff endpoint
- ❌ Change existing entities/DTOs/controllers

**Expected Tests:**

| Test | Description |
|------|-------------|
| Mock retrieval success → validation pass → handoff call with bytes | Full happy path |
| Mock retrieval success → validation fail (bad MIME) → `STORAGE_FAILED` | Invalid MIME type |
| Mock retrieval success → validation fail (too large) → `STORAGE_FAILED` | Oversized file |
| Mock retrieval success → validation fail (corrupt) → `STORAGE_FAILED` | Corrupt image |
| Mock retrieval failure → `STORAGE_FAILED` + safe error | Retrieval failure |
| Validation failure does not change `GeneratedAssetStatus` | Review gate preserved |
| Storage success does not change `GeneratedAssetStatus` | Review gate preserved |
| Existing 217+ tests pass | Baseline preservation |

---

### Task 07 — Provider URL Gap Resolution (OpenAI Gateway Fix)

**Purpose:**
Safely address the OpenAI provider gateway gap where the returned image URL is currently lost. Implement Option A: immediate internal handoff without persistence.

**Expected Scope:**

| Area | Change |
|------|--------|
| `ImageProviderGenerationResult` | Add transient `providerOutputUrl` field (internal model, no DB column, no DTO mapping) |
| `OpenAiImageGenerationProviderGateway` | Modify `extractSafeResult()` to extract `data[0].url` from OpenAI response and set on result |
| `MockImageGenerationProviderGateway` | Return a mock `providerOutputUrl` for testing |
| `ImageGenerationOrchestrationService` | After successful generation, pass transient URL to retrieval/handoff path |
| Guardrail tests | Verify URL is not persisted, not in DTO, not logged |

**Decision: Option A (Immediate handoff, no persistence)**

- `providerOutputUrl` is added to `ImageProviderGenerationResult` as a transient in-memory field only.
- It is never mapped to a DB column.
- It is never included in any DTO.
- It is never logged.
- It is used once for retrieval and then discarded via garbage collection.

**Files Likely to Inspect:**
- `ImageProviderGenerationResult` — add transient field
- `OpenAiImageGenerationProviderGateway` — extract URL from response
- `MockImageGenerationProviderGateway` — return mock URL
- `ImageGenerationOrchestrationService` — wire URL to retrieval
- All DTO classes — verify no exposure
- All mapper classes — verify no mapping

**Allowed Changes:**
- Add field to `ImageProviderGenerationResult` (in-memory model, not entity)
- Modify provider gateway implementations
- Modify orchestration service
- New test classes

**Forbidden Changes:**
- ❌ Add provider URL entity/DB field
- ❌ Add provider URL DTO field
- ❌ Add provider URL to any log statement
- ❌ Add provider raw response exposure
- ❌ Add base64 persistence
- ❌ Create migrations
- ❌ Change DTOs

**Expected Tests:**

| Test | Description |
|------|-------------|
| `ImageProviderGenerationResult.providerOutputUrl` set after extraction | URL captured from response |
| `ImageProviderGenerationResult.providerOutputUrl` is not in any DTO | DTO mapping guardrail |
| `ImageGenerationResultResponse` does not contain provider URL field | DTO guardrail |
| Mock gateway returns mock URL | Mock provider test |
| Orchestration passes URL to retrieval service | Integration test |
| Provider URL not logged (where testable) | Log sanitization |
| Provider unavailable → safe failure | Error path |
| Raw provider payload not exposed | Raw response guardrail |
| Existing tests pass | Baseline preservation |

---

### Task 08 — V12 Migration + S3 Storage Adapter Shell

**Purpose:**
Add V12 migration for missing storage columns. Create S3-compatible storage adapter shell with configuration boundary. No public/signed URL exposure. Keep implementation safe and minimal.

**Expected Scope:**

| Area | Change |
|------|--------|
| V12 migration | Add `storage_provider`, `storage_bucket`, `storage_object_key` columns to `fp007_image_media_assets` (if missing) |
| `S3ImageStorageAdapter` | New class implementing `ImageStorageAdapter` — write/read to S3-compatible storage |
| Storage config properties | `imagegen.storage.provider`, `imagegen.storage.s3.*` in `application.yml` |
| Adapter selection | Conditional bean activation: `inmemory` vs `s3` based on config |
| `InMemoryImageStorageAdapter` | Remove `@Primary` if present, add conditional activation |
| Unit/integration tests | S3 adapter tests, no URL/key exposure tests |

**Files Likely to Inspect:**
- `ImageStorageAdapter` interface — existing contract
- `InMemoryImageStorageAdapter` — existing implementation
- `ImageStorageWriteRequest` / `ImageStorageWriteResult` — existing models
- `ImageStorageReadRequest` / `ImageStorageReadResult` — existing models
- `application.yml` — config
- `db/migration/` — existing migrations

**Allowed Changes:**
- New migration file (V12)
- New `S3ImageStorageAdapter` class
- New storage configuration properties class
- Modify `InMemoryImageStorageAdapter` bean registration
- New test classes
- Config changes for storage section

**Forbidden Changes:**
- ❌ Return public URL from adapter
- ❌ Return signed URL to admin/user
- ❌ Expose storage key in any DTO
- ❌ Log secrets/config values
- ❌ Expose raw storage payload
- ❌ Add provider URL migration field
- ❌ Add base64 migration field

**Expected Tests:**

| Test | Description |
|------|-------------|
| S3 adapter write success (mock S3 client or test container) | Happy path |
| S3 adapter read success | Read after write |
| S3 adapter write failure → safe error code | Error handling |
| S3 adapter does not generate public URL | `publicUrl` field absence |
| S3 adapter does not generate signed URL | `signedUrl` field absence |
| Storage key not in admin DTO (`StorageHandoffResponse`) | DTO guardrail |
| Storage key not in admin DTO (`ImageMediaAssetResponse`) | DTO guardrail |
| InMemory adapter still works when config is `inmemory` | Backward compatibility |
| Existing tests pass | Baseline preservation |

---

### Task 09 — Retrieval → Validation → Storage Handoff Activation

**Purpose:**
Wire the full controlled handoff path: retrieve provider output → validate → store in private storage → update safe status. Activate the storage handoff endpoint from 501 to real handoff.

**Expected Scope:**

| Area | Change |
|------|--------|
| Controller endpoint | Change `POST .../storage-handoff` from 501 to real handoff action |
| Handoff orchestration | Full flow: load asset → resolve provider output → retrieve → validate → store → metadata update |
| State preconditions | Verify asset is in correct state before handoff |
| Safe response | Return `StorageHandoffResponse` with safe fields only |
| Status updates | `STORAGE_PENDING` → `STORAGE_VALIDATING` → `STORAGE_STORED` / `STORAGE_FAILED` |
| Preview update | `PREVIEW_UNAVAILABLE` → `PREVIEW_AVAILABLE` on success |
| Unit/integration tests | Handoff success/failure, state transitions, review gate preservation |

**Files Likely to Inspect:**
- `AdminImageGenerationController` — storage-handoff endpoint
- `ImageGeneratedAssetHandoffService` — existing handoff service
- `ImageStorageHandoffService` — existing handoff flow
- `ProviderOutputRetrievalService` — from Task 05/06
- `ImageGenerationOrchestrationService` — generation flow

**Allowed Changes:**
- Modify controller endpoint (remove 501, add real handoff delegation)
- New or modified orchestration service
- New test classes

**Forbidden Changes:**
- ❌ Introduce generic status update (`PATCH /status`)
- ❌ Introduce READY_FOR_PUBLISH
- ❌ Introduce COMPLETED
- ❌ Trigger approval on handoff success
- ❌ Trigger publish/export
- ❌ Expose provider/storage internals in response
- ❌ Return signed/public URL

**Expected Tests:**

| Test | Description |
|------|-------------|
| Handoff success → `STORAGE_STORED` + `PREVIEW_AVAILABLE` | Happy path |
| Handoff retrieval failure → `STORAGE_FAILED` + safe error | Retrieval failure |
| Handoff validation failure → `STORAGE_FAILED` + safe error | Validation failure |
| Handoff storage failure → `STORAGE_FAILED` + safe error | Storage write failure |
| Asset not found → 404 | Missing asset |
| Asset in wrong state → safe error | State precondition |
| `StorageHandoffResponse` does not contain provider URL | DTO guardrail |
| `StorageHandoffResponse` does not contain storage key | DTO guardrail |
| `StorageHandoffResponse` does not contain signed/public URL | DTO guardrail |
| Handoff success does not change `GeneratedAssetStatus` | Storage ≠ approval |
| `PREVIEW_AVAILABLE` does not change `GeneratedAssetStatus` | Preview ≠ approval |
| No generic status update endpoint exists | Endpoint absence |
| Existing tests pass | Baseline preservation |

---

### Task 10 — Preview Proxy Regression Hardening for Real Private Storage

**Purpose:**
Ensure the existing preview proxy continues to stream bytes correctly when reading from real storage backend. Harden against exposure regressions.

**Expected Scope:**

| Area | Change |
|------|--------|
| Preview read path | Verify `ImagePreviewProxyService` reads from active `ImageStorageAdapter` (S3 or InMemory) |
| Safety hardening | Ensure no storage key, signed URL, public URL, provider URL, or base64 leaks in response or headers |
| Error handling | Verify safe error responses when preview is unavailable |
| Tests | Preview path tests with both InMemory and S3 adapter configurations |

**Allowed Changes:**
- New test classes
- Minor hardening to preview proxy if regressions found

**Forbidden Changes:**
- ❌ Redirect frontend to storage/provider URL
- ❌ Return signed/public URL
- ❌ Return storage key in body or headers
- ❌ Return base64 in body
- ❌ Change preview access to trigger approval/status change

**Expected Tests:**

| Test | Description |
|------|-------------|
| Preview streams image bytes after successful handoff | Happy path |
| Preview Content-Type matches stored MIME | Correct MIME |
| Preview does not contain `X-Storage-Key` or similar header | Header guardrail |
| Preview does not contain signed URL in headers or body | URL guardrail |
| Preview does not contain provider URL in headers or body | URL guardrail |
| Preview does not contain base64 in body | Base64 guardrail |
| Preview for asset without storage → safe error | Unavailable case |
| Preview does not change asset review status | Idempotent |
| Preview does not trigger approval | Review gate preserved |
| Existing tests pass | Baseline preservation |

---

### Task 11 — Admin DTO, Failure Message, Logging & Audit Hardening

**Purpose:**
Harden admin-safe DTO mapping, failure category messaging, and logging/audit rules. Ensure all admin-facing responses use safe categories only.

**Expected Scope:**

| Area | Change |
|------|--------|
| `ImageStorageErrorNormalizer` | Extend `safeMessage()` with any new retrieval-specific error codes (e.g., `INTERNAL_CONFIGURATION_ERROR` if added) |
| `ImageMediaAssetMapper` | Verify all mappings exclude forbidden fields |
| `StorageHandoffResponse` | Verify DTO excludes forbidden fields |
| `ImageMediaAssetResponse` | Verify DTO excludes forbidden fields |
| Log audit | Verify retrieval/validation/storage log statements are sanitized |
| Tests | DTO regression, sanitization, forbidden field absence |

**Allowed Changes:**
- Extend error normalizer with new codes
- New test classes
- Minor DTO/mapper fixes if regressions found

**Forbidden Changes:**
- ❌ Expose provider URL in any DTO
- ❌ Expose storage key in any DTO
- ❌ Expose raw payloads in any DTO
- ❌ Expose secrets/config in any DTO
- ❌ Expose stack traces in admin response
- ❌ Add automatic approval

**Expected Tests:**

| Test | Description |
|------|-------------|
| `StorageHandoffResponse` — no `storageObjectKey` field | DTO regression |
| `StorageHandoffResponse` — no `providerUrl` field | DTO regression |
| `StorageHandoffResponse` — no `signedUrl` / `publicUrl` / `cdnUrl` field | DTO regression |
| `StorageHandoffResponse` — no `base64` / `imageBinary` field | DTO regression |
| `ImageMediaAssetResponse` — no `storageObjectKey` field | DTO regression |
| `ImageMediaAssetResponse` — no `storageProvider` / `storageBucket` field | DTO regression |
| `ImageMediaAssetResponse` — no `providerUrl` field | DTO regression |
| `ImageStorageErrorNormalizer.sanitize()` masks URLs | Sanitization |
| `ImageStorageErrorNormalizer.sanitize()` masks storage keys | Sanitization |
| `ImageStorageErrorNormalizer.sanitize()` masks secrets/tokens | Sanitization |
| All safe error messages are product-level | Human-readable |
| Existing tests pass | Baseline preservation |

---

### Task 12 — End-to-End Regression Tests & FP-009 Closure Candidate

**Purpose:**
Run full regression across all FP-009 boundaries. Confirm no approval/publish/export behavior. Prepare closure baseline document.

**Expected Scope:**

| Area | Change |
|------|--------|
| Full test suite | All backend tests including new FP-009 tests |
| Safety regression | Systematic check of all forbidden concepts |
| Closure baseline | Documentation of final state, test count, passing status |

**Allowed Changes:**
- New test classes (regression)
- Closure documentation only (if explicitly requested)

**Forbidden Changes:**
- ❌ Mark FP-009 closed before all coding tasks pass
- ❌ Claim publish/export readiness
- ❌ Claim READY_FOR_PUBLISH
- ❌ Claim automatic approval

**Expected Tests:**

| Test | Description |
|------|-------------|
| Full backend test suite passes | All tests green |
| No `READY_FOR_PUBLISH` enum value exists | Regression check |
| No `COMPLETED` enum value exists | Regression check |
| No generic status update endpoint exists | Endpoint absence |
| No publish/export endpoint exists | Endpoint absence |
| No automatic approval after storage success | Review gate |
| No automatic approval after preview access | Review gate |
| AI output remains `GENERATED_PENDING_REVIEW` after handoff | Review gate |
| Provider URL not in any DTO | Field absence |
| Storage key not in any admin DTO | Field absence |
| Signed/public URL not in any response | URL absence |
| Base64 not in any DB field or DTO | Data absence |
| All error messages sanitized | Sanitization |
| Final test count documented | Baseline count |

---

## 6. Detailed Task Template

Every future coding task prompt must follow this template:

```markdown
### Context
- Feature: FP-009 — Provider Output Retrieval & Real Storage Provider Integration
- Task: FP-009 Task {N} — {Title}
- Source docs: [list of relevant FP-009 design docs]
- Baseline: FP-008 CLOSED (commit a36eba8, {current_test_count} tests)
- Previous task: FP-009 Task {N-1} — {Title} (status)

### Exact Scope
- [Numbered list of what this task does]

### Files to Inspect
- [List of existing files to read/understand]

### Allowed Changes
- [Explicit list of what may be created/modified]

### Forbidden Changes
- [Explicit list of what must NOT be changed]

### Implementation Steps
1. [Step-by-step instructions]

### Safety Constraints
- [Relevant subset of safety checklist]

### Tests to Run
- [Specific test commands or descriptions]

### Acceptance Criteria
- [Measurable criteria for task completion]

### Report Format
After completing the task, report:
- Changed files
- Created files
- Tests run
- Test result (pass count / total)
- Safety confirmations (see Section 7)
- Whether code/config/migration/DTO/entity/controller/service changed
- Whether any forbidden exposure was introduced (must be NO)
- Commit ID (only if user explicitly requested commit)
```

---

## 7. Safety Checklist for Every Coding Task

Every future FP-009 coding task must confirm all of the following upon completion:

| # | Safety Check | Required Result |
|---|-------------|-----------------|
| 1 | Provider URL persisted in database? | ❌ NO |
| 2 | Provider URL exposed in any DTO/API response? | ❌ NO |
| 3 | Provider URL logged in any log statement? | ❌ NO |
| 4 | Storage key exposed in admin/user DTO? | ❌ NO |
| 5 | Public URL generated? | ❌ NO |
| 6 | Signed URL returned to admin/user? | ❌ NO |
| 7 | Base64 in DB, DTO, or logs? | ❌ NO |
| 8 | Raw provider payload exposed? | ❌ NO |
| 9 | Raw storage payload exposed? | ❌ NO |
| 10 | Secrets/API keys/config values exposed? | ❌ NO |
| 11 | Preview remains backend-controlled byte streaming? | ✅ YES |
| 12 | AI output automatically APPROVED? | ❌ NO |
| 13 | Human review gate preserved? | ✅ YES |
| 14 | READY_FOR_PUBLISH introduced? | ❌ NO |
| 15 | COMPLETED introduced? | ❌ NO |
| 16 | Generic status update endpoint introduced? | ❌ NO |
| 17 | Publish/export introduced? | ❌ NO |
| 18 | Video/audio introduced? | ❌ NO |
| 19 | Existing tests remain passing? | ✅ YES (or failures explained) |

---

## 8. Test Strategy Per Task

### Test Count Progression Target

| Task | Expected New Tests | Cumulative Baseline |
|------|-------------------|---------------------|
| Task 05 | ~6–10 | 217 + new |
| Task 06 | ~8–12 | Previous + new |
| Task 07 | ~6–10 | Previous + new |
| Task 08 | ~8–12 | Previous + new |
| Task 09 | ~10–15 | Previous + new |
| Task 10 | ~6–10 | Previous + new |
| Task 11 | ~8–12 | Previous + new |
| Task 12 | ~5–10 (regression) | Final baseline |

### Per-Task Test Focus

**Task 05 — Contracts & Models:**
- Contract/model construction tests
- Sanitization tests (error messages contain no URLs/secrets)
- Mock client happy/failure paths
- Compile verification

**Task 06 — Mock Retrieval + Validation:**
- Mock retrieval success → validation pass
- Mock retrieval success → validation fail (MIME / size / corrupt)
- Mock retrieval failure → safe error
- Validation-before-storage assertion
- Storage success does not approve

**Task 07 — Provider URL Gap:**
- Provider URL captured from response
- Provider URL not persisted in DB
- Provider URL not returned in any DTO
- Provider URL not logged (where testable)
- Provider unavailable → safe failure
- Raw provider payload not exposed

**Task 08 — S3 Adapter:**
- Storage write success with private reference
- Storage read success
- Storage failure → safe error
- No public URL in result
- No signed URL in result
- No storage key in admin DTO
- InMemory adapter backward compatibility

**Task 09 — Handoff Activation:**
- Handoff endpoint returns 200 (not 501)
- Full handoff success flow
- Handoff failure categories (retrieval/validation/storage)
- Retrieved ≠ approved
- Stored ≠ approved
- Preview available ≠ approved
- No generic status update behavior

**Task 10 — Preview Hardening:**
- Preview streams bytes from storage
- Preview does not expose key/URL/base64
- Preview unavailable → safe error
- Preview does not change status

**Task 11 — DTO/Audit Hardening:**
- DTO forbidden field regression (all DTOs)
- Failure message sanitization
- Logging audit sanitization (where testable)
- Error normalizer coverage

**Task 12 — Full Regression:**
- Full backend test suite green
- Safety regression checklist (all 19 items)
- Final operational baseline snapshot

---

## 9. Migration / Config Strategy

### Migration Rules

| Rule | Constraint |
|------|-----------|
| No migration in this Task 04 | Documentation-only |
| Prefer reusing FP-008 V11 fields | `storageHandoffStatus`, `previewStatus`, `storageErrorCode`, `storageErrorMessage`, `storedAt` |
| V12 is a separate task (Task 08) | Explicit acceptance criteria |
| V12 must add only `storage_provider`, `storage_bucket`, `storage_object_key` if missing | Columns already on entity |
| V12 must not add provider URL field | ⛔ Forbidden |
| V12 must not add base64 field | ⛔ Forbidden |
| V12 must not add raw provider/storage payload field | ⛔ Forbidden |
| V12 must not expose secrets/config | ⛔ Forbidden |

### Config Rules

| Rule | Constraint |
|------|-----------|
| Real storage config must be internal server config only | `application.yml` |
| Secrets must not be committed to repository | Use `${ENV_VAR}` placeholders |
| Config values must not be logged | No startup config dump |
| Config values must not be returned in API responses | ⛔ Forbidden |
| Default mode must be `inmemory` | Safe default for dev/test |
| S3 mode requires explicit environment configuration | Opt-in only |

### Proposed Config Structure

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
  retrieval:
    connect-timeout-ms: 10000
    read-timeout-ms: 30000
    max-download-size-bytes: 10485760  # 10 MB
```

---

## 10. Endpoint Activation Strategy

### Current State

| Endpoint | Status |
|----------|--------|
| `POST .../generated-assets/{id}/storage-handoff` | **501** Not Implemented |
| `GET .../generated-assets/{id}/preview` | **Active** — byte streaming |

### Activation Rules

| Rule | Constraint |
|------|-----------|
| Do not activate handoff until retrieval, validation, storage, and DTO safety are ready | Task 09 only |
| When activated, handoff must be a command-specific action | Explicit trigger |
| Handoff must not be a generic status update | No `PATCH /status` |
| Handoff must not support arbitrary status transitions | Only the defined flow |
| Handoff must not approve assets | Storage ≠ approval |
| Handoff must not publish/export assets | ⛔ Forbidden |
| Handoff must return safe DTO only | `StorageHandoffResponse` |
| Preview endpoint remains unchanged | Only preview path |
| No new preview endpoints | ⛔ Forbidden |
| No signed URL endpoint | ⛔ Forbidden |
| No public URL endpoint | ⛔ Forbidden |
| No download redirect endpoint | ⛔ Forbidden |

### Activation Prerequisite Checklist

Before Task 09 activates the handoff endpoint, the following must be confirmed:

- [ ] `ProviderOutputRetrievalService` exists and tested (Task 05, 06)
- [ ] Provider URL gap resolved and tested (Task 07)
- [ ] S3 storage adapter exists and tested (Task 08)
- [ ] V12 migration applied (Task 08)
- [ ] Validation-before-storage flow tested (Task 06)
- [ ] All safety guardrails tested (Tasks 05–08)

---

## 11. Review / Approval Boundary

### Explicit Rules

| Rule | Constraint |
|------|-----------|
| Retrieval success does not approve | ⛔ No automatic approval |
| Validation success does not approve | ⛔ No automatic approval |
| Storage success does not approve | ⛔ No automatic approval |
| Preview availability does not approve | ⛔ No automatic approval |
| AI output remains review-required | `GENERATED_PENDING_REVIEW` unchanged after handoff |
| Approval must remain explicit human action | Through existing `ImageGeneratedAssetReviewService` |
| No automatic APPROVED status | ⛔ Forbidden |
| No READY_FOR_PUBLISH status | ⛔ Forbidden |
| No COMPLETED status | ⛔ Forbidden |
| No publish/export action | ⛔ Forbidden |

### Status Independence

```
Storage success → STORAGE_STORED + PREVIEW_AVAILABLE
                  ≠ APPROVED
                  ≠ READY_FOR_PUBLISH
                  ≠ COMPLETED

Preview access  → Information only (read-only, idempotent)
                  ≠ APPROVED
                  ≠ Status change

Approval        → Explicit admin action only
                  → Creates review audit record
                  → Independent from storage/preview
```

---

## 12. Non-Goals

| # | Non-Goal |
|---|----------|
| 1 | Publish/export |
| 2 | Public media delivery |
| 3 | Signed URL exposure to admin/user |
| 4 | CDN integration |
| 5 | User-facing media library |
| 6 | Video/audio |
| 7 | Bulk retrieval |
| 8 | Broad multi-provider redesign |
| 9 | Destructive migration / data backfill |
| 10 | Automatic approval |
| 11 | Generic status update endpoint |
| 12 | Provider URL persistence |
| 13 | Provider URL DTO/log exposure |
| 14 | Storage key admin/user exposure |
| 15 | Base64 persistence/exposure |
| 16 | Raw provider/storage payload exposure |
| 17 | Secret/config exposure |
| 18 | READY_FOR_PUBLISH |
| 19 | COMPLETED |
| 20 | Image editing / cropping / resizing |
| 21 | Regeneration / variation flow |
| 22 | Multi-provider failover |
| 23 | Provider cost optimization |
| 24 | Background worker / orchestration redesign |
| 25 | Public URL generation |

---

## 13. Acceptance Criteria

| # | Criterion | Status |
|---|-----------|--------|
| 1 | Coding Task Breakdown document exists | ✅ |
| 2 | References FP-009 Product Design | ✅ |
| 3 | References FP-009 AI Platform / Media Contract Design | ✅ |
| 4 | References FP-009 Engineering Design | ✅ |
| 5 | References FP-008 closure baseline (commit `a36eba8`, 217/217 tests) | ✅ |
| 6 | Summarizes current baseline | ✅ |
| 7 | Captures the provider URL handling gap from Task 03 | ✅ |
| 8 | Selects and documents Option A (immediate handoff, no persistence) for provider URL resolution | ✅ |
| 9 | Defines a safe task sequence from Task 05 through Task 12 | ✅ |
| 10 | Keeps tasks small and reviewable | ✅ |
| 11 | Defines expected scope and non-scope for each future task | ✅ |
| 12 | Defines per-task test strategy | ✅ |
| 13 | Defines reusable 19-item safety checklist | ✅ |
| 14 | Defines migration/config strategy | ✅ |
| 15 | Defines endpoint activation strategy with prerequisite checklist | ✅ |
| 16 | Defines review/approval boundary rules | ✅ |
| 17 | Preserves backend-controlled preview proxy | ✅ |
| 18 | Preserves human review gate | ✅ |
| 19 | Prohibits provider URL persistence/exposure/logging | ✅ |
| 20 | Prohibits storage key admin/user exposure | ✅ |
| 21 | Prohibits public/signed URL exposure | ✅ |
| 22 | Prohibits base64 in DB/DTO/logs | ✅ |
| 23 | Prohibits raw provider/storage payload exposure | ✅ |
| 24 | Prohibits secret/API key/config exposure | ✅ |
| 25 | Prohibits automatic approval | ✅ |
| 26 | Confirms READY_FOR_PUBLISH is out of scope | ✅ |
| 27 | Confirms COMPLETED is out of scope | ✅ |
| 28 | Confirms generic status update endpoint is out of scope | ✅ |
| 29 | Confirms publish/export is out of scope | ✅ |
| 30 | Confirms video/audio is out of scope | ✅ |
| 31 | Contains no code implementation | ✅ |

---

## 14. Next Step

### FP-009 Task 05 — Provider Output Retrieval Contracts & Safe Result Model

Task 05 is the first implementation task. It must remain contract/model-level and safety-first.

**Scope:** Internal interfaces, result models, mock client, and unit tests only. No real provider download. No endpoint activation. No storage integration. No provider URL persistence/exposure/logging.

**Safety-first:** The retrieval contracts must be designed so that provider URLs can never leak through the result model, DTOs, or log statements.

### Workflow Status

1. ✅ **Product Design** — FP-009 Task 01 (completed)
2. ✅ **AI Platform / Media Contract Design** — FP-009 Task 02 (completed)
3. ✅ **Engineering Design** — FP-009 Task 03 (completed)
4. ✅ **Coding Task Breakdown** — FP-009 Task 04 ← this document
5. ⬜ **Implementation** — FP-009 Task 05 → Task 12

---

*Bu doküman FP-009 Task 04 kapsamında oluşturulmuştur. Documentation-only — kod, config, migration, test, DTO, enum, entity, controller, service veya runtime değişikliği yoktur.*
