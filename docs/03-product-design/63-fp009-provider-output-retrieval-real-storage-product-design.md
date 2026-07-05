# FP-009 — Provider Output Retrieval & Real Storage Provider Integration — Product Design

---

## 1. Feature Status

| Item | Detail |
|------|--------|
| **Feature** | FP-009 — Provider Output Retrieval & Real Storage Provider Integration |
| **Status** | **PLANNED** |
| **Task** | FP-009 Task 01 — Product Design Document |
| **Task scope** | Documentation-only |
| **Source baseline** | FP-008 — Safe Storage Handoff & Preview Proxy (CLOSED, commit `a36eba8`) |
| **FP-008 closure doc** | [62-fp008-final-closure-operational-baseline.md](../05-engineering/62-fp008-final-closure-operational-baseline.md) |
| **FP-008 product design** | [59-fp008-safe-storage-handoff-preview-proxy-product-design.md](59-fp008-safe-storage-handoff-preview-proxy-product-design.md) |
| **Final backend test baseline** | 217/217 passing |
| **Closed predecessor chain** | FP-001 → FP-002 → FP-003 → FP-004 → FP-005 → FP-006 → FP-007 → FP-008 |

> **This is a product/design-only task.** No code, configuration, migration, test, or runtime changes are made.

---

## 2. Product Purpose

### Why FP-009 Exists

The platform's image generation and media asset pipeline has been built incrementally across three predecessor features:

**FP-007 — Image Generation & Media Asset Pipeline** introduced:

- Real provider integration for image generation (OpenAI / Mock).
- Generation result metadata capture.
- `ImageMediaAsset` entity for media tracking.
- Human review gate (approve/reject with audit).
- **Metadata-only** — provider output was not downloaded, stored, or previewed.

**FP-008 — Safe Storage Handoff & Preview Proxy** introduced:

- Storage/preview status model (`ImageStorageHandoffStatus`, `ImagePreviewStatus`, `ImageStorageErrorCode`).
- V11 migration with storage handoff fields on `ImageMediaAsset`.
- `ImageMediaValidationService` — in-memory MIME sniffing (PNG/JPEG/WebP), 10 MB size limit, dimension extraction, SHA-256 checksum.
- `ImageStorageAdapter` — provider-agnostic storage boundary.
- `InMemoryImageStorageAdapter` — mock/private storage adapter.
- `ImageStorageHandoffService` — validate → store → metadata update flow.
- Safe admin DTO updates with storage/preview status fields.
- Backend-controlled preview proxy — image bytes streaming, no URL exposure.
- `ImageStorageErrorNormalizer` — safe error normalization and sanitization.
- Storage handoff endpoint contract — **intentionally 501** (provider output retrieval deferred).
- Human review gate preserved.

**What FP-008 intentionally deferred:**

- Provider output retrieval — downloading image bytes from provider temporary URL.
- Real object storage integration — S3/MinIO/GCS adapter implementation.
- Storage handoff endpoint activation — transitioning from 501 to real handoff.
- Storage lifecycle and cleanup.
- Provider output TTL handling.

**FP-009 starts:**

- Controlled provider output retrieval — safely downloading provider-generated image bytes in the backend.
- Controlled real storage provider integration — replacing mock storage with private, backend-controlled object storage.
- Completing the path from "provider result metadata exists" to "provider output can be safely retrieved, validated, stored privately, and previewed through the backend-controlled proxy."

### Product Goal

Move the platform from the current state where provider generation results exist as **metadata only** (FP-007) with a **mock storage boundary** (FP-008), to a state where provider output can be:

1. **Safely retrieved** from the provider's output reference in a controlled backend path.
2. **Validated** using the established FP-008 media validation pipeline.
3. **Stored privately** in backend-controlled object storage.
4. **Previewed** through the existing backend-controlled preview proxy.
5. **Reviewed** by an admin through the existing explicit human review flow.

All of this while maintaining every safety, privacy, and governance rule established in FP-007 and FP-008.

---

