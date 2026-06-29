/**
 * FP-001: Action visibility helper fonksiyonları.
 *
 * Bu fonksiyonlar backend service'lerdeki iş kurallarıyla birebir uyumlu olmalıdır.
 * Source of truth: backend entity status enums ve service sınıflarındaki Set/EnumSet tanımları.
 *
 * Generic status update yoktur — sadece action-specific endpoint'ler kullanılır.
 */

// ── Knowledge Item ──────────────────────────────────────────
// Source: KnowledgeItemService.java

/** Mark Ready: sadece DRAFT → READY */
export function canMarkKnowledgeItemReady(status: string): boolean {
  return status === 'DRAFT';
}

/** Archive: DRAFT veya READY → ARCHIVED */
export function canArchiveKnowledgeItem(status: string): boolean {
  return status === 'DRAFT' || status === 'READY';
}

// ── Content Project ─────────────────────────────────────────
// Source: ContentProjectService.java, AiGenerationService.java

/** Generate AI Draft: CREATED veya REVISION_REQUESTED */
export function canGenerateAiDraft(status: string): boolean {
  return status === 'CREATED' || status === 'REVISION_REQUESTED';
}

/** Retry AI Draft: sadece FAILED */
export function canRetryAiDraft(status: string): boolean {
  return status === 'FAILED';
}

/** Start Review: sadece DRAFT_READY */
export function canStartReview(status: string): boolean {
  return status === 'DRAFT_READY';
}

/** Approve: sadece IN_REVIEW */
export function canApproveProject(status: string): boolean {
  return status === 'IN_REVIEW';
}

/** Request Revision: sadece IN_REVIEW */
export function canRequestRevision(status: string): boolean {
  return status === 'IN_REVIEW';
}

/** Reject: sadece IN_REVIEW */
export function canRejectProject(status: string): boolean {
  return status === 'IN_REVIEW';
}

/** Mark Ready For Publish: sadece APPROVED */
export function canMarkReadyForPublish(status: string): boolean {
  return status === 'APPROVED';
}

/**
 * Cancel: CREATED, AI_REQUESTED, AI_PROCESSING, DRAFT_READY, IN_REVIEW, REVISION_REQUESTED
 * Source: ContentProjectService.CANCELLABLE_STATUSES
 */
const CANCELLABLE_STATUSES = new Set([
  'CREATED', 'AI_REQUESTED', 'AI_PROCESSING',
  'DRAFT_READY', 'IN_REVIEW', 'REVISION_REQUESTED',
]);

export function canCancelProject(status: string): boolean {
  return CANCELLABLE_STATUSES.has(status);
}

// ── Content Draft ───────────────────────────────────────────
// Source: ContentDraftService.EDITABLE_STATUSES

/** Edit Draft: sadece GENERATED veya REVISION_NEEDED */
export function canEditDraft(status: string): boolean {
  return status === 'GENERATED' || status === 'REVISION_NEEDED';
}
