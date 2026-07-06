# FP-014 Review Decision Execution & Internal Approval Follow-through — Combined Product / AI Platform / Engineering Design

## 1. Status

| Field | Value |
|-------|-------|
| Feature | FP-014 — Review Decision Execution & Internal Approval Follow-through |
| Status | **PLANNED / DESIGN ONLY** |
| Task | Task 01 — Combined Design |
| Type | Documentation-only |
| Date | 2026-07-06 |
| Runtime Behavior Changes | None |
| Code Changes | None |
| Test Changes | None |
| Migration Changes | None |
| Endpoint Changes | None |

---

## 2. Background

### Feature Chain Context

FP-012 created the human review workflow with safe states, actors, transitions, audit trail,
and an admin review console. Content enters review via explicit submission or generation handoff.

FP-013 connected AI-generated content and assets into that workflow. Every successful
generation now produces a review-gated artifact that reaches `NEEDS_REVIEW`. Reviewers
can inspect safe admin previews, claim reviews, request changes, approve for internal use,
or reject.

### Current State

- Generated outputs reach `NEEDS_REVIEW` automatically via `GenerationReviewHandoffAdapter`.
- Admin review console supports full review lifecycle through 14 endpoints.
- `APPROVED_FOR_INTERNAL_USE` is the positive terminal review state — but it is
  **internal-only** and does **not** trigger publish, export, public delivery, or any
  external side effect.
- `REJECTED` is the negative terminal review state for the current version.
- After approval or rejection, **nothing further happens.** There is no downstream
  workflow, no readiness summary, no internal inventory of approved artifacts.

### The Gap FP-014 Addresses

FP-014 defines what happens after review decisions — especially after
`APPROVED_FOR_INTERNAL_USE` — without crossing into publish/export/public delivery.
It creates safe internal visibility into review outcomes so that admins and future
workflows can understand which artifacts are internally approved and ready for
controlled downstream use.

---

## 3. Product Intent

### What Internal Approval Means

When a reviewer marks an artifact as `APPROVED_FOR_INTERNAL_USE`, they are asserting:

- The AI-generated content has been reviewed by a human.
- The content meets internal quality standards for the current editorial cycle.
- The content is safe and accurate enough for internal team use.
- The content is **not** approved for public distribution, external sharing, or
  automated delivery.

### What Reviewers/Admins Need After Approval

After approving artifacts, the editorial team needs:

1. **Visibility:** Which artifacts across the project are internally approved?
2. **Completeness:** Is a content project's full pipeline (scenario → storyboard → 
   visual assets → platform package) internally approved end-to-end?
3. **Next steps:** What internal actions are available for approved artifacts?
4. **Safety assurance:** Confirmation that approved content has not been accidentally
   published or made externally accessible.
5. **Revision path:** Ability to send approved content back for revision if
   post-approval issues are discovered.

### Why Approval Follow-through Is Useful

Without follow-through visibility:
- Admins cannot determine project readiness without querying each artifact individually.
- There is no aggregated view of internal approval status across a content pipeline.
- The gap between "reviewed" and "actionable" creates operational friction.
- Future publish/export features have no clean readiness signal to build on.

### Why It Must Remain Separate from Publish/Export

Internal approval is an editorial quality gate. Publication is a distribution decision.
These are fundamentally different concerns:

| Concern | Internal Approval | Publication |
|---------|------------------|-------------|
| Who decides | Reviewer/Admin | Product/Business (future) |
| What it means | Quality-checked | Publicly distributed |
| Reversibility | Can request revision | May require retraction |
| External effect | None | User-facing content |
| Infrastructure | Review workflow | CDN, URL signing, media library |

FP-014 operates entirely within the internal approval domain. Publication, export,
public delivery, CDN, signed URLs, and user-facing media library remain future features
that will build upon — but are strictly separate from — FP-014.

---

## 4. Explicit Non-Goals

FP-014 does **NOT** include:

| Non-Goal | Status |
|----------|--------|
| Publish / export | ❌ Out of scope |
| Public URL generation or delivery | ❌ Out of scope |
| Signed URL generation or delivery | ❌ Out of scope |
| CDN / public delivery | ❌ Out of scope |
| User-facing media library | ❌ Out of scope |
| Video / audio processing | ❌ Out of scope |
| Automatic approval | ❌ Forbidden |
| Generic status update endpoint | ❌ Forbidden |
| Production auth integration | ❌ Deferred |
| Provider / storage inspection endpoint | ❌ Forbidden |
| Provider URL exposure | ❌ Forbidden |
| Storage key exposure | ❌ Forbidden |
| Raw payload / base64 exposure | ❌ Forbidden |
| Secret / API key / config exposure | ❌ Forbidden |
| Stack trace display | ❌ Forbidden |
| Unsafe HTML rendering | ❌ Forbidden |
| New migration in Task 01 | ❌ Not in this task |
| Endpoint implementation in Task 01 | ❌ Not in this task |

