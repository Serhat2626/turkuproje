# FP-018 Task 03 — Review Action Controller Actor Resolution Integration

## Status

| Field | Value |
|-------|-------|
| Task | FP-018 Task 03 — Review Action Controller Actor Resolution Integration |
| Type | Backend production + backend tests |
| Endpoints Changed | None — 21 (9 POST + 12 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changed | No |

---

## Controller Integration Summary

### What Changed

AdminReviewController now routes all 9 POST endpoints through AdminActorResolver before passing actor role/reference to service layer:

| Endpoint | Resolution Method | SYSTEM Allowed? |
|----------|-------------------|:---------------:|
| POST /submit | resolveReviewActor | ❌ No |
| POST /start | resolveReviewActor | ❌ No |
| POST /request-changes | resolveReviewActor | ❌ No |
| POST /approve-internal | resolveReviewActor | ❌ No |
| POST /reject | resolveReviewActor | ❌ No |
| POST /return-to-draft | resolveReviewActor | ❌ No |
| POST /note | resolveReviewActor | ❌ No |
| POST /handoff | resolveHandoffActor | ✅ Yes (internal) |
| POST /{t}/{id}/request-internal-revision | resolveRevisionActor | ❌ No |

### Resolution Flow

```
Request Body (actorRole) → AdminActorResolutionRequest
    → AdminActorResolver.resolve()
    → ResolvedAdminActor (TEST_FALLBACK mode)
    → actor.getActorRole() / actor.getActorReference()
    → Service Layer (unchanged)
```

### What Did NOT Change
- No endpoint additions/removals/renames
- No response format changes
- No service layer changes
- No state machine changes
- No migration/schema/config changes

---

## Actor Role Trust Boundary

### Current Behavior (TEST_FALLBACK)
- Request actorRole is treated as **TEST_FALLBACK input only**
- It is explicitly **not production authentication**
- AdminActorResolver wraps it with resolutionMode=TEST_FALLBACK
- Actor reference is sanitized (bearer tokens stripped, passwords stripped)

### SYSTEM Restriction
- Standard review actions (7 endpoints): SYSTEM is **blocked**
- Handoff: SYSTEM is **allowed** via systemInternalAllowed=true
- Revision request: SYSTEM is **blocked**

### Production Auth (deferred)
- No JWT/session/principal/authorization header integration
- No Spring Security changes
- Production auth remains deferred to future feature

---

## Safety Regressions

### Sanitization
- Bearer tokens in actor references → [REDACTED]
- Passwords in actor references → [REDACTED]
- Reference length limited to 200 chars
- Safe toString on all DTOs and exceptions

### Forbidden Fields Absent
- No token/jwt/session/credential/password/secret/apiKey in any DTO
- No stackTrace/exceptionClass in responses
- No publicUrl/signedUrl/cdnUrl/providerUrl
- No storageKey/bucket/objectPath/rawPayload/base64

### Exception Safety
- AdminActorResolutionException handler returns safe HTTP status codes
- SYSTEM_NOT_ALLOWED → 403 Forbidden
- FALLBACK_NOT_ALLOWED → 403 Forbidden
- ACTOR_ROLE_NULL → 400 Bad Request
- No credential/token leak in exception messages

---

## Changed Files

### Modified Production (2)
- AdminReviewController.java — actor resolution integration
- AdminReviewDtoMapper.java — overloaded toHandoffRequest with resolved actor

### New Tests (1)
- FP018ControllerActorResolutionIntegrationTest.java

### New Documentation (1)
- 114-fp018-controller-actor-resolution-integration.md (this file)

---

## Deferred Items
- Handoff/revision boundary hardening (Task 04)
- Frontend actor display cleanup (Task 05)
- Auth boundary regression (Task 06)
- Final closure (Task 07)

## Recommended Next
**Task 04 — Handoff/Internal Action Actor Boundary Hardening**
