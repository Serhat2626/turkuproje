import { useState, useEffect, useCallback } from 'react'
import type {
  ReviewTargetType,
  SafeInternalApprovalReadiness,
  InternalApprovalReadinessAvailability,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-015 Task 04: Approved Internal Inventory Panel.
 *
 * Lists all internally-approved items (APPROVED_FOR_INTERNAL_USE).
 * Read-only. No publish/export/CDN/public delivery.
 * Safe text rendering only — no unsafe HTML.
 * "READY" means internal readiness, not publish/export.
 */

const AVAILABILITY_LABELS: Record<InternalApprovalReadinessAvailability, string> = {
  READY: 'Dahili Hazır',
  NOT_APPROVED: 'Onay Bekliyor',
  REJECTED: 'Reddedildi',
  MISSING_TARGET: 'Hedef Bulunamadı',
  UNSAFE_TARGET: 'Güvenli Değil',
  PREVIEW_UNAVAILABLE: 'Önizleme Kullanılamıyor',
  UNAVAILABLE: 'Kullanılamıyor',
}

const AVAILABILITY_CLASSES: Record<InternalApprovalReadinessAvailability, string> = {
  READY: 'readiness-badge--ready',
  NOT_APPROVED: 'readiness-badge--not-approved',
  REJECTED: 'readiness-badge--rejected',
  MISSING_TARGET: 'readiness-badge--missing',
  UNSAFE_TARGET: 'readiness-badge--unsafe',
  PREVIEW_UNAVAILABLE: 'readiness-badge--preview-unavailable',
  UNAVAILABLE: 'readiness-badge--unavailable',
}

const TARGET_TYPE_OPTIONS: ReviewTargetType[] = [
  'CONTENT_PROJECT', 'SCENARIO', 'STORYBOARD', 'PACKAGE', 'IMAGE_ASSET',
]

interface ApprovedInternalInventoryPanelProps {
  onSelectItem?: (targetType: ReviewTargetType, targetId: string) => void
}

export default function ApprovedInternalInventoryPanel({
  onSelectItem,
}: ApprovedInternalInventoryPanelProps) {
  const [items, setItems] = useState<SafeInternalApprovalReadiness[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [targetTypeFilter, setTargetTypeFilter] = useState<ReviewTargetType | ''>('')

  const loadInventory = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await reviewApi.getApprovedInternalReadiness(
        targetTypeFilter || undefined
      )
      if (res.success && res.data) {
        setItems(res.data)
      } else {
        setError(safeText(res.message ?? 'Dahili envanter yüklenemedi'))
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Dahili envanter yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [targetTypeFilter])

  useEffect(() => {
    loadInventory()
  }, [loadInventory])

  return (
    <div className="inventory-panel" data-testid="inventory-panel">
      <h3 className="inventory-panel__title">Dahili Onaylı Envanter (Sadece İç Kullanım)</h3>

      {/* Disclaimer */}
      <div className="inventory-panel__disclaimer" data-testid="inventory-disclaimer">
        Bu liste yayın/export hazır anlamına gelmez — sadece dahili olarak onaylanmış öğeler
      </div>

      {/* Filter */}
      <div className="inventory-panel__filters" data-testid="inventory-filters">
        <select
          data-testid="inventory-filter-target-type"
          value={targetTypeFilter}
          onChange={e => setTargetTypeFilter(e.target.value as ReviewTargetType | '')}
        >
          <option value="">Tüm Hedef Tipleri</option>
          {TARGET_TYPE_OPTIONS.map(t => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </div>

      {/* Loading */}
      {loading && (
        <div className="inventory-panel__loading" data-testid="inventory-loading">
          Yükleniyor...
        </div>
      )}

      {/* Error */}
      {error && !loading && (
        <div className="inventory-panel__error" data-testid="inventory-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Items */}
      {!loading && !error && (
        <ul className="inventory-panel__list" data-testid="inventory-list">
          {items.map(item => (
            <li
              key={`${item.targetType}-${item.targetId}`}
              className="inventory-panel__item"
              data-testid="inventory-item"
              onClick={() => onSelectItem?.(item.targetType, item.targetId)}
            >
              <div className="inventory-panel__item-header">
                <span className="inventory-panel__item-title">
                  {safeText(item.safeTitle ?? item.targetType)}
                </span>
                <span className={`readiness-badge ${AVAILABILITY_CLASSES[item.availability]}`}>
                  {AVAILABILITY_LABELS[item.availability]}
                </span>
              </div>
              {item.safeSummary && (
                <div className="inventory-panel__item-summary">
                  {safeText(item.safeSummary)}
                </div>
              )}
              {item.approvedAt && (
                <div className="inventory-panel__item-meta">
                  Onay: {new Date(item.approvedAt).toLocaleString('tr-TR')}
                  {item.approvedByRole && ` — ${safeText(item.approvedByRole)}`}
                </div>
              )}
            </li>
          ))}
          {items.length === 0 && (
            <li className="inventory-panel__empty" data-testid="inventory-empty">
              Dahili onaylı öğe bulunamadı
            </li>
          )}
        </ul>
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