## 3. User / Admin Value

### Admin Perspective

| Value | Description |
|-------|-------------|
| **Output retrieval** | Generated image outputs can be retrieved from provider output references — the system downloads them safely in the backend |
| **Pre-storage validation** | Outputs are validated (MIME type, file size, dimensions, integrity) before storage is considered successful |
| **Private storage** | Outputs are stored in private, backend-controlled storage — not in-memory mock |
| **Safe preview** | Admins can preview stored media safely through the existing backend-controlled preview proxy |
| **Preserved review gate** | Admins still cannot approve automatically generated outputs without explicit human review |
| **Safe-by-default** | The product remains safe-by-default: no URL exposure, no storage key exposure, no automatic approval |
| **Failure visibility** | Admins see safe, product-level failure summaries when retrieval or storage fails — no technical details leak |

### System Perspective

| Value | Description |
|-------|-------------|
| **Real persistence** | Provider-generated images are persisted beyond the provider's temporary URL TTL |
| **Production readiness** | The storage layer moves from mock (in-memory) to real object storage — a necessary step for production |
| **Adapter pattern validated** | The `ImageStorageAdapter` interface proven in FP-008 with mock adapter is now implemented with real storage |
| **Pipeline continuity** | The validate → store → metadata update pipeline established in FP-008 remains intact |

---

## 4. Scope

### In Scope

| # | Item |
|---|------|
| 1 | Controlled provider output retrieval planning |
| 2 | Controlled provider output download flow definition |
| 3 | Controlled real storage provider integration planning |
| 4 | Private storage only — no public, CDN, or signed URL generation |
| 5 | Validation before storage — FP-008 validation pipeline or successor contract |
| 6 | Safe storage handoff continuation — extending the existing handoff service |
| 7 | Existing backend-controlled preview proxy remains the only preview path |
| 8 | Safe admin-facing retrieval/storage/preview status visibility |
| 9 | Human review gate remains mandatory |
| 10 | Provider URL may be used transiently in backend execution only if needed, but must not be persisted, exposed, logged, or returned in DTOs |
| 11 | Storage object key may exist internally only, but must not be admin-facing or user-facing |
| 12 | Product-level rules for retry and failure visibility |
| 13 | Product-level auditability requirements without exposing secrets or raw payloads |

### Out of Scope

| # | Item | Reason |
|---|------|--------|
| 1 | Public URLs | Security — private storage only |
| 2 | Signed URLs exposed to admin/user | Security — backend-controlled preview only |
| 3 | Storage object key exposure | Internal infrastructure detail |
| 4 | Provider URL persistence | URL lifecycle risk — transient use only |
| 5 | Provider URL exposure | Security — internal backend use only |
| 6 | Base64 in DB | Performance and security risk |
| 7 | Base64 in DTO | Performance and security risk |
| 8 | Base64 in logs | Security risk |
| 9 | Raw provider payload exposure | Provider security |
| 10 | Raw storage payload exposure | Infrastructure security |
| 11 | Secret/API key/config exposure | Secret management |
| 12 | Automatic approval | Human review gate violation |
| 13 | READY_FOR_PUBLISH status | Scope boundary — not introduced |
| 14 | COMPLETED status | Scope boundary — not introduced |
| 15 | Generic status update endpoint | Security risk — explicit actions preferred |
| 16 | Publish/export | Separate feature |
| 17 | Video | Image scope only |
| 18 | Audio | Image scope only |
| 19 | CDN / public delivery | Distribution infrastructure separate |
| 20 | User-facing media library | Admin-only at this stage |
| 21 | Bulk retrieval | Controlled single-item retrieval preferred |
| 22 | Background worker/orchestration redesign | Deferred unless explicitly scoped in engineering |
| 23 | Multi-provider abstraction expansion beyond controlled image output retrieval | Future enhancement |
| 24 | Destructive migration or data backfill | Not needed |

---

## 5. Product States / Lifecycle

### Intended Conceptual Flow

