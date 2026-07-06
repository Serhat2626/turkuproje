# FP-018 — Production Auth Boundary Preparation & Admin Actor Resolution — Combined Design

---

## 1. Status

| Field | Value |
|-------|-------|
| Feature | FP-018 — Production Auth Boundary Preparation & Admin Actor Resolution |
| Status | **PLANNED / DESIGN ONLY** |
| Type | Documentation-only |
| Runtime Changes | None |
| Endpoints Changed | None — 21 (9 POST + 12 GET) |
| Migrations | None |
| Schema Changes | None |
| Config Changes | None |

---

## 2. Background

### Feature History

| Feature | Relevance to FP-018 |
|---------|---------------------|
| FP-012 | Introduced review actions and actor roles (CREATOR, REVIEWER, ADMIN, SYSTEM). actorRole is a request field, not authenticated. |
| FP-013 | Handoff uses SYSTEM safely but SYSTEM cannot approve. Demonstrated actor role restrictions work at service level. |
| FP-014 | Readiness is read-only — no actor resolution needed for reads. |
| FP-015 | Internal revision request allows REVIEWER/ADMIN only. Showed per-action role checks exist but rely on request-provided role. |
| FP-016 | Pipeline visibility is read-only — no actor resolution needed. |
| FP-017 | Observability is read-only — no actor resolution needed. |

### Core Problem

The `actorRole` field in review action request DTOs is **client-provided**. In the current internal-only admin, this is acceptable because:
- There is no public-facing API
- All callers are trusted internal admin UI
- No production users exist yet

However, before any production auth integration, the system must:
1. **Never trust client-provided actorRole as authentication** in production
2. **Resolve actor identity/role from authenticated context**
3. **Preserve current test/dev compatibility** during the transition
4. **Maintain safe audit attribution**

---

## 3. Product Intent

- **Production-safe admin identity boundary** — prepare the codebase so that when production auth arrives, actor identity is resolved server-side, not taken from request body
- **Clear admin/reviewer responsibility** — formalize which actions require which roles via server-resolved context
- **Safer audit actor attribution** — audit records should reference resolved identity, not self-declared role
- **Future auth integration without changing workflow semantics** — the review state machine, action rules, and safety guardrails remain identical; only the source of actor identity changes

---

## 4. Explicit Non-Goals

FP-018 does **NOT**:

| Non-Goal | Reason |
|----------|--------|
| Implement production auth (login/session/JWT) | Separate future feature |
| Add login/session/security config changes | Out of scope |
| Change endpoints | Endpoint count stays 21 |
| Add migrations/schema changes | No DB changes |
| Add publish/export | Out of scope |
| Add public/signed/CDN delivery | Out of scope |
| Add provider/storage exposure | Out of scope |
| Add automatic approval | Forbidden |
| Add generic status update | Forbidden |
| Expose tokens/secrets/JWTs | Forbidden |

---

## 5. Actor Model Baseline

### Actor Roles

| Role | Description | Can Approve? | Can Create Reviews? | Can Handoff? | Can Request Revision? |
|------|-------------|:------------:|:-------------------:|:------------:|:---------------------:|
| CREATOR | Content creator | ❌ No | ✅ Yes (submit) | ❌ No | ❌ No |
| REVIEWER | Assigned reviewer | ✅ Yes | ❌ No | ✅ Yes (reassign) | ✅ Yes |
| ADMIN | Admin operator | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| SYSTEM | Internal automation | ❌ No | ✅ Yes (auto-submit) | ✅ Yes (internal) | ❌ No |

### Current Trust Model

```
[Client Request] → actorRole: "ADMIN" → [Controller] → [Service] → trusted as-is
```

This is **not authentication**. The `actorRole` field is a self-declaration.

### Target Trust Model (future)

```
[Authenticated Request] → [AdminActorResolver] → ResolvedAdminActor → [Service] → trusted
                                                     ↑
                                           resolved from auth context,
                                           NOT from request body
```

---

## 6. Current Action Boundary Inventory

### Read-Only Endpoints (no actor resolution needed)

| Endpoint | Category | Future Auth Requirement |
|----------|----------|------------------------|
| GET /api/admin/reviews/state/{targetType}/{targetId} | Review state | Admin visibility |
| GET /api/admin/reviews/audit/{targetType}/{targetId} | Audit timeline | Admin visibility |
| GET /api/admin/reviews/worklist | Worklist | Admin visibility |
| GET /api/admin/reviews/context/{targetType}/{targetId} | Target context | Admin visibility |
| GET /api/admin/reviews/actions/{targetType}/{targetId} | Available actions | Admin visibility |
| GET /api/admin/reviews/preview/{targetType}/{targetId} | Preview | Admin visibility |
| GET /api/admin/reviews/readiness/{targetType}/{targetId} | Readiness | Admin visibility |
| GET /api/admin/reviews/approved-readiness/{targetType}/{targetId} | Approved readiness | Admin visibility |
| GET /api/admin/pipeline/overview | Pipeline overview | Admin visibility |
| GET /api/admin/pipeline/{targetType}/{targetId} | Pipeline detail | Admin visibility |
| GET /api/admin/workflow-observability/summary | Observability summary | Admin visibility |
| GET /api/admin/workflow-observability/diagnostics | Observability diagnostics | Admin visibility |