---

## 5. Review Decision Model Baseline

### FP-012 Review States

| State | Description | Terminal |
|-------|-------------|----------|
| `DRAFT` | Content project created, no AI generation yet | No |
| `AI_GENERATED` | AI output produced — awaiting human review | No |
| `NEEDS_REVIEW` | Explicitly queued for reviewer attention | No |
| `IN_REVIEW` | Reviewer has claimed and started review | No |
| `CHANGES_REQUESTED` | Reviewer requested modifications | No |
| `APPROVED_FOR_INTERNAL_USE` | Approved — internal use only, NOT publish/export | Yes (review) |
| `REJECTED` | Rejected — terminal for this version | Yes (review) |

### FP-012 Review Actions

| Action | Actor | Effect |
|--------|-------|--------|
| `SUBMIT_FOR_REVIEW` | CREATOR, REVIEWER, ADMIN | → NEEDS_REVIEW |
| `START_REVIEW` | REVIEWER, ADMIN | → IN_REVIEW |
| `REQUEST_CHANGES` | REVIEWER, ADMIN | → CHANGES_REQUESTED |
| `APPROVE_FOR_INTERNAL_USE` | REVIEWER, ADMIN | → APPROVED_FOR_INTERNAL_USE |
| `REJECT` | REVIEWER, ADMIN | → REJECTED |
| `RETURN_TO_DRAFT` | CREATOR, REVIEWER, ADMIN | → DRAFT |
| `ADD_REVIEW_NOTE` | CREATOR, REVIEWER, ADMIN | No state change |

### Critical Rules

- `APPROVED_FOR_INTERNAL_USE` is terminal for the review workflow but **not** for publication.
- `REJECTED` is terminal for the current version.
- `CHANGES_REQUESTED` returns work to creators/generation teams for revision.
- `AI_GENERATED` does **not** imply approval.
- `SYSTEM` **cannot** approve or reject.
- `CREATOR` **cannot** approve.
- `READY_FOR_PUBLISH`, `COMPLETED`, `PUBLISHED`, `EXPORTED` do **not** exist.

---

## 6. FP-014 Concept: Internal Approval Follow-through

### Safe Follow-through Concepts

| Concept | Description | Scope |
|---------|-------------|-------|
| **Internal Approval Readiness View** | Derived read-only view showing whether a target is internally approved | Core FP-014 |
| **Internal Approved Inventory** | List of all internally approved artifacts, filterable by target type | Core FP-014 |
| **Pipeline Readiness Summary** | Aggregated view: is an entire content project's pipeline approved? | Core FP-014 |
| **Internal Approval Event Audit** | Approval decision details visible in audit timeline | Already exists (FP-012) |
| **Safe Internal Handoff Candidate List** | Which approved artifacts are candidates for future downstream use | Core FP-014 |
| **Review Decision Outcome DTO** | Safe summary of review decision for internal consumers | Core FP-014 |
| **Internal Revision Request** | Admin sends approved artifact back for re-review | Candidate FP-014 |
| **Internal Approval Notification Event** | Domain event emitted on approval (for future listeners) | Deferred |
| **Internal Package Readiness Check** | Per-platform readiness within a package | Candidate FP-014 |

### Critical Boundary

These concepts are **internal administrative views**. They do not:
- Trigger publish or export
- Create public or signed URLs
- Deliver content to end users
- Expose storage or provider infrastructure

---

## 7. Target Types Covered

### CONTENT_PROJECT

| Field | Value |
|-------|-------|
| Internal approval means | The overall content project draft is editorially approved |
| Safe follow-through exposes | Safe title, topic, niche, language, approval date, approver role |
| Must remain hidden | No raw AI response, no provider detail |
| Deferred | Pipeline completeness (requires aggregating child approvals) |

### SCENARIO

| Field | Value |
|-------|-------|
| Internal approval means | The scenario/script draft meets editorial quality |
| Safe follow-through exposes | Safe title, beat count, version, approval date |
| Must remain hidden | No raw beat content in readiness DTO |
| Deferred | Scenario revision re-generation trigger |

### STORYBOARD

