# FP-010 — Operational Regression & Closure Candidate

## A. Document Status

| Field | Value |
|-------|-------|
| **Feature** | FP-010 — Real Storage Activation & Production Storage Operations |
| **Document Type** | Operational Regression & Closure Candidate |
| **Status** | CLOSURE_CANDIDATE |
| **Source Docs** | [FP-010 Combined Design](69-fp010-real-storage-activation-production-operations-combined-design.md), [FP-009 Closure](68-fp009-final-closure-operational-baseline.md) |
| **Created** | 2026-07-05 |

> **NOTE:** This is a closure-candidate document. FP-010 is NOT marked CLOSED yet.
> Final closure requires explicit acceptance as a separate task.

---

## B. Implementation Summary

### Delivered FP-010 Capabilities

| # | Capability | Task | Status |
|---|-----------|------|--------|
| 1 | Combined Product / AI Platform / Engineering Design | Task 01 | ✅ |
| 2 | Real HTTP provider output download client (`HttpProviderOutputRetrievalClient`) | Task 02 | ✅ |
| 3 | Provider download config (`ProviderOutputDownloadProperties`) — disabled by default | Task 02 | ✅ |
| 4 | Download safety: timeout, max size, MIME validation, redirect limit | Task 02 | ✅ |
| 5 | S3 private storage config hardening (`S3PrivateStorageProperties.validateForProduction`) | Task 02 | ✅ |
| 6 | S3 storage adapter real write/read behind `S3StorageClient` abstraction | Task 03 | ✅ |
| 7 | `InMemoryImageStorageAdapter` fallback via `@ConditionalOnMissingBean` | Task 03 | ✅ |
| 8 | `ImmediateStorageHandoffService` — generation → controlled handoff bridge | Task 04 | ✅ |
| 9 | `generateWithOutputReference()` on `ImageGenerationProviderGateway` | Task 04 | ✅ |
| 10 | Generation flow calls `generateWithOutputReference()` → immediate handoff | Task 04 | ✅ |
| 11 | Immediate handoff disabled by default (`imagegen.storage.immediate-handoff.enabled=false`) | Task 04 | ✅ |
| 12 | Handoff failure is non-fatal — generation succeeds regardless | Task 04 | ✅ |
| 13 | Preview remains backend-controlled byte streaming | All | ✅ |
| 14 | Human review remains mandatory | All | ✅ |
| 15 | Operational regression + observability tests | Task 05 | ✅ |

---

## C. Operational Behavior Baseline

### Default Local/Test Behavior
- All real external services are disabled by default.
- `InMemoryImageStorageAdapter` is active when S3 is not enabled.
- `MockProviderOutputRetrievalClient` is active when real download is not enabled.
- Immediate handoff defaults to disabled.
- Backend operates safely without any external dependency.

### Real Provider Download
- Requires explicit enablement: `imagegen.provider.download.enabled=true`
- Config properties: `ProviderOutputDownloadProperties` (timeout, max size, allowed MIME types, redirect limit)
- `HttpProviderOutputRetrievalClient` activates only when enabled
- `MockProviderOutputRetrievalClient` remains as `@ConditionalOnMissingBean` fallback

### Real S3 Storage
- Requires explicit configuration: `imagegen.storage.s3.enabled=true`
- `S3PrivateStorageProperties` must pass `validateForProduction()` — bucket, region, accessKey, secretKey required
- `S3ImageStorageAdapter` activates via `@ConditionalOnProperty`
- `InMemoryImageStorageAdapter` remains as `@ConditionalOnMissingBean` fallback

### Immediate Storage Handoff
- Requires explicit enablement: `imagegen.storage.immediate-handoff.enabled=true`
- When enabled, successful generation triggers: transient URL → download → validation → private storage write
- Handoff failure is non-fatal — generation always succeeds independently
- Admin endpoint remains available for manual/retry handoff

### Safety Invariants (Runtime)
- Admin endpoint remains command-specific (`POST /generated-assets/{id}/storage-handoff`)
- Admin never supplies provider URL — it flows from backend generation only
- Provider URL is **transient** backend-only execution data — never persisted/logged/returned
- Storage object key is **internal-only** — never in admin/user DTO
- **Stored ≠ Approved** — storage success does not change review status
- **Preview Available ≠ Approved** — preview readiness is independent of review
- No public/signed/CDN URL generation
- No automatic approval
- No publish/export

---

## D. Test Baseline

| Metric | Value |
|--------|-------|
| **Command** | `mvn test` |
| **Total Tests** | 1471 |
| **Failures** | 0 |
| **Errors** | 0 |
| **Build** | SUCCESS |

