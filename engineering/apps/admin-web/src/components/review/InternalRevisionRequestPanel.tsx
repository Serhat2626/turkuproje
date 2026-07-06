import { useState, useCallback } from 'react'
import type {
  ReviewTargetType,
  ReviewActorRole,
  AdminInternalRevisionResponse,
  SafeInternalApprovalReadiness,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-015 Task 04: Internal Revision Request Panel.
 *
 * Allows REVIEWER/ADMIN to request internal revision for approved items.
 * APPROVED_FOR_INTERNAL_USE → CHANGES_REQUESTED.
 * Safe rendering only — no unsafe HTML, no publish/export behavior.
 * This is NOT a publish/export/regenerate action.
 */

const MAX_MESSAGE_LENGTH = 2000
const MAX_REASON_LENGTH = 100

/** Eligible states for internal revision request */
const ELIGIBLE_STATES = ['APPROVED_FOR_INTERNAL_USE', 'CHANGES_REQUESTED']

/** Eligible actor roles */
const ELIGIBLE_ACTORS: ReviewActorRole[] = ['REVIEWER', 'ADMIN']

interface InternalRevisionRequestPanelProps {
  targetType: ReviewTargetType
  targetId: string
  actorRole: ReviewActorRole
  actorReference: string
  readiness: SafeInternalApprovalReadiness | null
  onRevisionRequested?: () => void
}

export default function InternalRevisionRequestPanel({
  targetType,
  targetId,
  actorRole,
  actorReference,
  readiness,
  onRevisionRequested,
}: InternalRevisionRequestPanelProps) {
  const [safeMessage, setSafeMessage] = useState('')
  const [safeReasonCode, setSafeReasonCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<AdminInternalRevisionResponse | null>(null)

  const isActorEligible = ELIGIBLE_ACTORS.includes(actorRole)
  const isStateEligible = readiness?.reviewState
    ? ELIGIBLE_STATES.includes(readiness.reviewState)
    : false
  const canRequest = isActorEligible && isStateEligible && !loading

  const handleSubmit = useCallback(async () => {
    if (!canRequest) return
    if (!safeMessage.trim()) {
      setError('Revizyon mesajı gereklidir')
      return
    }
    try {
      setLoading(true)
      setError(null)
      setResult(null)
      const res = await reviewApi.requestInternalRevision(
        targetType,
        targetId,
        {
          actorRole,
          actorReference,
          safeReasonCode: safeReasonCode.trim().slice(0, MAX_REASON_LENGTH) || undefined,
          safeMessage: safeMessage.trim().slice(0, MAX_MESSAGE_LENGTH),
        }
      )
      if (res.success && res.data) {
        setResult(res.data)
        setSafeMessage('')
        setSafeReasonCode('')
        onRevisionRequested?.()
      } else {
        setError(safeText(res.message ?? 'Dahili revizyon isteği başarısız'))
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Dahili revizyon isteği başarısız'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [canRequest, safeMessage, safeReasonCode, targetType, targetId, actorRole, actorReference, onRevisionRequested])

  // Don't render for ineligible actors
  if (!isActorEligible) return null

  return (
    <div className="revision-panel" data-testid="revision-panel">
      <h4 className="revision-panel__title">Dahili Revizyon İsteği (Sadece İç Kullanım)</h4>

      {/* Disclaimer */}
      <div className="revision-panel__disclaimer" data-testid="revision-disclaimer">
        Bu işlem yayın/export/yeniden üretim değildir — sadece dahili değişiklik talebi
      </div>

      {/* Ineligible state */}
      {!isStateEligible && (
        <div className="revision-panel__ineligible" data-testid="revision-ineligible">
          Dahili revizyon isteği şu an bu durum için uygun değil.
          {readiness?.reviewState && (
            <span> Mevcut durum: {safeText(readiness.reviewState)}</span>
          )}
        </div>
      )}

      {/* Form — only when eligible */}
      {isStateEligible && (
        <div className="revision-panel__form" data-testid="revision-form">
          <div className="revision-panel__field">
            <label htmlFor="revision-reason">Neden Kodu (opsiyonel):</label>
            <input
              id="revision-reason"
              data-testid="revision-reason-input"
              type="text"
              value={safeReasonCode}
              onChange={e => setSafeReasonCode(e.target.value)}
              maxLength={MAX_REASON_LENGTH}
              placeholder="Ör: ICERIK_GUNCELLEME"
              disabled={loading}
            />
          </div>
          <div className="revision-panel__field">
            <label htmlFor="revision-message">Revizyon Mesajı (zorunlu):</label>
            <textarea
              id="revision-message"
              data-testid="revision-message-input"
              value={safeMessage}
              onChange={e => setSafeMessage(e.target.value)}
              maxLength={MAX_MESSAGE_LENGTH}
              placeholder="Değişiklik talebinizi açıklayın..."
              disabled={loading}
              rows={3}
            />
          </div>
          <button
            data-testid="revision-submit-button"
            onClick={handleSubmit}
            disabled={!canRequest || !safeMessage.trim()}
            className="revision-panel__submit"
          >
            {loading ? 'Gönderiliyor...' : 'Dahili Revizyon İste'}
          </button>
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="revision-panel__error" data-testid="revision-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Success result */}
      {result && (
        <div className="revision-panel__success" data-testid="revision-success">
          <div>Dahili revizyon isteği başarılı</div>
          <div data-testid="revision-result-state">
            {safeText(result.previousState)} → {safeText(result.currentState)}
          </div>
          {result.actionApplied && (
            <div data-testid="revision-result-action">
              İşlem: {safeText(result.actionApplied)}
            </div>
          )}
          {result.appendedNote && (
            <div data-testid="revision-result-note">Not eklendi</div>
          )}
          {result.auditRecorded && (
            <div data-testid="revision-result-audit">Denetim kaydedildi</div>
          )}
        </div>
      )}
    </div>
  )
}

function safeText(text: string): string {
  if (!text) return ''
  return text
    .replace(/[<>]/g, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .slice(0, 2000)
}