The following describes the intended product-level lifecycle for provider output retrieval and storage. This is a conceptual flow for product design purposes — it does not prescribe implementation details.

```
┌──────────────────────────────────────────────────────────────────┐
│  FP-007 BASELINE                                                 │
│  Provider generation result exists (metadata-only).              │
│  Provider output reference exists internally.                    │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│  PROVIDER OUTPUT RETRIEVAL                                       │
│                                                                  │
│  1. Retrieval is requested/triggered in a controlled backend     │
│     path (admin action or system trigger — design TBD).          │
│  2. Backend retrieves output from provider using internal        │
│     credentials/config.                                          │
│  3. Provider URL is used transiently — never persisted,          │
│     exposed, logged, or returned in DTOs.                        │
│  4. If retrieval fails → safe failure status.                    │
│  5. If retrieval succeeds → bytes proceed to validation.         │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│  MEDIA VALIDATION (FP-008 pipeline)                              │
│                                                                  │
│  1. MIME type validation (PNG/JPEG/WebP magic bytes).            │
│  2. File size validation (10 MB limit).                          │
│  3. Image dimension extraction (width/height).                   │
│  4. SHA-256 checksum calculation.                                │
│  5. If validation fails → safe failure status.                   │
│  6. If validation succeeds → bytes proceed to storage.           │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│  PRIVATE STORAGE                                                 │
│                                                                  │
│  1. Backend stores validated image in private object storage.    │
│  2. Storage object key generated server-side (internal only).    │
│  3. Safe storage/preview status fields updated.                  │
│  4. If storage fails → safe failure status.                      │
│  5. If storage succeeds → preview becomes available.             │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│  ADMIN PREVIEW (FP-008 — unchanged)                              │
│                                                                  │
│  • Backend-controlled preview proxy.                             │
│  • Image bytes streaming — no URL exposure.                      │
│  • Preview ≠ approval.                                           │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────────────┐
│  HUMAN REVIEW (FP-007 — unchanged)                               │
│                                                                  │
│  • Admin reviews output via preview.                             │
│  • Explicit approve → APPROVED.                                  │
│  • Explicit reject → REJECTED (reason required).                 │
│  • Review audit record created.                                  │
│  • No automatic approval.                                        │
│  • No publish/export.                                            │
└──────────────────────────────────────────────────────────────────┘
```

### Conceptual Status Model

The following conceptual statuses describe product-level states. Exact enum names, field mappings, and persistence strategy are deferred to Engineering Design.

#### Retrieval Status (New Concept — FP-009)

| Conceptual Status | Meaning |
|-------------------|---------|
| Retrieval Pending | Provider output retrieval has not started |
| Retrieval In Progress | Backend is downloading provider output |
| Retrieval Failed | Download failed (timeout, HTTP error, TTL expired, etc.) |
| Retrieved | Provider output bytes successfully downloaded |

#### Validation Status (Existing Concept — FP-008)

| Conceptual Status | Meaning |
|-------------------|---------|
| Validation Pending | Validation has not started |
| Validation Failed | MIME, size, dimension, or integrity check failed |
| Validated | All validation checks passed |

#### Storage Status (Existing — FP-008)

| Conceptual Status | Meaning |
|-------------------|---------|
| Storage Pending | Storage write has not started |
| Storage Failed | Storage write failed |
| Stored | Image successfully written to private storage |

#### Preview Status (Existing — FP-008)

| Conceptual Status | Meaning |
|-------------------|---------|
| Preview Unavailable | Storage not complete or failed |
| Preview Available | Image can be previewed via backend proxy |

#### Review Status (Existing — FP-007, unchanged)

| Conceptual Status | Meaning |
|-------------------|---------|
| Review Required | Output awaiting human review |
| Approved | Admin explicitly approved |
| Rejected | Admin explicitly rejected (reason required) |

### Status Rules

