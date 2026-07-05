# FP-012 Task 08 — Admin Review API Authorization Boundary, Observability & Regression Hardening

**Document Status:** Accepted — Task 08 complete  
**Feature:** FP-012 — Admin Review Console & Human Approval Workflow Hardening  
**Date:** 2026-07-06

---

## 1. Scope

This document defines the internal admin review API boundary, authorization assumptions,
safe observability rules, and regression coverage for the `/api/admin/reviews/**` surface
introduced in Task 07.

No new business endpoints, authentication framework, or runtime instrumentation are introduced.

---

## 2. Admin API Boundary

- `/api/admin/reviews/**` is an **internal/admin API surface**.
- This surface is NOT public-facing. It must not be exposed to unauthenticated external callers.
- This task does **not** implement a new authentication/authorization framework.
- Production deployment **must** protect admin routes using existing auth/edge/network policy
  (e.g., API gateway, reverse proxy, internal-only network, VPN, IP allowlist, or platform-level IAM).
- The `actorRole` field in the request body is an **internal workflow input only**.
  It is NOT a replacement for real authentication or authorization.
- Future auth integration must map an authenticated admin identity to a safe actor reference.
- `CREATOR` and `SYSTEM` cannot approve regardless of request body content.

---

## 3. Authorization Assumptions

| Assumption | Status |
|------------|--------|
| Admin routes protected at edge/network | Required for production |
| actorRole is workflow input, not auth | Documented |
| CREATOR cannot approve | Enforced by ReviewTransitionPolicy |
| SYSTEM cannot approve | Enforced by ReviewTransitionPolicy |
| REVIEWER/ADMIN can approve from IN_REVIEW only | Enforced by ReviewTransitionPolicy |
| actorRole cannot bypass transition policy | Enforced by ReviewDomainService |
| actorRole cannot create publish/export behavior | No publish/export exists |
| No automatic approval path | Enforced by ReviewTransitionPolicy |
| AI_GENERATED does not imply approval | No auto-approval transition |
| APPROVED_FOR_INTERNAL_USE is NOT publish/export | Internal only, documented |

---

## 4. Actor Role Handling

- `actorRole` is accepted in the request body as a workflow semantic input.
- It does NOT grant privileges. Transition policy enforces all role-based restrictions.
- In production, the authenticated identity should be used to derive the actor reference.
- The actor role should be validated against the authenticated user's real roles.
- Until real auth integration, the current model is safe because:
  - No publish/export behavior exists.
  - No public/signed URL generation exists.
  - No storage/provider side effects exist.
  - Transition policy blocks forbidden actor+action combinations.

---

## 5. Explicit Endpoint Inventory

### Implemented (Task 07)

| Method | Path | Action |
|--------|------|--------|
| POST | /api/admin/reviews/submit | Submit for review |
| POST | /api/admin/reviews/start | Start review |
| POST | /api/admin/reviews/request-changes | Request changes |
| POST | /api/admin/reviews/approve-internal | Approve for internal use only |
| POST | /api/admin/reviews/reject | Reject content |
| POST | /api/admin/reviews/return-to-draft | Return to draft |
| POST | /api/admin/reviews/note | Add review note |
| GET | /api/admin/reviews/{targetType}/{targetId}/state | Get review state |
| GET | /api/admin/reviews/{targetType}/{targetId}/audit | Get audit timeline |

### Forbidden Endpoints — Must NEVER Be Added

| Forbidden Path Fragment | Reason |
|-------------------------|--------|
| publish | No publish behavior |
| export | No export behavior |
| complete / completed | No COMPLETED state |
| ready-for-publish | No such state |
| public-url | No public URL delivery |
| signed-url | No signed URL delivery |
| cdn | No CDN delivery |
| provider-url | No provider URL exposure |
| storage-key | No storage key exposure |
| generic-status | No generic status update |
| config-inspection | No config exposure |

---

## 6. Safe Request Logging Rules

- **Do NOT log** full request body.
- **Do NOT log** note content.
- **Do NOT log** raw reason text.
- **Do NOT log** actorReference unless existing privacy policy permits safe opaque references.
- **Do NOT log** targetId in high-cardinality metrics.
- **Do NOT log** provider/storage/config data (none exists in review request, but guard against future regression).
- **Do NOT log** stack traces in admin-facing responses.

Safe to log per request:
- actionType (enum)
- targetType (enum)
- actorRole (enum)
- HTTP status code
- timestamp

---

## 7. Safe Response Logging Rules