| Field | Value |
|-------|-------|
| Internal approval means | The storyboard scene breakdown is editorially approved |
| Safe follow-through exposes | Safe title, scene count, version, approval date |
| Must remain hidden | No raw scene content, no visual prompt detail |
| Deferred | Storyboard → image generation pipeline trigger |

### PACKAGE

| Field | Value |
|-------|-------|
| Internal approval means | Platform-specific content package meets editorial quality |
| Safe follow-through exposes | Safe title, platform, approval date, per-platform status |
| Must remain hidden | No platform credentials, no export/publish payloads |
| Deferred | Multi-platform aggregate readiness |

### IMAGE_ASSET

| Field | Value |
|-------|-------|
| Internal approval means | The generated image asset passes visual quality review |
| Safe follow-through exposes | Safe title, availability indicator, approval date |
| Must remain hidden | No provider URL, no storage key, no bytes, no base64, no signed URL |
| Deferred | Image asset delivery/display beyond metadata |

---

## 8. Safe Internal Approval Readiness Contract

### Candidate DTO: `InternalApprovalReadiness`

```java
@Getter
@Builder
public class InternalApprovalReadiness {

    private final ReviewTargetType targetType;
    private final UUID targetId;
    private final ReviewState currentState;
    private final boolean internallyApproved;
    private final boolean rejected;
    private final boolean pendingReview;

    // Safe metadata
    private final String safeTitle;
    private final String safeSummary;
    private final String safePreviewLabel;
    private final boolean safePreviewAvailable;

    // Approval detail (only when approved)
    private final LocalDateTime approvedAt;
    private final String approvedByRole;       // e.g., "REVIEWER", "ADMIN"
    private final String safeApprovalReasonCode;
    private final String safeApprovalMessage;

    // Safe next internal actions
    private final List<SafeInternalAction> safeNextInternalActions;

    // Safe warnings
    private final List<String> safeWarnings;

    private final LocalDateTime updatedAt;
}
```

### Forbidden Fields — NOT present in any FP-014 DTO

`publicUrl`, `signedUrl`, `cdnUrl`, `providerUrl`, `storageKey`, `bucket`, `objectPath`,
`rawPayload`, `base64`, `bytes`, `imageBytes`, `imageUrl`, `imageData`, `rawContent`,
`secret`, `apiKey`, `accessKey`, `token`, `stackTrace`, `exceptionClass`

---

## 9. Safe Internal Actions Taxonomy

### Candidate Enum: `SafeInternalAction`

| Value | Description | Executes? |
|-------|-------------|-----------|
| `VIEW_SAFE_PREVIEW` | View safe admin preview of the approved artifact | Read-only |
| `REVIEW_APPROVAL_DETAILS` | View approval audit timeline | Read-only |
| `REQUEST_INTERNAL_REVISION` | Send approved artifact back for re-review | State change |
| `VIEW_PIPELINE_READINESS` | View pipeline completion status for content project | Read-only |
| `MARK_INTERNAL_READY_CANDIDATE` | Informational marker for future downstream use | Metadata-only |

### Forbidden Actions — NOT included

| Forbidden Action | Reason |
|------------------|--------|
| `PUBLISH` | Out of scope — future feature |
| `EXPORT` | Out of scope — future feature |
| `PUBLIC_DELIVERY` | Out of scope — future feature |
| `CREATE_SIGNED_URL` | Out of scope — future feature |
| `CREATE_PUBLIC_URL` | Out of scope — future feature |
| `CDN_DELIVERY` | Out of scope — future feature |
| `USER_MEDIA_LIBRARY` | Out of scope — future feature |
| `AUTO_APPROVE` | Forbidden — violates review governance |

### Note on `MARK_INTERNAL_READY_CANDIDATE`

This action is informational only. It marks an artifact as a future downstream candidate
without triggering any external side effect. It does **not** publish, export, create URLs,
or make content externally accessible. It may be deferred if the product team does not
require it in FP-014's initial scope.

### Note on `PREPARE_FOR_FUTURE_EXPORT`

If this concept appears in any future design iteration, it must be explicitly **non-executing**
and **informational only**. It must not trigger export, create export packages, generate URLs,
or cause any external side effect. It is a readiness signal, not an execution trigger.

---

## 10. API Design Direction

### Candidate Future Internal Admin Endpoints

| Method | Path | Purpose | Task |
|--------|------|---------|------|
| GET | `/api/admin/reviews/approved/internal-readiness` | List all internally approved artifacts with readiness | Task 03 |
| GET | `/api/admin/reviews/{targetType}/{targetId}/internal-readiness` | Single target internal readiness | Task 03 |
| POST | `/api/admin/reviews/{targetType}/{targetId}/request-internal-revision` | Send approved artifact back for re-review | Task 03 (candidate) |