- Retrieval status, validation status, and storage status are **independent from review status**.
- Preview status is **derived from storage status** — not from review status.
- Retrieval success does **not** automatically trigger approval.
- Storage success does **not** automatically trigger approval.
- Preview availability does **not** automatically trigger approval.
- `READY_FOR_PUBLISH` does **not** exist.
- `COMPLETED` does **not** exist.
- Generic "status update" model does **not** exist.
- Automatic approval status transitions do **not** exist.

---

## 6. Safety and Privacy Rules

> **Non-negotiable.** These rules carry forward from FP-007 and FP-008 and are extended for FP-009.

### Provider URL Rules

| Rule | Detail |
|------|--------|
| Provider URLs are never persisted | Transient backend use only — cleared after retrieval |
| Provider URLs are never returned in DTOs | Not in any response body |
| Provider URLs are never logged | Not in application logs, audit logs, or error messages |
| Provider URLs are never exposed to admin/user | Not in UI, API response, header, or error detail |

### Storage Key Rules

| Rule | Detail |
|------|--------|
| Storage object keys are internal only | Used by backend services and storage adapter |
| Storage object keys are never admin-facing | Not in DTOs, response bodies, or headers |
| Storage object keys are never user-facing | Not in any client-visible surface |

### URL Generation Rules

| Rule | Detail |
|------|--------|
| Public URLs are not generated | No public access path exists |
| Signed URLs are not returned to admin/user | Not in any response |
| CDN URLs are not generated | Distribution infrastructure is out of scope |

### Preview Rules

| Rule | Detail |
|------|--------|
| Preview remains backend-controlled byte streaming | No URL-based preview |
| Preview endpoint does not expose storage key | Not in body or headers |
| Preview endpoint does not expose signed/public URL | Not in body or headers |
| Preview endpoint does not expose provider URL | Not in body or headers |

### Data Rules

| Rule | Detail |
|------|--------|
| Base64 must not enter DB | No base64 columns |
| Base64 must not enter DTO | No base64 response fields |
| Base64 must not enter logs | No base64 in log output |
| Raw provider payloads must not be exposed | Not in DTOs, logs, or admin UI |
| Raw storage payloads must not be exposed | Not in DTOs, logs, or admin UI |
| Secrets/API keys/config values must not be exposed | Not in DTOs, logs, error messages, or admin UI |

### Validation and Review Rules

| Rule | Detail |
|------|--------|
| Validation must happen before storage is considered successful | Invalid media must not reach storage |
| AI-generated output must require human review | No bypass of the review gate |
| AI-generated output must never become APPROVED automatically | Only explicit admin approve action |
| Storage success ≠ approval | Independent concerns |
| Preview access ≠ approval | Read-only, idempotent |

### Scope Boundary Rules

| Rule | Detail |
|------|--------|
| Publish/export remains out of scope | Separate feature |
| READY_FOR_PUBLISH does not exist | Not introduced |
| COMPLETED does not exist | Not introduced |
| Generic status update endpoint does not exist | Explicit actions only |
| Video/audio remain out of scope | Image only |

---

## 7. Admin Experience

### What the Admin Can See

| Item | Visible? | Detail |
|------|----------|--------|
| Safe retrieval status | ✅ Yes | Product-level retrieval state (pending/failed/retrieved) |
| Safe storage status | ✅ Yes | Product-level storage state (pending/failed/stored) |
| Safe preview status | ✅ Yes | Preview available / unavailable |
| Safe failure reason category | ✅ Yes | Product-level failure classification (e.g., "retrieval failed", "validation failed") |
| Image preview | ✅ Yes | Through backend-controlled preview proxy — image bytes only |
| Review actions | ✅ Yes | Explicit approve/reject through existing review flow |
| Safe media metadata | ✅ Yes | MIME type, dimensions, file size, checksum, stored timestamp |

### What the Admin Must Not See

