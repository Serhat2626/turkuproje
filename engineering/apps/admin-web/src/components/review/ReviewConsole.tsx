import { useState, useEffect, useCallback } from 'react'
import type {
  ReviewTargetType,
  ReviewState,
  ReviewActionType,
  ReviewActorRole,
  AdminReviewStateSummary,
  AdminReviewAuditTimelineItem,
  AdminReviewActionRequest,
  AdminReviewAvailableAction,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-012 Task 11: Admin Review Console — availability-driven, UX-hardened.
 *
 * Safe rendering — no unsafe HTML injection, no publish/export,
 * no provider URL/storage key/raw payload/encoded binary/secret display.
 * APPROVED_FOR_INTERNAL_USE shown as internal-only approval.
 * Actions enabled/disabled based on backend availability response.
 */

/** Safe state labels — no publish/export wording */
const STATE_LABELS: Record<ReviewState, string> = {
  DRAFT: 'Taslak',
  NEEDS_REVIEW: 'İnceleme Bekliyor',
  IN_REVIEW: 'İnceleniyor',
  CHANGES_REQUESTED: 'Değişiklik İstendi',
  APPROVED_FOR_INTERNAL_USE: 'Dahili Onay (Sadece İç Kullanım)',
  REJECTED: 'Reddedildi',
}

/** Safe state CSS classes */
const STATE_CLASSES: Record<ReviewState, string> = {
  DRAFT: 'review-badge--draft',
  NEEDS_REVIEW: 'review-badge--needs-review',
  IN_REVIEW: 'review-badge--in-review',
  CHANGES_REQUESTED: 'review-badge--changes-requested',
  APPROVED_FOR_INTERNAL_USE: 'review-badge--approved-internal',
  REJECTED: 'review-badge--rejected',
}

/** Safe action button labels — no publish/export wording */
const ACTION_LABELS: Record<ReviewActionType, string> = {
  SUBMIT_FOR_REVIEW: 'İncelemeye Gönder',
  START_REVIEW: 'İncelemeyi Başlat',
  REQUEST_CHANGES: 'Değişiklik İste',
  APPROVE_FOR_INTERNAL_USE: 'Dahili Onayla (İç Kullanım)',
  REJECT: 'Reddet',
  RETURN_TO_DRAFT: 'Taslağa Döndür',
  ADD_REVIEW_NOTE: 'Not Ekle',
}

/** Event type labels */
const EVENT_LABELS: Record<string, string> = {
  REVIEW_SUBMITTED: 'İncelemeye Gönderildi',
  REVIEW_STARTED: 'İnceleme Başladı',
  CHANGES_REQUESTED: 'Değişiklik İstendi',
  INTERNAL_APPROVAL_GRANTED: 'Dahili Onay Verildi',
  REVIEW_REJECTED: 'Reddedildi',
  RETURNED_TO_DRAFT: 'Taslağa Döndürüldü',
  REVIEW_NOTE_ADDED: 'Not Eklendi',
}

/** Max lengths for safe input */
const MAX_NOTE_LENGTH = 2000
const MAX_REASON_LENGTH = 100

interface ReviewConsoleProps {
  targetType: ReviewTargetType
  targetId: string
  actorRole: ReviewActorRole
  actorReference: string
  onActionComplete?: () => void
}

export default function ReviewConsole({
  targetType,
  targetId,
  actorRole,
  actorReference,
  onActionComplete,
}: ReviewConsoleProps) {
  const [stateSummary, setStateSummary] = useState<AdminReviewStateSummary | null>(null)
  const [timeline, setTimeline] = useState<AdminReviewAuditTimelineItem[]>([])
  const [availableActions, setAvailableActions] = useState<AdminReviewAvailableAction[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [note, setNote] = useState('')
  const [reasonCode, setReasonCode] = useState('')
  const [actionLoading, setActionLoading] = useState(false)
  const [noteError, setNoteError] = useState<string | null>(null)

  const loadAll = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const [stateRes, auditRes, actionsRes] = await Promise.all([
        reviewApi.getReviewState(targetType, targetId),
        reviewApi.getAuditTimeline(targetType, targetId),
        reviewApi.getAvailableActions(targetType, targetId, actorRole),
      ])
      if (stateRes.success && stateRes.data) {
        setStateSummary(stateRes.data)
      }
      if (auditRes.success && auditRes.data) {
        setTimeline(auditRes.data)
      }
      if (actionsRes.success && actionsRes.data) {
        setAvailableActions(actionsRes.data.availableActions ?? [])
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Durum yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [targetType, targetId, actorRole])

  useEffect(() => {
    loadAll()
  }, [loadAll])

  const handleAction = async (actionType: ReviewActionType) => {
    // Validate note for actions that need reason
    if ((actionType === 'REQUEST_CHANGES' || actionType === 'REJECT') && !note.trim()) {
      setNoteError('Bu işlem için not/açıklama gereklidir')
      return
    }
    setNoteError(null)

    try {
      setActionLoading(true)
      setError(null)
      const request: AdminReviewActionRequest = {
        targetType,
        targetId,
        actionType,
        actorRole,
        actorReference,
        ...(note.trim() ? { note: note.trim().slice(0, MAX_NOTE_LENGTH) } : {}),
        ...(reasonCode.trim() ? { reasonCode: reasonCode.trim().slice(0, MAX_REASON_LENGTH) } : {}),
      }

      let result
      switch (actionType) {
        case 'SUBMIT_FOR_REVIEW':
          result = await reviewApi.submitForReview(request); break
        case 'START_REVIEW':
          result = await reviewApi.startReview(request); break
        case 'REQUEST_CHANGES':
          result = await reviewApi.requestChanges(request); break
        case 'APPROVE_FOR_INTERNAL_USE':
          result = await reviewApi.approveForInternalUse(request); break
        case 'REJECT':
          result = await reviewApi.rejectReview(request); break
        case 'RETURN_TO_DRAFT':
          result = await reviewApi.returnToDraft(request); break
        case 'ADD_REVIEW_NOTE':
          result = await reviewApi.addReviewNote(request); break
      }

      if (result?.success) {
        setNote('')
        setReasonCode('')
        setNoteError(null)
        await loadAll()
        onActionComplete?.()
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'İşlem başarısız'
      setError(safeText(msg))
    } finally {
      setActionLoading(false)
    }
  }

  const currentState = stateSummary?.currentState ?? 'DRAFT'

  // Filter actions: hide APPROVE_FOR_INTERNAL_USE for CREATOR/SYSTEM
  const visibleActions = availableActions.filter(action => {
    if (action.actionType === 'APPROVE_FOR_INTERNAL_USE' &&
        (actorRole === 'CREATOR' || actorRole === 'SYSTEM')) {
      return false
    }
    return true
  })

  return (
    <div className="review-console" data-testid="review-console">
      <h3 className="review-console__title">İnceleme Konsolu</h3>

      {/* Error Display — safe text only */}
      {error && (
        <div className="review-console__error" data-testid="review-error" role="alert">
          {safeText(error)}
          <button
            className="review-console__retry-btn"
            data-testid="review-retry-btn"
            onClick={loadAll}
            type="button"
          >
            Tekrar Dene
          </button>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="review-console__loading" data-testid="review-loading">
          Yükleniyor...
        </div>
      )}

      {/* State Summary */}
      {stateSummary && (
        <div className="review-console__state" data-testid="review-state">
          <div className="review-state__header">
            <span className="review-state__label">Durum:</span>
            <span
              className={`review-badge ${STATE_CLASSES[currentState]}`}
              data-testid="review-state-badge"
            >
              {STATE_LABELS[currentState]}
            </span>
          </div>
          {stateSummary.lastActionType && (
            <div className="review-state__detail">
              <span>Son İşlem: {ACTION_LABELS[stateSummary.lastActionType] ?? stateSummary.lastActionType}</span>
              {stateSummary.lastActionAt && (
                <span className="review-state__time">
                  {new Date(stateSummary.lastActionAt).toLocaleString('tr-TR')}
                </span>
              )}
            </div>
          )}
        </div>
      )}

      {/* Action Panel — availability-driven */}
      <div className="review-console__actions" data-testid="review-actions">
        <h4>İşlemler</h4>

        {/* Note/Reason Input */}
        <div className="review-console__inputs">
          <textarea
            className="review-console__note-input"
            data-testid="review-note-input"
            placeholder="Not / Açıklama"
            value={note}
            onChange={e => setNote(e.target.value)}
            maxLength={MAX_NOTE_LENGTH}
            rows={3}
          />
          <span className="review-console__char-count" data-testid="review-note-count">
            {note.length}/{MAX_NOTE_LENGTH}
          </span>
          {noteError && (
            <span className="review-console__note-error" data-testid="review-note-error" role="alert">
              {safeText(noteError)}
            </span>
          )}
          <input
            className="review-console__reason-input"
            data-testid="review-reason-input"
            placeholder="Sebep kodu (opsiyonel)"
            value={reasonCode}
            onChange={e => setReasonCode(e.target.value)}
            maxLength={MAX_REASON_LENGTH}
          />
        </div>

        {/* Action Buttons — enabled/disabled from backend availability */}
        <div className="review-console__buttons" data-testid="review-action-buttons">
          {visibleActions.map(action => (
            <button
              key={action.actionType}
              className={`review-console__action-btn review-console__action-btn--${action.actionType.toLowerCase().replace(/_/g, '-')}`}
              data-testid={`review-action-${action.actionType}`}
              onClick={() => handleAction(action.actionType)}
              disabled={!action.enabled || actionLoading}
              title={action.enabled
                ? (safeText(action.safeLabel ?? '') || ACTION_LABELS[action.actionType])
                : safeText(action.safeDescription ?? action.disabledReasonCode ?? 'Kullanılamaz')}
            >
              {ACTION_LABELS[action.actionType]}
            </button>
          ))}
          {visibleActions.length === 0 && !loading && (
            <p className="review-console__no-actions" data-testid="review-no-actions">
              Kullanılabilir işlem yok
            </p>
          )}
        </div>
      </div>

      {/* Audit Timeline — safe fields only */}
      <div className="review-console__timeline" data-testid="review-timeline">
        <h4>İnceleme Geçmişi</h4>
        {timeline.length === 0 && !loading && (
          <p className="review-console__empty" data-testid="review-timeline-empty">Henüz geçmiş yok</p>
        )}
        <ul className="review-timeline__list">
          {timeline.map((item, idx) => (
            <li key={idx} className="review-timeline__item" data-testid="review-timeline-item">
              <div className="review-timeline__header">
                <span className="review-timeline__event">
                  {EVENT_LABELS[item.eventType] ?? item.eventType}
                </span>
                <span className="review-timeline__time">
                  {new Date(item.occurredAt).toLocaleString('tr-TR')}
                </span>
              </div>
              <div className="review-timeline__detail">
                <span>{STATE_LABELS[item.fromState]} → {STATE_LABELS[item.toState]}</span>
                <span className="review-timeline__actor">
                  {item.actorRole}
                </span>
              </div>
              {item.safeReasonCode && (
                <div className="review-timeline__reason">
                  Sebep: {safeText(item.safeReasonCode)}
                </div>
              )}
              {item.sanitizedNote && (
                <div className="review-timeline__note">
                  Not: {safeText(item.sanitizedNote)}
                </div>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}

/**
 * Safe text rendering — strips dangerous content.
 * Never use unsafe HTML injection.
 */
function safeText(text: string): string {
  if (!text) return ''
  return text
    .replace(/[<>]/g, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .slice(0, 2000)
}
