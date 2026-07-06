import { useState, useEffect, useCallback } from 'react'
import type {
  ReviewTargetType,
  SafeInternalApprovalReadiness,
  InternalApprovalReadinessAvailability,
  SafeInternalAction,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-014 Task 04: Internal Approval Readiness Panel.
 *
 * Displays safe internal approval readiness information only.
 * No bytes, no URL, no encoded binary, no storage key, no provider detail,
 * no raw payload, no publish/export behavior.
 *
 * Safe text rendering only — no unsafe HTML injection methods.
 * APPROVED_FOR_INTERNAL_USE shown as internal-only readiness.
 * PREPARE_FOR_FUTURE_EXPORT is informational/non-executing only.
 * REQUEST_INTERNAL_REVISION now active — linked to revision request panel.
 */

/** Safe availability labels — Turkish */
const AVAILABILITY_LABELS: Record<InternalApprovalReadinessAvailability, string> = {
  READY: 'Dahili Hazır',
  NOT_APPROVED: 'Onay Bekliyor',
  REJECTED: 'Reddedildi',
  MISSING_TARGET: 'Hedef Bulunamadı',
  UNSAFE_TARGET: 'Güvenli Değil',
  PREVIEW_UNAVAILABLE: 'Önizleme Kullanılamıyor',
  UNAVAILABLE: 'Kullanılamıyor',
}

/** Safe availability CSS modifier */
const AVAILABILITY_CLASSES: Record<InternalApprovalReadinessAvailability, string> = {
  READY: 'readiness-badge--ready',
  NOT_APPROVED: 'readiness-badge--not-approved',
  REJECTED: 'readiness-badge--rejected',
  MISSING_TARGET: 'readiness-badge--missing',
  UNSAFE_TARGET: 'readiness-badge--unsafe',
  PREVIEW_UNAVAILABLE: 'readiness-badge--preview-unavailable',
  UNAVAILABLE: 'readiness-badge--unavailable',
}

/** Safe internal action labels — Turkish */
const ACTION_LABELS: Record<SafeInternalAction, string> = {
  VIEW_SAFE_PREVIEW: 'Güvenli Önizleme',
  REVIEW_APPROVAL_DETAILS: 'Onay Detayları',
  REQUEST_INTERNAL_REVISION: 'Dahili Revizyon İsteği',
  PREPARE_FOR_FUTURE_EXPORT: 'Gelecekte Export Hazırlığı — bilgilendirme',
  MARK_AS_INTERNAL_READY_CANDIDATE: 'Dahili Hazır Adayı Olarak İşaretle',
}

/** Actions that are informational-only — disabled, no POST behavior */
const DEFERRED_ACTIONS: SafeInternalAction[] = [
  'PREPARE_FOR_FUTURE_EXPORT',
  'MARK_AS_INTERNAL_READY_CANDIDATE',
]

interface InternalApprovalReadinessPanelProps {
  targetType: ReviewTargetType
  targetId: string
}

export default function InternalApprovalReadinessPanel({
  targetType,
  targetId,
}: InternalApprovalReadinessPanelProps) {
  const [readiness, setReadiness] = useState<SafeInternalApprovalReadiness | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const loadReadiness = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await reviewApi.getInternalReadiness(targetType, targetId)
      if (res.success && res.data) {
        setReadiness(res.data)
      } else {
        setError(safeText(res.message ?? 'Dahili hazırlık durumu yüklenemedi'))
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Dahili hazırlık durumu yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [targetType, targetId])

  useEffect(() => {
    loadReadiness()
  }, [loadReadiness])

  return (
    <div className="readiness-panel" data-testid="readiness-panel">
      <h4 className="readiness-panel__title">Dahili Onay Durumu</h4>

      {/* Loading */}
      {loading && (
        <div className="readiness-panel__loading" data-testid="readiness-loading">
          Yükleniyor...
        </div>
      )}

      {/* Error — safe text only */}
      {error && !loading && (
        <div className="readiness-panel__error" data-testid="readiness-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Readiness Content */}
      {readiness && !loading && (
        <div className="readiness-panel__content" data-testid="readiness-content">

          {/* Availability Badge */}
          <div className="readiness-panel__availability" data-testid="readiness-availability">
            <span className={`readiness-badge ${AVAILABILITY_CLASSES[readiness.availability]}`}>
              {AVAILABILITY_LABELS[readiness.availability]}
            </span>
          </div>

          {/* Internal-only readiness disclaimer */}
          {readiness.availability === 'READY' && (
            <div className="readiness-panel__disclaimer" data-testid="readiness-disclaimer">
              Bu durum yayın/export anlamına gelmez — sadece dahili onay
            </div>
          )}

          {/* NOT_APPROVED message */}
          {readiness.availability === 'NOT_APPROVED' && (
            <div className="readiness-panel__not-approved" data-testid="readiness-not-approved">
              Manuel onay hâlâ gereklidir
            </div>
          )}

          {/* REJECTED message */}
          {readiness.availability === 'REJECTED' && (
            <div className="readiness-panel__rejected" data-testid="readiness-rejected">
              Bu öğe dahili olarak hazır değildir
            </div>
          )}

          {/* MISSING_TARGET / UNAVAILABLE / UNSAFE_TARGET / PREVIEW_UNAVAILABLE */}
          {(readiness.availability === 'MISSING_TARGET' ||
            readiness.availability === 'UNAVAILABLE' ||
            readiness.availability === 'UNSAFE_TARGET' ||
            readiness.availability === 'PREVIEW_UNAVAILABLE') && (
            <div className="readiness-panel__unavailable" data-testid="readiness-unavailable">
              {safeText(readiness.safeMessage ?? AVAILABILITY_LABELS[readiness.availability])}
            </div>
          )}

          {/* Safe Title */}
          {readiness.safeTitle && (
            <div className="readiness-panel__field" data-testid="readiness-safe-title">
              <strong>Başlık:</strong> {safeText(readiness.safeTitle)}
            </div>
          )}

          {/* Safe Summary */}
          {readiness.safeSummary && (
            <div className="readiness-panel__field" data-testid="readiness-safe-summary">
              <strong>Özet:</strong> {safeText(readiness.safeSummary)}
            </div>
          )}

          {/* Safe Message */}
          {readiness.safeMessage && readiness.availability === 'READY' && (
            <div className="readiness-panel__field" data-testid="readiness-safe-message">
              <strong>Mesaj:</strong> {safeText(readiness.safeMessage)}
            </div>
          )}

          {/* Approval Detail */}
          {readiness.approvedAt && (
            <div className="readiness-panel__field" data-testid="readiness-approved-at">
              <strong>Onay Tarihi:</strong>{' '}
              {new Date(readiness.approvedAt).toLocaleString('tr-TR')}
            </div>
          )}
          {readiness.approvedByRole && (
            <div className="readiness-panel__field" data-testid="readiness-approved-by">
              <strong>Onaylayan Rol:</strong> {safeText(readiness.approvedByRole)}
            </div>
          )}

          {/* Safe Next Internal Actions */}
          {readiness.safeNextInternalActions && readiness.safeNextInternalActions.length > 0 && (
            <div className="readiness-panel__actions" data-testid="readiness-actions">
              <strong>Dahili işlem önerileri:</strong>
              <ul className="readiness-panel__action-list">
                {readiness.safeNextInternalActions.map(action => (
                  <li
                    key={action}
                    className={`readiness-panel__action-item ${
                      DEFERRED_ACTIONS.includes(action) ? 'readiness-panel__action-item--deferred' : ''
                    }`}
                    data-testid={`readiness-action-${action}`}
                  >
                    {ACTION_LABELS[action] ?? action}
                    {DEFERRED_ACTIONS.includes(action) && (
                      <span className="readiness-panel__deferred-badge"> (bilgilendirme)</span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Safe Warnings */}
          {readiness.safeWarnings && readiness.safeWarnings.length > 0 && (
            <div className="readiness-panel__warnings" data-testid="readiness-warnings">
              {readiness.safeWarnings.map((w, i) => (
                <div key={i} className="readiness-panel__warning">
                  ⚠ {safeText(w)}
                </div>
              ))}
            </div>
          )}

          {/* Updated At */}
          {readiness.updatedAt && (
            <div className="readiness-panel__field readiness-panel__updated-at" data-testid="readiness-updated-at">
              <strong>Son Güncelleme:</strong>{' '}
              {new Date(readiness.updatedAt).toLocaleString('tr-TR')}
            </div>
          )}
        </div>
      )}
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
