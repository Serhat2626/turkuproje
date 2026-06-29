/**
 * FP-001: Compile-time status consistency checks.
 *
 * Bu dosya TypeScript build sırasında status constant'larının
 * backend enum'larıyla uyumluluğunu doğrular.
 *
 * Eğer backend enum değişirse, burada compile hatası alınır.
 * Bu sayede frontend status drift'i build zamanında yakalanır.
 *
 * Runtime'da çalıştırılması gerekmez — sadece tsc tarafından kontrol edilir.
 */

import {
  KNOWLEDGE_ITEM_STATUSES,
  CONTENT_PROJECT_STATUSES,
} from '../types';
import {
  canMarkKnowledgeItemReady,
  canArchiveKnowledgeItem,
  canGenerateAiDraft,
  canRetryAiDraft,
  canStartReview,
  canApproveProject,
  canRequestRevision,
  canRejectProject,
  canMarkReadyForPublish,
  canCancelProject,
  canEditDraft,
} from './actionVisibility';

// ── 1. KnowledgeItemStatus Enum Check ───────────────────────
// Backend: DRAFT, READY, ARCHIVED
const KI_STATUS_VALUES = KNOWLEDGE_ITEM_STATUSES.map(s => s.value);
type KIStatusTuple = typeof KI_STATUS_VALUES;
type KIAssertDraft = KIStatusTuple extends readonly (infer U)[] ? 'DRAFT' extends U ? true : never : never;
type KIAssertReady = KIStatusTuple extends readonly (infer U)[] ? 'READY' extends U ? true : never : never;
type KIAssertArchived = KIStatusTuple extends readonly (infer U)[] ? 'ARCHIVED' extends U ? true : never : never;
const _kiCheck: [KIAssertDraft, KIAssertReady, KIAssertArchived] = [true, true, true];

// ── 2. ContentProjectStatus Enum Check ──────────────────────
// Backend: CREATED, AI_REQUESTED, AI_PROCESSING, DRAFT_READY, IN_REVIEW,
//          REVISION_REQUESTED, APPROVED, REJECTED, READY_FOR_PUBLISH, FAILED, CANCELLED
const CP_STATUS_VALUES = CONTENT_PROJECT_STATUSES.map(s => s.value);
type CPStatusTuple = typeof CP_STATUS_VALUES;
type CPAssert<V extends string> = CPStatusTuple extends readonly (infer U)[] ? V extends U ? true : never : never;
const _cpCheck: [
  CPAssert<'CREATED'>, CPAssert<'AI_REQUESTED'>, CPAssert<'AI_PROCESSING'>,
  CPAssert<'DRAFT_READY'>, CPAssert<'IN_REVIEW'>, CPAssert<'REVISION_REQUESTED'>,
  CPAssert<'APPROVED'>, CPAssert<'REJECTED'>, CPAssert<'READY_FOR_PUBLISH'>,
  CPAssert<'FAILED'>, CPAssert<'CANCELLED'>,
] = [true, true, true, true, true, true, true, true, true, true, true];

// ── 3. ContentProjectStatus Count Check ─────────────────────
// Backend has exactly 11 statuses
type AssertLength11 = typeof CONTENT_PROJECT_STATUSES extends readonly [
  unknown, unknown, unknown, unknown, unknown, unknown,
  unknown, unknown, unknown, unknown, unknown,
] ? true : never;
const _cpLengthCheck: AssertLength11 = true;

// ── 4. KnowledgeItemStatus Count Check ──────────────────────
// Backend has exactly 3 statuses
type AssertLength3 = typeof KNOWLEDGE_ITEM_STATUSES extends readonly [
  unknown, unknown, unknown,
] ? true : never;
const _kiLengthCheck: AssertLength3 = true;

// ── 5. Action Visibility Smoke Checks ───────────────────────
// These are compile-time type assertions ensuring functions accept string

// KI actions
const _ki1: boolean = canMarkKnowledgeItemReady('DRAFT');
const _ki2: boolean = canArchiveKnowledgeItem('DRAFT');
const _ki3: boolean = canArchiveKnowledgeItem('READY');

// CP actions
const _cp1: boolean = canGenerateAiDraft('CREATED');
const _cp2: boolean = canGenerateAiDraft('REVISION_REQUESTED');
const _cp3: boolean = canRetryAiDraft('FAILED');
const _cp4: boolean = canStartReview('DRAFT_READY');
const _cp5: boolean = canApproveProject('IN_REVIEW');
const _cp6: boolean = canRequestRevision('IN_REVIEW');
const _cp7: boolean = canRejectProject('IN_REVIEW');
const _cp8: boolean = canMarkReadyForPublish('APPROVED');
const _cp9: boolean = canCancelProject('CREATED');
const _cp10: boolean = canCancelProject('AI_REQUESTED');
const _cp11: boolean = canCancelProject('AI_PROCESSING');

// Draft actions
const _d1: boolean = canEditDraft('GENERATED');
const _d2: boolean = canEditDraft('REVISION_NEEDED');

// ── 6. Negative Checks (runtime assertions) ────────────────
// These verify that visibility helpers correctly return false
if (canMarkKnowledgeItemReady('READY')) throw new Error('READY should not be markable as ready');
if (canMarkKnowledgeItemReady('ARCHIVED')) throw new Error('ARCHIVED should not be markable as ready');
if (canGenerateAiDraft('IN_REVIEW')) throw new Error('IN_REVIEW should not allow generate');
if (canRetryAiDraft('CREATED')) throw new Error('CREATED should not allow retry');
if (canStartReview('CREATED')) throw new Error('CREATED should not allow start-review');
if (canApproveProject('DRAFT_READY')) throw new Error('DRAFT_READY should not allow approve');
if (canMarkReadyForPublish('IN_REVIEW')) throw new Error('IN_REVIEW should not allow publish');
if (canCancelProject('APPROVED')) throw new Error('APPROVED should not allow cancel');
if (canCancelProject('REJECTED')) throw new Error('REJECTED should not allow cancel');
if (canCancelProject('READY_FOR_PUBLISH')) throw new Error('READY_FOR_PUBLISH should not allow cancel');
if (canCancelProject('FAILED')) throw new Error('FAILED should not allow cancel');
if (canCancelProject('CANCELLED')) throw new Error('CANCELLED should not allow cancel');
if (canEditDraft('EDITED')) throw new Error('EDITED should not allow edit');
if (canEditDraft('APPROVED')) throw new Error('APPROVED draft should not allow edit');
if (canEditDraft('REJECTED')) throw new Error('REJECTED draft should not allow edit');

// ── 7. GENERATING must not exist ────────────────────────────
// TypeScript itself confirms GENERATING is not in the union (TS2367 would catch if added)
const _noGenerating: boolean = CONTENT_PROJECT_STATUSES.every(s => (s.value as string) !== 'GENERATING');
if (!_noGenerating) throw new Error('GENERATING found in CONTENT_PROJECT_STATUSES — must not exist');

// Suppress unused variable warnings
void _kiCheck; void _cpCheck; void _cpLengthCheck; void _kiLengthCheck;
void _ki1; void _ki2; void _ki3;
void _cp1; void _cp2; void _cp3; void _cp4; void _cp5; void _cp6;
void _cp7; void _cp8; void _cp9; void _cp10; void _cp11;
void _d1; void _d2;
