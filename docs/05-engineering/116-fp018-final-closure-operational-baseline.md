# FP-018 Final Closure — Production Auth Boundary Preparation & Admin Actor Resolution

## Status

| Field | Value |
|-------|-------|
| Feature | FP-018 — Production Auth Boundary Preparation & Admin Actor Resolution |
| Status | **CLOSED** |
| Closure Date | 2026-07-07 |
| Documentation Only | No — backend + frontend + tests + docs |

---

## Baselines

| Metric | Before FP-018 | After FP-018 | Delta |
|--------|:-------------:|:------------:|:-----:|
| Backend tests | 2562 | 2775 | +213 |
| Frontend tests | 680 | 697 | +17 |
| Combined tests | 3242 | 3472 | +230 |
| Endpoints | 21 (9P+12G) | 21 (9P+12G) | 0 |
| Migrations | — | — | None |
| Schema changes | — | — | None |
| Config changes | — | — | None |

---

## Task History

| Task | Commit | Description |
|------|--------|-------------|
| Task 01 | Design doc | Combined design for auth boundary preparation |
| Task 02 | e0d9677 | AdminActorResolver + BoundaryPolicy + exception foundation |
| Task 03 | 721ba79 | Controller actor resolution integration (all 9 POST) |
| Task 04 | 1e500a1 | Handoff/revision boundary hardening |
| Task 05 | c54d02a | Frontend cleanup + broad regression + final closure |

---

## Feature Summary

### AdminActorResolver Foundation (Task 02)

Created the actor resolution layer that sits between request-body `actorRole` and service layer:

- **AdminActorResolutionMode** enum: `TEST_FALLBACK`, `AUTHENTICATED_CONTEXT`, `SYSTEM_INTERNAL`
- **ResolvedAdminActor** value object: actorRole, actorReference, resolutionMode, safeDisplayName, safeReasonCode, safeMessage
- **AdminActorResolutionRequest** internal request object with `fallbackAllowed` and `systemInternalAllowed` flags
- **AdminActorResolutionException** safe exception with `safeErrorCode`
- **AdminActorBoundaryPolicy** classification and policy matrix

### Controller Integration (Task 03)

All 9 POST admin workflow endpoints now route through AdminActorResolver:

| Endpoint | Resolution | SYSTEM? |
|----------|:----------:|:-------:|
| POST /submit | resolveReviewActor | ❌ |
| POST /start | resolveReviewActor | ❌ |
| POST /request-changes | resolveReviewActor | ❌ |
| POST /approve-internal | resolveReviewActor | ❌ |
| POST /reject | resolveReviewActor | ❌ |
| POST /return-to-draft | resolveReviewActor | ❌ |
| POST /note | resolveReviewActor | ❌ |
| POST /handoff | resolveHandoffActor | ✅ narrow gate |
| POST /request-internal-revision | resolveRevisionActor | ❌ |

### Handoff/Revision Boundary Hardening (Task 04)

- **Handoff**: SYSTEM allowed ONLY via `isHandoffSystemInternalPath()` narrow gate → SYSTEM_INTERNAL mode. Post-resolution `enforce()` validates against boundary policy.
- **Revision**: `isRevisionSystemInternalPath()` always returns false for ALL roles. Triple-check: gate + resolver + enforce all block SYSTEM.
- CREATOR blocked from handoff and revision by boundary enforcement.

### Frontend Actor Boundary Cleanup (Task 05)

- No auth/JWT/session/credential/password patterns in frontend code
- No publish/export/delivery URL fields
- No unsafe HTML (no dangerouslySetInnerHTML, no innerHTML)
- actorRole used as workflow context only, not presented as production auth
- API client does not send Authorization headers

---

## actorRole Trust Boundary

```
Request Body actorRole
    ↓
AdminActorResolutionRequest (TEST_FALLBACK mode)
    ↓
AdminActorResolver.resolve()
    ↓
ResolvedAdminActor (mode=TEST_FALLBACK)
    ↓
AdminActorBoundaryPolicy.enforce() [handoff/revision]
    ↓
Service Layer (unchanged)
```

**Key principle**: `actorRole` in the request body is a **TEST_FALLBACK compatibility input** only. It is **NOT** production authentication. The resolver wraps it with an explicit mode marker, sanitizes the reference, and passes it through. When production auth is implemented, the resolver will use `AUTHENTICATED_CONTEXT` mode from Spring Security context.

---

## SYSTEM Rules Summary

| Action Category | SYSTEM Allowed? | Mode Required |
|----------------|:--------------:|:-------------:|
| REVIEW_ACTION | ❌ No | — |
| SUBMIT_FOR_REVIEW | ❌ No | — |
| ADD_REVIEW_NOTE | ❌ No | — |
| INTERNAL_REVISION_REQUEST | ❌ No | — |
| INTERNAL_HANDOFF | ✅ Yes | SYSTEM_INTERNAL |
| READ_ONLY_VISIBILITY | ✅ Yes | Any |

| Guard | Value |
|-------|:-----:|
| canSystemApprove() | false |
| canSystemRevision() | false |
| canSystemHandoff() | true |
| canCreatorApprove() | false |
| canCreatorHandoff() | false |
| canCreatorRevision() | false |

---

## Sanitization Summary

| Pattern | Action |
|---------|--------|
| `Bearer xxx` | → `[REDACTED]` |
| `password=xxx` | → `[REDACTED]` |
| `eyJ...base64.base64.base64` (JWT-like) | → `[REDACTED]` |
| `authorization=xxx` | → `[REDACTED]` |
| HTML tags `<>` | stripped |
| Length > 200 chars | truncated to 200 |
| Empty/null reference | fallback to role name |

---

## Safety Baseline Preserved

- ✅ No token/JWT/session/credential/password/secret/apiKey exposure
- ✅ No stackTrace/exceptionClass in responses
- ✅ No publicUrl/signedUrl/cdnUrl/providerUrl
- ✅ No storageKey/bucket/objectPath/rawPayload/base64
- ✅ No publish/export/delivery behavior
- ✅ No auto-approval
- ✅ No generic status update
- ✅ No unsafe HTML
- ✅ No migration/schema/config changes
- ✅ BOUNDARY_POLICY_VIOLATION → 403 Forbidden
- ✅ SYSTEM_NOT_ALLOWED → 403 Forbidden
- ✅ ACTOR_ROLE_NULL → 400 Bad Request

---

## Production Auth — Deferred

Production authentication was explicitly **NOT** implemented in FP-018. The following remain deferred:

- Spring Security integration
- JWT/session validation
- Principal extraction
- AUTHENTICATED_CONTEXT mode in resolver
- User identity mapping

These are preparation/design items for a future feature (not FP-019).

---

## Recommended Next Feature

**FP-019 — Türkü Story-to-Video Vertical Slice**

FP-019 should focus on the core content pipeline: taking a topic input through research → script → storyboard → scene prompts, creating a complete vertical slice of the content production workflow.