All 12 GET endpoints: **read-only admin visibility**. Future auth: require authenticated admin session, but no per-action role check needed.

### Review Action Endpoints (actor resolution required)

| Endpoint | Action | Allowed Roles | Future Auth |
|----------|--------|---------------|-------------|
| POST .../submit-for-review | Submit | CREATOR, ADMIN | Resolved actor |
| POST .../start-review | Start review | REVIEWER, ADMIN | Resolved actor |
| POST .../request-changes | Request changes | REVIEWER, ADMIN | Resolved actor |
| POST .../approve-internal | Approve | REVIEWER, ADMIN | Resolved actor |
| POST .../reject | Reject | REVIEWER, ADMIN | Resolved actor |
| POST .../return-to-draft | Return to draft | REVIEWER, ADMIN | Resolved actor |
| POST .../add-note | Add note | ALL | Resolved actor |
| POST .../handoff | Handoff | REVIEWER, ADMIN, SYSTEM | Resolved actor |
| POST .../request-internal-revision | Request revision | REVIEWER, ADMIN | Resolved actor |

All 9 POST endpoints: **require resolved actor identity** in production.

---

## 7. Future Actor Resolution Design

### Proposed Components

#### AdminActorResolver (Service)
```
Responsibility: Resolve actor identity/role from request context.
Methods:
  - resolve(HttpServletRequest | SecurityContext) → ResolvedAdminActor
  - resolveFromHeader(String authHeader) → ResolvedAdminActor  (future)
  - fallbackForTest(String actorRole, String actorRef) → ResolvedAdminActor
```

#### ResolvedAdminActor (DTO)
```
Fields:
  - actorRole: ReviewActorRole
  - actorReference: String (safe internal reference, not raw principal)
  - resolutionMode: AdminActorResolutionMode
  - resolvedAt: LocalDateTime
  - safeDisplayName: String (optional, sanitized)

Forbidden fields:
  - token, jwt, sessionId, password, principal (raw), credentials
```

#### AdminActorResolutionMode (Enum)
```
Values:
  - AUTHENTICATED        (production: resolved from auth context)
  - TEST_FALLBACK        (test/dev: explicit fallback with warning)
  - SYSTEM_INTERNAL      (system actions: no human auth needed)

Forbidden values:
  - CLIENT_TRUSTED       (never trust client-provided role as auth)
  - ANONYMOUS            (anonymous admin actions are not allowed)
```

#### SafeActorResolutionException
```
Safe exception when resolution fails.
No stack trace in response.
No token/credential leak.
Safe error code + message only.
```

### Design Goals
- Resolve actor from authenticated context in production
- Allow controlled test/dev fallback with explicit mode marker
- Never trust client actorRole blindly in production mode
- Preserve safe audit attribution (resolved role, not self-declared)
- Keep toString() safe (no token/credential/principal dump)

---

## 8. Compatibility Strategy

### Phased Migration

#### Phase 1 — Resolver Abstraction (Task 02)
- Create AdminActorResolver, ResolvedAdminActor, AdminActorResolutionMode
- Default mode: TEST_FALLBACK (wraps current request actorRole)
- No controller changes yet
- No behavioral changes
- Existing tests pass unmodified

#### Phase 2 — Controller Integration (Task 03–04)
- Update POST action controllers to use AdminActorResolver
- Resolver initially delegates to request actorRole (TEST_FALLBACK mode)
- Service layer receives ResolvedAdminActor instead of raw actorRole
- All tests pass — behavior identical
- Audit records start recording resolutionMode

#### Phase 3 — Request Field Downgrade (Task 05)
- Mark request actorRole as test/debug-only in frontend
- UI labels clarify "Geliştirme Ortamı Rol Seçimi" (Development Environment Role Selection)
- No production auth yet, but boundary is clear

#### Phase 4 — Production Auth (future, not in FP-018)
- Integrate real auth provider (Spring Security, OAuth2, etc.)
- AdminActorResolver switches to AUTHENTICATED mode
- Request actorRole field ignored in production
- Audit records show AUTHENTICATED resolution

### Compatibility Guarantees
- All existing tests continue to pass throughout phases 1–3
- No endpoint count changes
- No migration/schema changes
- No behavioral changes to review state machine
- SYSTEM/CREATOR restrictions preserved

---

## 9. Safe DTO/API Direction

### Request DTOs
- May retain `actorRole` field temporarily for test/dev compatibility
- Future production behavior should prefer resolved actor from auth context
- Field should be documented as "non-auth, test/dev only"
- No new request fields for tokens/credentials/session IDs

