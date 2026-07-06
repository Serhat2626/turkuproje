# FP-018 Task 04 — Handoff & Internal Revision Actor Boundary Hardening

## Status

| Field | Value |
|-------|-------|
| Task | FP-018 Task 04 |
| Type | Backend production + backend tests + docs |
| Endpoints Changed | None — 21 (9 POST + 12 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |
| Frontend Changed | No |

---

## Boundary Hardening Summary

### Handoff Boundary

| Actor | Allowed? | Resolution Mode | Gate Method |
|-------|:--------:|:---------------:|-------------|
| REVIEWER | ✅ Yes | TEST_FALLBACK | — |
| ADMIN | ✅ Yes | TEST_FALLBACK | — |
| SYSTEM | ✅ Yes | SYSTEM_INTERNAL only | `isHandoffSystemInternalPath()` |
| CREATOR | ❌ No | — | `canCreatorHandoff()` → false |

Key design:
- SYSTEM cannot enter handoff through **request-body `actorRole` alone**.
- SYSTEM must pass through the narrow gate: `AdminActorBoundaryPolicy.isHandoffSystemInternalPath()`.
- This returns `true` only for `ReviewActorRole.SYSTEM`, which enables `systemInternalAllowed=true` in the resolver.
- After resolution, `AdminActorBoundaryPolicy.enforce(INTERNAL_HANDOFF, actor)` validates the resolved actor.
- **Double-check**: resolver + boundary policy both must agree.

### Internal Revision Boundary

| Actor | Allowed? | Resolution Mode | Gate Method |
|-------|:--------:|:---------------:|-------------|
| REVIEWER | ✅ Yes | TEST_FALLBACK | — |
| ADMIN | ✅ Yes | TEST_FALLBACK | — |
| SYSTEM | ❌ No | — | `isRevisionSystemInternalPath()` → always false |
| CREATOR | ❌ No | — | `canCreatorRevision()` → false |

Key design:
- `isRevisionSystemInternalPath()` **always returns false** for all roles.
- Even if someone accidentally passes `systemInternalAllowed=true`, the resolver would create a `SYSTEM_INTERNAL` actor, but `AdminActorBoundaryPolicy.enforce(INTERNAL_REVISION_REQUEST, actor)` would reject it because `SYSTEM_INTERNAL` mode is not in the allowed modes for revision.
- **Triple-check**: gate + resolver + boundary policy all block SYSTEM.

### Why This Design?

Revision and handoff have different trust requirements:

- **Handoff** is a workflow routing operation that may legitimately be triggered by internal system processes (e.g., auto-assignment, load balancing). SYSTEM is allowed through the narrow, auditable gate.
- **Revision** is a human judgment action ("this content needs changes"). SYSTEM should never make this judgment autonomously. Only REVIEWER and ADMIN can request revision.

---

## actorRole Trust Boundary (unchanged)

- actorRole in request body → **TEST_FALLBACK compatibility input only**
- NOT production authentication
- Production auth remains deferred
- Bearer tokens / passwords / JWT-like strings → sanitized to `[REDACTED]`

---

## Safety Regressions

- No token/JWT/session/credential/password/secret/apiKey exposure
- No stackTrace/exceptionClass in responses or exceptions
- No publicUrl/signedUrl/cdnUrl/providerUrl
- No storageKey/bucket/objectPath/rawPayload/base64
- BOUNDARY_POLICY_VIOLATION errors return safe codes only
- All toString methods verified safe
- No unsafe HTML

---

## Changed Files

### Modified Production (2)
- `AdminActorBoundaryPolicy.java` — narrow handoff/revision policy methods + enforce()
- `AdminReviewController.java` — use policy gate methods + enforce() in resolvers

### New Tests (1)
- `FP018HandoffRevisionBoundaryHardeningTest.java`

### New Documentation (1)
- `115-fp018-handoff-revision-boundary-hardening.md` (this file)

---

## Deferred Items
- Frontend actor display/compatibility cleanup (Task 05)
- Auth boundary regression & safety hardening (Task 06)
- Final closure (Task 07)

## Recommended Next
**Task 05 — Frontend Actor Display/Compatibility Cleanup**