- **Do NOT log** response body content.
- **Do NOT log** safeMessage/safeReasonCode at high volume (prefer metrics).
- **Do NOT expose** stack traces.
- **Do NOT log** provider URLs, storage keys, secrets, raw payloads, base64.

---

## 8. Safe Metrics and Labels

### Recommended Metric Names

| Metric | Type | Description |
|--------|------|-------------|
| `admin_review_action_total` | Counter | Successful review actions |
| `admin_review_action_rejected_total` | Counter | Rejected/invalid review actions |
| `admin_review_transition_violation_total` | Counter | Transition policy violations |
| `admin_review_state_query_total` | Counter | State query requests |
| `admin_review_audit_query_total` | Counter | Audit timeline query requests |

### Allowed Labels

| Label | Source |
|-------|--------|
| `action_type` | ReviewActionType enum |
| `target_type` | ReviewTargetType enum |
| `review_state` | ReviewState enum |
| `result` | success / error |
| `error_code` | safe error classification |
| `actor_role` | ReviewActorRole enum |

### Forbidden Labels — Must NEVER Be Used

| Forbidden Label | Reason |
|-----------------|--------|
| `actor_reference` | High cardinality, PII risk |
| `target_id` | High cardinality |
| `note` | Sensitive content |
| `reason_text` | Sensitive content |
| `provider_url` | No provider URL in review |
| `storage_key` | No storage key in review |
| `bucket` / `path` | No storage paths in review |
| `raw_payload` | No raw payloads |
| `base64` | No base64 content |
| `secret` / `api_key` / `config` | Never expose secrets |
| `public_url` / `signed_url` | No URL delivery |

---

## 9. Safe Alert Candidates

| Alert | Trigger | Severity |
|-------|---------|----------|
| High transition violation rate | `admin_review_transition_violation_total` spike | Warning |
| Unexpected action type distribution | Unusual action_type label values | Info |
| High error rate on review actions | `admin_review_action_rejected_total` > threshold | Warning |
| Audit query volume spike | `admin_review_audit_query_total` spike | Info |

---

## 10. Error Handling and Incident Triage

| Error Type | HTTP Status | Triage |
|------------|-------------|--------|
| ReviewApplicationException | 404 | Missing review record — check target existence |
| ReviewTransitionViolationException | 409 | Invalid transition — check state + action + role |
| IllegalArgumentException | 400 | Validation error — check request fields |

All error responses are safe:
- No stack trace
- No provider URL / storage key / secret / config
- No raw payload / base64
- No bucket/path sensitive detail

---

## 11. Abuse/Misuse Scenarios

| Scenario | Mitigation |
|----------|------------|
| Attacker sends APPROVE with CREATOR role | ReviewTransitionPolicy blocks — 409 CONFLICT |
| Attacker sends PUBLISH action type | ReviewActionType enum has no PUBLISH — 400 BAD_REQUEST |
| Attacker sends generic status string | Request uses ReviewActionType enum, not free text |
| Attacker tries to read provider URLs | No provider URL field exists in any DTO |
| Attacker floods review note endpoint | Note is sanitized + length-limited; rate limiting should be added at edge |
| Attacker inspects error for secrets | Error responses are safe — no stack trace / secrets / config |

---

## 12. Regression Coverage

| Test Area | Test File |
|-----------|-----------|
| Endpoint inventory | AdminReviewApiBoundaryRegressionTest |
| DTO forbidden fields | AdminReviewApiBoundaryRegressionTest |
| Actor/authorization boundary | AdminReviewApiBoundaryRegressionTest |
| Safe error regression | AdminReviewApiBoundaryRegressionTest |
| Observability doc safety | AdminReviewApiBoundaryRegressionTest |
| Forbidden status/action | AdminReviewApiBoundaryRegressionTest |
| Boundary regression | AdminReviewApiBoundaryRegressionTest |

---

## 13. Security Acceptance Criteria

- [x] Admin routes documented as internal-only
- [x] actorRole documented as workflow input, not authentication
- [x] CREATOR/SYSTEM cannot approve — enforced and tested
- [x] No publish/export endpoint exists
- [x] No public/signed URL endpoint exists
- [x] No provider/storage inspection endpoint exists
- [x] No generic status update endpoint exists
- [x] Safe observability rules defined
- [x] Forbidden metrics labels defined
- [x] Error responses are safe — no secrets/stack traces
- [x] All DTOs have no forbidden fields
- [x] Comprehensive regression tests pass

---

## 14. Next Step

- FP-012 closure task (Task 09 or equivalent) should:
  - Run full test suite and record final baseline
  - Produce final closure document
  - Confirm FP-012 CLOSED status