### Confirmation

- ❌ **No endpoint is added in Task 01.**
- ❌ No publish/export/public/signed/CDN endpoint is proposed as part of FP-014 core.
- All proposed endpoints are admin-internal and read-only (except optional revision request).
- Endpoint inventory at FP-014 completion is expected to be 14 + (2–3) = 16–17.

---

## 11. Frontend/Admin Design Direction

### Possible Admin UI Additions

| Component | Description | Task |
|-----------|-------------|------|
| **Approved Item Detail Panel** | Shows approval details, safe preview, next internal actions | Task 04 |
| **Internal Readiness Badge** | Visual indicator on review worklist for approved items | Task 04 |
| **Safe Next-Action Hints** | Contextual actions available for approved artifacts | Task 04 |
| **Approval Audit Summary** | Compact approval decision summary in timeline | Already exists |
| **Pipeline Readiness View** | Aggregated approval status for a content project's pipeline | Task 04 (candidate) |
| **Unavailable/Deferred Messaging** | Safe messaging for items not yet eligible for follow-through | Task 04 |

### Confirmation

- ❌ **No frontend code in Task 01.**
- IMAGE_ASSET still shows metadata/availability only unless a later safe preview feature
  explicitly expands this.
- No rendering of URL/base64/raw HTML.
- No `dangerouslySetInnerHTML` or `innerHTML` usage.
- ReviewPreviewPanel from FP-013 continues to function unchanged.

---

## 12. AI Platform / Orchestration Design Direction

### Generation Handoff Boundaries

- Generation handoff stops at the review gate (FP-013 baseline).
- Approval follow-through does **not** trigger re-generation or auto-fix.
- No automatic downstream publishing after approval.
- No provider/storage payload inspection.
- No provider callback exposure.
- No external side effects from approval follow-through.

### Future Orchestration Considerations

- If `REQUEST_INTERNAL_REVISION` is implemented, it transitions the review state back
  (e.g., to `CHANGES_REQUESTED`) but does **not** auto-trigger re-generation.
- Re-generation after revision request is a manual creator/admin action using existing
  generation endpoints.
- Any future "smart re-generation" triggered by review decisions would be a separate
  feature beyond FP-014.

---

## 13. Audit / Observability Direction

### Safe Audit Concepts

| Concept | Safety |
|---------|--------|
| Internal approval decision | Already recorded by FP-012 via `ReviewAuditEventEntity` |
| Readiness queries | Use safe summaries from `InternalApprovalReadiness` DTO |
| Safe metrics | May count approved internal candidates by target type |
| Metric labels | No high-cardinality target IDs unless existing policy allows |
| Log safety | No provider/storage/key/URL/secret in logs or metrics |

### Existing Audit Coverage

FP-012 already records:
- `INTERNAL_APPROVAL_GRANTED` audit event with actor, timestamp, reason
- `REVIEW_REJECTED` audit event
- `CHANGES_REQUESTED` audit event
- `REVIEW_NOTE_ADDED` audit event

FP-014 adds no new audit event types. Readiness views derive from existing audit data.

---

## 14. Security and Safety Boundaries

| Boundary | Preserved |
|----------|-----------|
| `actorRole` request field is not authentication | ✅ |
| Production auth integration remains deferred | ✅ |
| SYSTEM cannot approve | ✅ |
| CREATOR cannot approve | ✅ |
| Internal approval is not publication | ✅ |
| Internal readiness is not publication | ✅ |
| Preview is not delivery | ✅ |
| Storage/provider details stay hidden | ✅ |
| No automatic approval path | ✅ |
| No external side effects from readiness queries | ✅ |

---

## 15. Data / Migration Direction

### Recommendation: Start Without Migrations

FP-014 should begin with **derived/read-only readiness** computed from existing data:

- `ReviewRecordEntity` already stores target type, target ID, current state, timestamps.
- `ReviewAuditEventEntity` already stores approval decisions with actor and reason.
- `AdminPreviewService` already provides safe preview metadata.

The `InternalApprovalReadiness` DTO can be constructed entirely from existing data
without new database tables.

### Migration Policy

| Task | Migration Expected |
|------|-------------------|
| Task 01 | ❌ None (documentation-only) |
| Task 02 | ❌ None (derived from existing data) |
| Task 03 | ❌ None (API layer only) |
| Task 04 | ❌ None (frontend only) |
| Task 05 | ❌ None (testing only) |
| Task 06 | ❌ None (documentation-only) |