| Item | Visible? | Detail |
|------|----------|--------|
| Provider URL | ⛔ Never | Internal transient use only |
| Provider temporary URL | ⛔ Never | Internal transient use only |
| Storage object key | ⛔ Never | Internal infrastructure detail |
| Storage bucket/prefix | ⛔ Never | Internal infrastructure detail |
| Storage provider alias | ⛔ Never | Internal infrastructure detail |
| Signed URL | ⛔ Never | Not generated for admin |
| Public URL | ⛔ Never | Not generated |
| CDN URL | ⛔ Never | Not generated |
| Raw provider response/payload | ⛔ Never | Provider security |
| Raw storage response/payload | ⛔ Never | Infrastructure security |
| Base64 data | ⛔ Never | Performance and security |
| API key / secret / config | ⛔ Never | Secret management |
| Internal prompt | ⛔ Never | Prompt security |

### Admin Flow (Conceptual)

```
1. Admin → Generated Assets list (pending review or all)
2. Admin → Opens asset detail
3. System → Shows safe retrieval/storage/preview status
   ├── Retrieval pending: "Çıktı alınıyor..."
   ├── Retrieval failed: Safe failure message
   ├── Storage pending: "Depolama işleniyor..."
   ├── Stored + Preview available: Preview ready
   └── Storage failed: Safe failure message
4. Admin → Opens preview (if storage complete)
   → Backend proxy streams image bytes
   → No URL, key, or raw data visible
5. Admin → Reviews output
   → Approve (explicit action)
   → Reject (explicit action, reason required)
6. System → Creates review audit record
7. No automatic approval at any step
8. No publish/export at any step
```

### Failure Experience

- If retrieval fails → admin sees safe product-level message (e.g., "Sağlayıcı çıktısı alınamadı")
- If validation fails → admin sees safe failure category (e.g., "Dosya doğrulaması başarısız")
- If storage fails → admin sees safe failure message (e.g., "Depolama hatası")
- Admin never sees the technical root cause, provider URL, storage SDK error, or raw payload
- Admin can re-trigger retrieval/handoff if the product flow allows it (design TBD in engineering)

---

## 8. Failure Handling

### Failure Categories

Each failure category below defines what the admin may see and what must remain internal.

---

#### Category: Provider Output Unavailable

| Aspect | Detail |
|--------|--------|
| **Condition** | Provider output reference does not exist or is invalid |
| **Admin-visible** | Safe product-level message: "Sağlayıcı çıktısı mevcut değil" |
| **Internal only** | Actual reference value, provider metadata, internal diagnostics |
| **Provider/storage raw detail** | Not exposed |

#### Category: Provider Retrieval Failed

| Aspect | Detail |
|--------|--------|
| **Condition** | HTTP download from provider failed (timeout, connection error, HTTP error, TTL expired) |
| **Admin-visible** | Safe product-level message: "Sağlayıcı çıktısı alınamadı" |
| **Internal only** | HTTP status code, provider URL, timeout value, network error detail |
| **Provider/storage raw detail** | Not exposed |

#### Category: Unsupported Media Type

| Aspect | Detail |
|--------|--------|
| **Condition** | Downloaded content is not PNG, JPEG, or WebP |
| **Admin-visible** | Safe product-level message: "Desteklenmeyen dosya türü" |
| **Internal only** | Actual detected MIME type, magic bytes, Content-Type header value |
| **Provider/storage raw detail** | Not exposed |

#### Category: File Too Large

| Aspect | Detail |
|--------|--------|
| **Condition** | Downloaded content exceeds 10 MB limit |
| **Admin-visible** | Safe product-level message: "Dosya boyutu çok büyük" |
| **Internal only** | Actual file size, Content-Length header value |
| **Provider/storage raw detail** | Not exposed |

#### Category: Validation Failed

| Aspect | Detail |
|--------|--------|
| **Condition** | Media validation checks failed (corrupt image, dimension extraction failure, checksum error) |
| **Admin-visible** | Safe product-level message: "Dosya doğrulaması başarısız" |
| **Internal only** | Specific validation failure detail, raw error, stack trace |
| **Provider/storage raw detail** | Not exposed |

#### Category: Storage Write Failed

