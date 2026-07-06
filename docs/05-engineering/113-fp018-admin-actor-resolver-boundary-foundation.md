# FP-018 Task 02 — Admin Actor Resolver & Boundary Foundation

## Status

| Field | Value |
|-------|-------|
| Task | FP-018 Task 02 — Admin Actor Resolver Foundation + Review Action Boundary Preparation |
| Type | Backend production + backend tests |
| Endpoints Changed | None — 21 (9 POST + 12 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changed | No |

---

## Resolver Model

### AdminActorResolutionMode (3 values)
| Value | Description |
|-------|-------------|
| AUTHENTICATED_CONTEXT | Production: resolved from auth context (future) |
| TEST_FALLBACK | Test/dev: wraps request-provided role as fallback |
| SYSTEM_INTERNAL | System: no human auth needed, SYSTEM cannot approve |

Forbidden modes: CLIENT_TRUSTED, REQUEST_ROLE_TRUSTED, BYPASS_AUTH, FORCE_ADMIN

### ResolvedAdminActor (value object)
| Field | Type | Description |
|-------|------|-------------|
| actorRole | ReviewActorRole | Resolved role |
| actorReference | String | Safe internal reference (sanitized) |
| resolutionMode | AdminActorResolutionMode | How resolution occurred |
| safeDisplayName | String | Turkish display name |
| safeReasonCode | String | Optional reason code |
| safeMessage | String | Resolution message |
| resolvedAt | LocalDateTime | Timestamp |

No forbidden fields: token, jwt, session, credential, password, secret, apiKey, accessKey, principalRaw, rawPrincipal, authorizationHeader

### AdminActorResolutionRequest (internal object)
| Field | Type | Description |
|-------|------|-------------|
| requestedActorRole | ReviewActorRole | Client-provided role |
| requestedActorReference | String | Client-provided reference |
| fallbackAllowed | boolean | Whether TEST_FALLBACK is allowed |
| systemInternalAllowed | boolean | Whether SYSTEM_INTERNAL is allowed |
| safeReasonCode | String | Context reason |

### AdminActorResolutionException
Safe error code + message + toString. No credential/token/stack trace leak.

---

## Resolver Behavior

### AdminActorResolver (service)
- REVIEWER fallback → TEST_FALLBACK mode, succeeds
- ADMIN fallback → TEST_FALLBACK mode, succeeds
- CREATOR fallback → TEST_FALLBACK mode, succeeds (but CREATOR cannot approve)
- SYSTEM → requires systemInternalAllowed=true, uses SYSTEM_INTERNAL mode
- SYSTEM without systemInternalAllowed → safe exception
- fallbackAllowed=false → safe exception (production auth not yet implemented)
- null request → safe exception
- null actorRole → safe exception
- Reference sanitization: bearer tokens stripped, passwords stripped, length limited

---

## Review Action Boundary

### AdminActorBoundaryPolicy (utility)
| Category | Allowed Roles | Notes |
|----------|---------------|-------|
| REVIEW_ACTION | REVIEWER, ADMIN | approve/reject/request changes |
| INTERNAL_HANDOFF | REVIEWER, ADMIN, SYSTEM | reassign reviewer |
| INTERNAL_REVISION_REQUEST | REVIEWER, ADMIN | request revision |
| READ_ONLY_VISIBILITY | ALL | GET endpoints |
| SUBMIT_FOR_REVIEW | CREATOR, REVIEWER, ADMIN | submit content |
| ADD_REVIEW_NOTE | CREATOR, REVIEWER, ADMIN | add note |

- SYSTEM cannot approve (canSystemApprove() = false)
- CREATOR cannot approve (canCreatorApprove() = false)
- All ReviewActionType values classifiable into categories
- classify() maps each action to its category

---

## Current actorRole Trust Inventory

| Request DTO | Has actorRole? | Production Auth? | Future Target |
|-------------|:--------------:|:----------------:|---------------|
| AdminReviewActionRequest | ✅ Yes | ❌ No | Resolver integration (Task 03) |
| AdminReviewHandoffRequest | ✅ Yes | ❌ No | Resolver integration (Task 04) |
| AdminInternalRevisionRequest | ✅ Yes | ❌ No | Resolver integration (Task 04) |

These DTOs retain actorRole for test/dev compatibility. They do NOT represent production authentication.

---

## Changed Files

### New Production (6)
- AdminActorResolutionMode.java — 3-value enum
- ResolvedAdminActor.java — value object with builder
- AdminActorResolutionRequest.java — resolution request
- AdminActorResolutionException.java — safe exception
- AdminActorResolver.java — resolver service
- AdminActorBoundaryPolicy.java — action boundary classification

### New Tests (3)
- AdminActorResolverTest.java — resolver + DTO + mode + exception tests
- AdminActorBoundaryPolicyTest.java — classification + restrictions + isAllowed
- FP018ActorRoleRequestBoundaryInventoryTest.java — DTO inventory + endpoints + components

### New Documentation (1)
- 113-fp018-admin-actor-resolver-boundary-foundation.md (this file)

---

## Safety Boundaries

- No endpoint changes (21 = 9 POST + 12 GET)
- No migration/schema/config changes
- No ReviewApplicationService dependency in resolver
- No provider/storage dependency
- No JWT/session/token/credential exposure
- No forbidden resolution modes
- SYSTEM cannot approve
- CREATOR cannot approve
- Reference sanitization (bearer/password stripping)
- Safe toString on all DTOs and exception

---

## Recommended Next

**Task 03 — Review Action Controller Actor Resolution Integration**