If a persistent readiness cache or internal-ready marker becomes necessary, it can be
introduced as a controlled schema addition in a later feature — not in FP-014.

---

## 16. Failure Behavior

| Scenario | Expected Behavior |
|----------|------------------|
| Missing target | Safe unavailable / readiness-unavailable result |
| Rejected target | Not an approved candidate — readiness shows rejected status |
| Non-approved target | Not internally ready — readiness shows current review state |
| IMAGE_ASSET readiness | Does not require public URL — shows availability only |
| Storage/provider failure | Does not leak details — safe error code/message only |
| Null targetType | Safe error response with reason code |
| Null targetId | Safe error response with reason code |
| Concurrent approval + readiness query | Read from committed state — no dirty reads |

---

## 17. Test Strategy

### Tests for Later Tasks

| Category | Task | Description |
|----------|------|-------------|
| Service unit tests | Task 02 | InternalApprovalReadinessService behavior |
| DTO safety tests | Task 02 | Forbidden field regression for new DTOs |
| Controller tests | Task 03 | Endpoint behavior, authorization, error handling |
| Endpoint inventory regression | Task 03 | Endpoint count verification |
| Serialization safety | Task 03 | JSON response field inspection |
| Admin-web render tests | Task 04 | Safe rendering, no URL/base64/HTML injection |
| Approval state preservation | Task 05 | Terminal state behavior under readiness queries |
| No automatic approval | Task 05 | SYSTEM/readiness cannot trigger approval |
| No publish/export regression | Task 05 | Forbidden behavior absence |
| Forbidden field regression | Task 05 | All DTOs, API responses, toString methods |
| E2E workflow | Task 05 | APPROVED_FOR_INTERNAL_USE → readiness → safe preview |

---

## 18. Dense FP-014 Task Sequence

### Task 02 — Safe Internal Approval Readiness Service + DTO Foundation

| Scope | Details |
|-------|---------|
| Type | Backend service + DTO |
| New classes | `InternalApprovalReadiness`, `SafeInternalAction`, `InternalApprovalReadinessService` |
| Data source | Derived from `ReviewRecordEntity`, `ReviewAuditEventEntity`, `AdminPreviewService` |
| Endpoints | None |
| Migration | None |
| Tests | Service unit tests, DTO safety tests, forbidden field regression |
| Safety | No provider/storage/URL/base64/publish/export |

### Task 03 — Internal Approval Readiness Admin API

| Scope | Details |
|-------|---------|
| Type | Backend controller + API |
| New endpoints | GET readiness list, GET single readiness, POST revision request (candidate) |
| Endpoint delta | +2 or +3 |
| Migration | None |
| Tests | Controller tests, endpoint inventory regression, JSON safety |
| Safety | Admin-internal only, no publish/export/CDN |

### Task 04 — Admin UI Internal Approval Follow-through Panel

| Scope | Details |
|-------|---------|
| Type | Frontend/admin |
| New components | Approved detail panel, readiness badge, next-action hints |
| API integration | Calls new readiness endpoints |
| Tests | Render safety, no URL/base64/HTML injection |
| Safety | No `dangerouslySetInnerHTML`, no img src from API, no raw HTML |

### Task 05 — Review Decision Outcome Regression & Safety Hardening

| Scope | Details |
|-------|---------|
| Type | Backend + frontend tests |
| Focus | E2E from APPROVED_FOR_INTERNAL_USE to readiness, terminal state preservation, forbidden behavior |
| Production code | None unless tests expose a real gap |
| Safety | Full forbidden behavior regression |

### Task 06 — Final Closure / Operational Baseline

| Scope | Details |
|-------|---------|
| Type | Documentation-only |
| Document | FP-014 final closure operational baseline |
| Code changes | None |
| Tests | Not rerun unless code changed |

---

## 19. Final Recommendation

### Start With

**Task 02 — Safe Internal Approval Readiness Service + DTO Foundation**

This task creates the core service and DTO layer that all subsequent tasks build on.
It is backend-only, derived from existing data, requires no migrations, and has clear
safety boundaries.

### Confirmed Constraints

| Constraint | Confirmed |
|------------|-----------|
| FP-014 starts from APPROVED_FOR_INTERNAL_USE | ✅ |
| Does not publish/export | ✅ |
| Does not create public/signed URLs | ✅ |
| Does not create CDN delivery | ✅ |
| Does not create user-facing media library | ✅ |
| Does not introduce automatic approval | ✅ |
| Does not expose provider/storage internals | ✅ |
| Internal readiness is not publication | ✅ |