| Aspect | Detail |
|--------|--------|
| **Condition** | Writing validated image to object storage failed |
| **Admin-visible** | Safe product-level message: "Depolama hatası" |
| **Internal only** | Storage SDK error, bucket name, object key, endpoint URL, credentials |
| **Provider/storage raw detail** | Not exposed |

#### Category: Preview Unavailable

| Aspect | Detail |
|--------|--------|
| **Condition** | Storage not complete, storage failed, or read from storage failed |
| **Admin-visible** | Safe product-level message: "Önizleme mevcut değil" |
| **Internal only** | Storage read error, object key, storage state detail |
| **Provider/storage raw detail** | Not exposed |

#### Category: Internal Configuration Unavailable

| Aspect | Detail |
|--------|--------|
| **Condition** | Storage provider configuration missing or invalid (bucket, endpoint, credentials) |
| **Admin-visible** | Safe product-level message: "Sistem yapılandırma hatası" |
| **Internal only** | Configuration key names, values, credential references |
| **Provider/storage raw detail** | Not exposed |

### Cross-Cutting Failure Rules

| Rule | Detail |
|------|--------|
| All failure messages are safe, product-level summaries | No technical detail leakage |
| Sensitive technical detail remains backend/internal only | Logged server-side with safe sanitization |
| No provider/storage raw details in admin-facing output | `ImageStorageErrorNormalizer` pattern applies |
| Validation failure does not auto-reject the asset | Failure status is separate from review status |
| Storage failure does not auto-reject the asset | Failure status is separate from review status |
| Error messages are sanitized | Provider URLs, storage keys, credentials, base64, stack traces are masked |

---

## 9. Acceptance Criteria

| # | Criterion | Status |
|---|-----------|--------|
| 1 | FP-009 product design document exists at the target path | ✅ |
| 2 | Document clearly references FP-008 closure baseline (commit `a36eba8`, 217/217 tests) | ✅ |
| 3 | Document defines controlled provider output retrieval | ✅ |
| 4 | Document defines controlled real storage provider integration | ✅ |
| 5 | Document keeps preview backend-controlled (existing proxy only) | ✅ |
| 6 | Document prohibits public/signed URL exposure | ✅ |
| 7 | Document prohibits storage key exposure | ✅ |
| 8 | Document prohibits provider URL persistence | ✅ |
| 9 | Document prohibits provider URL exposure | ✅ |
| 10 | Document prohibits provider URL logging | ✅ |
| 11 | Document prohibits base64 in DB/DTO/logs | ✅ |
| 12 | Document prohibits raw provider/storage payload exposure | ✅ |
| 13 | Document prohibits secret/API key/config exposure | ✅ |
| 14 | Document preserves human review gate | ✅ |
| 15 | Document confirms AI output is never automatically APPROVED | ✅ |
| 16 | Document confirms READY_FOR_PUBLISH is out of scope | ✅ |
| 17 | Document confirms COMPLETED is out of scope | ✅ |
| 18 | Document confirms generic status update endpoint is out of scope | ✅ |
| 19 | Document confirms publish/export is out of scope | ✅ |
| 20 | Document confirms video/audio are out of scope | ✅ |
| 21 | Document contains no code implementation | ✅ |
| 22 | Document contains no migration changes | ✅ |
| 23 | Document contains no config changes | ✅ |
| 24 | Document contains no test changes | ✅ |

---

## 10. Next Documents

| Task | Document |
|------|----------|
| **FP-009 Task 02** | AI Platform / Media Contract Design |
| **FP-009 Task 03** | Engineering Design |
| **FP-009 Task 04+** | Coding Tasks |

The workflow sequence is strictly followed:

1. ✅ **Product Design** ← this document
2. ⬜ AI Platform / Media Contract Design
3. ⬜ Engineering Design
4. ⬜ Coding Tasks
5. ⬜ Code Implementation

---

*Bu doküman FP-009 Task 01 kapsamında oluşturulmuştur. Documentation-only — kod, config, migration, test veya runtime değişikliği yoktur.*