### FP-010 Test Categories

| Category | Test Class | Task |
|----------|-----------|------|
| HTTP provider download client | `HttpProviderOutputRetrievalClientTest` | Task 02 |
| Provider download config | `ProviderOutputDownloadPropertiesTest` | Task 02 |
| S3 storage config hardening | `S3PrivateStoragePropertiesTest` | Task 02 |
| S3 adapter activation | `S3ImageStorageAdapterActivationTest` | Task 03 |
| S3 adapter (V12/migration) | `S3ImageStorageAdapterTest` | Task 03 (updated) |
| Immediate handoff wiring | `ImmediateStorageHandoffWiringTest` | Task 04 |
| Orchestration service | `ImageGenerationOrchestrationServiceTest` | Task 04 (updated) |
| Operational regression | `Fp010OperationalRegressionTest` | Task 05 |

---

## E. Safety Baseline

| # | Constraint | Status |
|---|-----------|--------|
| 1 | No provider URL persistence | ✅ |
| 2 | No provider URL entity field | ✅ |
| 3 | No provider URL DTO/API exposure | ✅ |
| 4 | No provider URL logging | ✅ |
| 5 | No provider URL in safe errors | ✅ |
| 6 | No storage key admin/user exposure | ✅ |
| 7 | No storage key logging | ✅ |
| 8 | No public URL generation | ✅ |
| 9 | No signed URL returned to admin/user | ✅ |
| 10 | No base64 DB/DTO/log/API response | ✅ |
| 11 | No raw provider/storage payload exposure | ✅ |
| 12 | No secret/API key/config exposure | ✅ |
| 13 | No stack trace in admin/API response | ✅ |
| 14 | No hardcoded credentials | ✅ |
| 15 | No committed secrets | ✅ |
| 16 | No real external provider/storage call in tests | ✅ |
| 17 | Preview backend-controlled | ✅ |
| 18 | Human review unchanged | ✅ |
| 19 | No automatic approval | ✅ |
| 20 | No READY_FOR_PUBLISH | ✅ |
| 21 | No COMPLETED | ✅ |
| 22 | No generic status update | ✅ |
| 23 | No publish/export | ✅ |
| 24 | No video/audio | ✅ |

---

## F. Deferred Items

| # | Item | Reason |
|---|------|--------|
| 1 | Production credential provisioning | Ops/infra concern — not code |
| 2 | Production bucket/container provisioning | Ops/infra concern |
| 3 | Production runtime enablement of real provider download | Requires credential readiness |
| 4 | Production runtime enablement of real S3 adapter | Requires bucket + credential readiness |
| 5 | Operational monitoring dashboards/alerts | No metrics infrastructure exists yet |
| 6 | CDN/public delivery | Future feature |
| 7 | Signed URL delivery | Future feature |
| 8 | Publish/export pipeline | Future feature |
| 9 | Video/audio generation/storage | Future feature |
| 10 | User-facing media library | Future feature |

---

## G. Residual Risks & Guardrails

| Risk | Guardrail |
|------|-----------|
| Production enablement may expose secrets if misconfigured | `validateForProduction()` prevents startup with missing config |
| Future developer adds provider URL field to entity/DTO | Forbidden-field tests in multiple test classes enforce absence |
| Future developer adds public/signed URL generation | DTO safety tests + no URL generation code pattern |
| Future developer adds automatic approval on storage success | Status tests enforce stored ≠ approved |
| Provider URLs may be logged accidentally | `ProviderOutputReference.toString()` excludes URL; all log calls use safe context only |
| Storage keys may leak to admin DTO | Forbidden-field tests on `StorageHandoffResponse`, `ImageMediaAssetResponse` |
| Preview redirect to external URL | Read result and preview controller use backend byte streaming only |

---

## H. Acceptance Criteria

| # | Criterion | Status |
|---|----------|--------|
| 1 | Closure-candidate document exists | ✅ |
| 2 | Full backend test suite passes | ✅ |
| 3 | FP-010 delivered behavior documented | ✅ |
| 4 | Operational baseline documented | ✅ |
| 5 | Safety baseline documented | ✅ |
| 6 | Deferred items documented | ✅ |
| 7 | FP-010 is NOT marked CLOSED | ✅ |
| 8 | No forbidden exposure introduced | ✅ |
| 9 | No implementation beyond regression/hardening/observability | ✅ |

---

## I. Next Step

**Next step:** FP-010 Final Closure / Operational Baseline

Only after Task 05 is accepted.
