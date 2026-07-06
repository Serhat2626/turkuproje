# FP-015 Task 04 — Admin UI Internal Revision Request + Approved Inventory

## Overview

Adds admin-web UI panels for:
1. **Approved Internal Inventory Panel** — lists all APPROVED_FOR_INTERNAL_USE items
2. **Internal Revision Request Panel** — REVIEWER/ADMIN can request revision
3. Integration with existing ReviewWorklist and InternalApprovalReadinessPanel

## Safety Baseline

- No publish/export/CDN/public delivery
- No public URL / signed URL
- No provider/storage/raw/base64/secret exposure
- No unsafe HTML rendering (no dangerouslySetInnerHTML, no innerHTML)
- No automatic approval or generic status update
- No migration/schema/config changes
- Backend endpoint count unchanged: 17 (9 POST + 8 GET)
- Labels emphasize internal-only nature

## New Types

| Type | Fields |
|------|--------|
| AdminInternalRevisionRequest | actorRole, actorReference, safeReasonCode?, safeMessage? |
| AdminInternalRevisionResponse | targetType, targetId, previousState, currentState, actionApplied, safeReasonCode, safeMessage, appendedNote, auditRecorded, updatedAt |

## New API Method

| Method | Endpoint |
|--------|----------|
| requestInternalRevision(targetType, targetId, data) | POST /admin/reviews/{targetType}/{targetId}/request-internal-revision |

## New Components

| Component | Purpose |
|-----------|--------|
| ApprovedInternalInventoryPanel | Lists approved items, filterable by target type |
| InternalRevisionRequestPanel | Revision request form (REVIEWER/ADMIN only) |

## Modified Components

| Component | Change |
|-----------|--------|
| InternalApprovalReadinessPanel | REQUEST_INTERNAL_REVISION activated (no longer deferred) |
| ReviewWorklist | Integrates inventory + revision panels, loads readiness |

## UI Rules Applied

1. All panels labeled as "Dahili" / "Sadece İç Kullanım"
2. Disclaimers on inventory and revision panels
3. READY means internal readiness, not publish/export
4. Revision request does not look like publish/export/regenerate
5. Safe text rendering via safeText() utility
6. Loading/error/empty states for all panels
7. Actor-gated: only REVIEWER/ADMIN see revision panel
8. State-gated: only APPROVED_FOR_INTERNAL_USE and CHANGES_REQUESTED eligible

## Test Coverage

- reviewApi: requestInternalRevision exists, no unsafe methods
- ApprovedInternalInventoryPanel: render, loading, error, empty, safety
- InternalRevisionRequestPanel: eligible, ineligible, success, error, safety
- ReviewWorklist: integration, new panel rendering
- Regression: no dangerouslySetInnerHTML/innerHTML, no img/base64/storage
