import { useState, useEffect, useCallback } from 'react'
import type {
  ReviewTargetType,
  ReviewState,
  AdminReviewWorklistItem,
  AdminReviewTargetContext,
  SafeInternalApprovalReadiness,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'
import ReviewConsole from './ReviewConsole'
import ReviewPreviewPanel from './ReviewPreviewPanel'
import InternalApprovalReadinessPanel from './InternalApprovalReadinessPanel'
import ApprovedInternalInventoryPanel from './ApprovedInternalInventoryPanel'
import InternalRevisionRequestPanel from './InternalRevisionRequestPanel'
import PipelineOverviewPanel from './PipelineOverviewPanel'

/**
 * FP-012 Task 10 + FP-014 Task 04 + FP-015 Task 04 + FP-016 Task 04:
 * Admin Review Worklist with console, preview, internal readiness,
 * inventory, revision request, and pipeline overview.
 *
 * Safe rendering. No publish/export. No unsafe HTML.
 */

/** Safe state labels */
const STATE_LABELS: Record<ReviewState, string> = {
  DRAFT: 'Taslak',
  NEEDS_REVIEW: 'İnceleme Bekliyor',
  IN_REVIEW: 'İnceleniyor',
  CHANGES_REQUESTED: 'Değişiklik İstendi',
  APPROVED_FOR_INTERNAL_USE: 'Dahili Onay',
  REJECTED: 'Reddedildi',
}

const STATE_OPTIONS: ReviewState[] = [
  'DRAFT', 'NEEDS_REVIEW', 'IN_REVIEW',
  'CHANGES_REQUESTED', 'APPROVED_FOR_INTERNAL_USE', 'REJECTED',
]

const TARGET_TYPE_OPTIONS: ReviewTargetType[] = [
  'CONTENT_PROJECT', 'SCENARIO', 'STORYBOARD', 'PACKAGE', 'IMAGE_ASSET',
]

interface ReviewWorklistProps {
  actorRole: 'CREATOR' | 'REVIEWER' | 'ADMIN' | 'SYSTEM'
  actorReference: string
}

export default function ReviewWorklist({
  actorRole,
  actorReference,
}: ReviewWorklistProps) {
  const [items, setItems] = useState<AdminReviewWorklistItem[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [stateFilter, setStateFilter] = useState<ReviewState | ''>('')
  const [targetTypeFilter, setTargetTypeFilter] = useState<ReviewTargetType | ''>('')

  // Selected item
  const [selectedItem, setSelectedItem] = useState<AdminReviewWorklistItem | null>(null)
  const [targetContext, setTargetContext] = useState<AdminReviewTargetContext | null>(null)
  const [selectedReadiness, setSelectedReadiness] = useState<SafeInternalApprovalReadiness | null>(null)

  const loadWorklist = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const params: { state?: ReviewState; targetType?: ReviewTargetType } = {}
      if (stateFilter) params.state = stateFilter
      if (targetTypeFilter) params.targetType = targetTypeFilter
      const res = await reviewApi.getWorklist(params)
      if (res.success && res.data) {
        setItems(res.data)
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'İş listesi yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [stateFilter, targetTypeFilter])

  useEffect(() => {
    loadWorklist()
  }, [loadWorklist])

  const handleSelectItem = async (item: AdminReviewWorklistItem) => {
    setSelectedItem(item)
    try {
      const res = await reviewApi.getTargetContext(item.targetType, item.targetId)
      if (res.success && res.data) {
        setTargetContext(res.data)
      }
    } catch {
      setTargetContext(null)
    }
    try {
      const readinessRes = await reviewApi.getInternalReadiness(item.targetType, item.targetId)
      if (readinessRes.success && readinessRes.data) {
        setSelectedReadiness(readinessRes.data)
      } else {
        setSelectedReadiness(null)
      }
    } catch {
      setSelectedReadiness(null)
    }
  }

  const handleConsoleActionComplete = () => {
    loadWorklist()
  }

  const handleRevisionRequested = () => {
    loadWorklist()
    // Reload readiness after revision request
    if (selectedItem) {
      reviewApi.getInternalReadiness(selectedItem.targetType, selectedItem.targetId)
        .then(res => {
          if (res.success && res.data) setSelectedReadiness(res.data)
        })
        .catch(() => setSelectedReadiness(null))
    }
  }

  return (
    <div className="review-worklist" data-testid="review-worklist">
      <h2 className="review-worklist__title">İnceleme İş Listesi</h2>

      {/* FP-016 Task 04: Dahili Pipeline Görünümü */}
      <PipelineOverviewPanel />

      {/* FP-015 Task 04: Approved Internal Inventory */}
      <ApprovedInternalInventoryPanel
        onSelectItem={(tt, tid) => {
          const match = items.find(i => i.targetType === tt && i.targetId === tid)
          if (match) handleSelectItem(match)
        }}
      />

      {/* Filters */}
      <div className="review-worklist__filters" data-testid="review-worklist-filters">
        <select
          data-testid="review-filter-state"
          value={stateFilter}
          onChange={e => setStateFilter(e.target.value as ReviewState | '')}
        >
          <option value="">Tüm Durumlar</option>
          {STATE_OPTIONS.map(s => (
            <option key={s} value={s}>{STATE_LABELS[s]}</option>
          ))}
        </select>

        <select
          data-testid="review-filter-target-type"
          value={targetTypeFilter}
          onChange={e => setTargetTypeFilter(e.target.value as ReviewTargetType | '')}
        >
          <option value="">Tüm Hedef Tipleri</option>
          {TARGET_TYPE_OPTIONS.map(t => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
      </div>

      {/* Error */}
      {error && (
        <div className="review-worklist__error" data-testid="review-worklist-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Loading */}
      {loading && <div className="review-worklist__loading">Yükleniyor...</div>}

      {/* Worklist */}
      <ul className="review-worklist__list" data-testid="review-worklist-list">
        {items.map(item => (
          <li
            key={`${item.targetType}-${item.targetId}`}
            className={`review-worklist__item ${
              selectedItem?.targetId === item.targetId ? 'review-worklist__item--selected' : ''
            }`}
            data-testid="review-worklist-item"
            onClick={() => handleSelectItem(item)}
          >
            <div className="review-worklist__item-header">
              <span className="review-worklist__item-title">
                {safeText(item.displayTitle ?? item.targetType)}
              </span>
              <span className={`review-badge review-badge--${item.currentState.toLowerCase().replace(/_/g, '-')}`}>
                {STATE_LABELS[item.currentState]}
              </span>
            </div>
            {item.lastActionAt && (
              <div className="review-worklist__item-meta">
                {new Date(item.lastActionAt).toLocaleString('tr-TR')}
              </div>
            )}
          </li>
        ))}
        {items.length === 0 && !loading && (
          <li className="review-worklist__empty">Sonuç bulunamadı</li>
        )}
      </ul>

      {/* Target Context + Console */}
      {selectedItem && (
        <div className="review-worklist__detail" data-testid="review-worklist-detail">
          {/* Target Context */}
          {targetContext && (
            <div className="review-worklist__context" data-testid="review-target-context">
              <h3>{safeText(targetContext.displayTitle ?? '')}</h3>
              <p>{safeText(targetContext.displaySubtitle ?? '')}</p>
              {!targetContext.safePreviewAvailable && targetContext.safePreviewLabel && (
                <p className="review-worklist__preview-label">
                  {safeText(targetContext.safePreviewLabel)}
                </p>
              )}
            </div>
          )}

          {/* FP-013 Task 04: Safe Preview Panel */}
          <ReviewPreviewPanel
            targetType={selectedItem.targetType}
            targetId={selectedItem.targetId}
          />

          {/* FP-014 Task 04: Internal Approval Readiness Panel */}
          <InternalApprovalReadinessPanel
            targetType={selectedItem.targetType}
            targetId={selectedItem.targetId}
          />

          {/* FP-015 Task 04: Internal Revision Request Panel */}
          <InternalRevisionRequestPanel
            targetType={selectedItem.targetType}
            targetId={selectedItem.targetId}
            actorRole={actorRole}
            actorReference={actorReference}
            readiness={selectedReadiness}
            onRevisionRequested={handleRevisionRequested}
          />

          {/* Integrated Console */}
          <ReviewConsole
            targetType={selectedItem.targetType}
            targetId={selectedItem.targetId}
            actorRole={actorRole}
            actorReference={actorReference}
            onActionComplete={handleConsoleActionComplete}
          />
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