### Response DTOs
- Must not expose tokens, security internals, session details
- No raw principal/JWT/session dump
- Safe actor reference only (sanitized string)
- resolutionMode may be included in admin responses for transparency

### Forbidden Response Fields
- token, jwt, sessionId, password, credentials, principal (raw)
- stackTrace, exceptionClass (existing rule)
- publicUrl, signedUrl, cdnUrl, providerUrl (existing rule)

---

## 10. Audit Direction

### Current State
- Audit records store `actorRole` and `actorReference` from request
- These are self-declared by the client

### Target State
- Audit records should store resolved actor identity/role
- `actorReference` should be a safe internal user reference
- `resolutionMode` should be recorded for traceability
- No sensitive identity data (email PII only if explicitly consented)
- No token/JWT/session dump in audit records
- Safe internal user reference only

### Migration Path
- Phase 1: Add `resolutionMode` field to audit (optional, nullable)
- Phase 2: Populate from ResolvedAdminActor
- Phase 3: Production auth populates real identity
- No breaking changes to existing audit schema

---

## 11. Frontend/Admin Direction

### Current State
- ReviewConsole has actorRole/actorReference inputs
- User selects their role freely

### Target State
- UI should not imply actorRole selection is real authentication
- Label should clarify: "Geliştirme Ortamı — Bu rol seçimi üretimde kullanılmaz"
- Future production UI should not let users select elevated roles
- Test/demo role selectors must be clearly marked as non-production
- No auth UI implementation in FP-018

### No Implementation in Task 01
- Frontend auth integration is out of scope for FP-018
- Only labels and documentation changes in Task 05

---

## 12. Security Boundaries

FP-018 must NOT introduce:

| Boundary | Status |
|----------|--------|
| Secrets/tokens/session IDs/JWT exposure | ❌ Forbidden |
| Stack traces in responses | ❌ Forbidden |
| Unsafe logs (credentials, principals) | ❌ Forbidden |
| Client-controlled privilege escalation | ❌ Forbidden (resolver prevents this) |
| Production auth implementation | ❌ Not in FP-018 (future) |
| Login/session/cookie management | ❌ Not in FP-018 |
| OAuth2/OIDC/SAML integration | ❌ Not in FP-018 |

FP-018 DOES prepare:
- Resolver abstraction that will accept auth context when available
- Clear boundary between "test fallback" and "authenticated" modes
- Safe exception handling for resolution failures

---

## 13. Test Strategy

Later FP-018 tasks should test:

### Backend
- AdminActorResolver resolves TEST_FALLBACK correctly
- AdminActorResolver rejects null/empty input safely
- ResolvedAdminActor DTO forbidden fields (no token, JWT, etc.)
- ResolvedAdminActor toString() safe
- AdminActorResolutionMode has no CLIENT_TRUSTED or ANONYMOUS
- Controllers that use resolver preserve existing behavior
- SYSTEM cannot approve (preserved)
- CREATOR cannot approve (preserved)
- Action permission matrix unchanged
- Audit records include resolutionMode when available
- No endpoint count changes (stays 21)
- No migration/schema/config changes

### Frontend
- Role selector labeled as non-production if modified
- No auth UI controls
- No token/credential fields
- Existing panel integration preserved

---

## 14. Dense FP-018 Task Sequence

| Task | Title | Scope |
|------|-------|-------|
| 01 | Combined Design | Docs only (this document) |
| 02 | Admin Actor Resolver DTO + Service Foundation | Backend production + tests |
| 03 | Review Action Controller Actor Resolution Integration | Backend production + tests |
| 04 | Handoff/Internal Action Actor Boundary Hardening | Backend production + tests |
| 05 | Frontend Actor Display/Compatibility Cleanup | Frontend + tests |
| 06 | Auth Boundary Regression & Safety Hardening | Tests + docs |
| 07 | Final Closure / Operational Baseline | Docs only |

### Task 02 Scope Preview
- Create AdminActorResolver service
- Create ResolvedAdminActor DTO
- Create AdminActorResolutionMode enum
- Create SafeActorResolutionException
- Default: TEST_FALLBACK mode (wraps request actorRole)
- No controller changes
- No endpoint changes
- Tests: resolver safety, DTO forbidden fields, mode enum, toString

---

## 15. Final Recommendation

**Proceed with Task 02 — Admin Actor Resolver DTO + Service Foundation**

Create the resolver abstraction with TEST_FALLBACK default mode. This establishes the boundary without changing any existing behavior. All 2562 backend tests and 680 frontend tests should continue to pass.

---

## Standing Safety Baseline

FP-018 preserves the Anadolu Atlası safety baseline:
- No publish/export
- No public/signed/CDN delivery
- No auto-approval
- No generic status update
- No provider/storage/raw/base64/secret exposure
- No unsafe HTML
- No migration/schema/config changes (unless explicitly scoped in later tasks)
