import { useState, useEffect, useCallback } from 'react'
import type {
  ReviewTargetType,
  SafePipelineStage,
  SafePipelineOverview,
  SafePipelineAggregateItem,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-016 Task 04: Pipeline Overview Panel.
 *
 * Internal-only pipeline visibility. Read-only aggregation.
 * No publish/export/CDN/public delivery.
 * Safe text rendering only — no unsafe HTML.
 */

const STAGE_LABELS: Record<SafePipelineStage, string> = {
  GENERATED: 'Üretildi',
  REVIEW_PENDING: 'İnceleme Bekliyor',
  IN_REVIEW: 'İnceleniyor',
  CHANGES_REQUESTED: 'Değişiklik İstendi',
  INTERNALLY_APPROVED: 'Dahili Onaylandı',
  INTERNAL_READY: 'Dahili Hazır',
  INTERNAL_REVISION_REQUESTED: 'Revizyon İstendi',
  REJECTED: 'Reddedildi',
  UNAVAILABLE: 'Kullanılamıyor',
}

const STAGE_CLASSES: Record<SafePipelineStage, string> = {
  GENERATED: 'pipeline-stage--generated',
  REVIEW_PENDING: 'pipeline-stage--review-pending',
  IN_REVIEW: 'pipeline-stage--in-review',
  CHANGES_REQUESTED: 'pipeline-stage--changes-requested',
  INTERNALLY_APPROVED: 'pipeline-stage--internally-approved',
  INTERNAL_READY: 'pipeline-stage--internal-ready',
  INTERNAL_REVISION_REQUESTED: 'pipeline-stage--revision-requested',
  REJECTED: 'pipeline-stage--rejected',
  UNAVAILABLE: 'pipeline-stage--unavailable',
}

const TARGET_TYPE_LABELS: Record<ReviewTargetType, string> = {
  CONTENT_PROJECT: 'İçerik Projesi',
  SCENARIO: 'Senaryo',
  STORYBOARD: 'Hikâye Tahtası',
  PACKAGE: 'Platform Paketi',
  IMAGE_ASSET: 'Görsel Varlık',
}

const STAGE_OPTIONS: SafePipelineStage[] = [
  'GENERATED', 'REVIEW_PENDING', 'IN_REVIEW',
  'CHANGES_REQUESTED', 'INTERNALLY_APPROVED', 'INTERNAL_READY',
  'INTERNAL_REVISION_REQUESTED', 'REJECTED', 'UNAVAILABLE',
]

const TARGET_TYPE_OPTIONS: ReviewTargetType[] = [
  'CONTENT_PROJECT', 'SCENARIO', 'STORYBOARD', 'PACKAGE', 'IMAGE_ASSET',
]

/** Safe text rendering — strip potential XSS */
function safeText(value: string | null | undefined): string {
  if (!value) return ''
  return String(value).replace(/[<>&"']/g, '')
}

interface PipelineOverviewPanelProps {
  onSelectItem?: (item: SafePipelineAggregateItem) => void
}

export default function PipelineOverviewPanel({
  onSelectItem,
}: PipelineOverviewPanelProps) {
  const [overview, setOverview] = useState<SafePipelineOverview | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Filters
  const [targetTypeFilter, setTargetTypeFilter] = useState<ReviewTargetType | ''>('')
  const [stageFilter, setStageFilter] = useState<SafePipelineStage | ''>('')

  const loadOverview = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const params: {
        targetType?: ReviewTargetType
        stage?: SafePipelineStage
        limit?: number
      } = { limit: 50 }
      if (targetTypeFilter) params.targetType = targetTypeFilter
      if (stageFilter) params.stage = stageFilter
      const res = await reviewApi.getPipelineOverview(params)
      if (res.success && res.data) {
        setOverview(res.data)
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Pipeline görünümü yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [targetTypeFilter, stageFilter])

  useEffect(() => {
    loadOverview()
  }, [loadOverview])

  return (
    <section data-testid="pipeline-overview-panel" aria-label="Dahili Pipeline Görünümü">
      <h3 data-testid="pipeline-overview-title">Dahili Pipeline Görünümü</h3>
      <p data-testid="pipeline-overview-disclaimer" className="disclaimer">
        Sadece İç Kullanım — Bu görünüm yayın/export değildir
      </p>

      {/* Filters */}
      <div data-testid="pipeline-filters" className="pipeline-filters">
        <label>
          Hedef Tipi:
          <select
            data-testid="pipeline-filter-targetType"
            value={targetTypeFilter}
            onChange={(e) => setTargetTypeFilter(e.target.value as ReviewTargetType | '')}
          >
            <option value="">Tümü</option>
            {TARGET_TYPE_OPTIONS.map((t) => (
              <option key={t} value={t}>{TARGET_TYPE_LABELS[t]}</option>
            ))}
          </select>
        </label>
        <label>
          Aşama:
          <select
            data-testid="pipeline-filter-stage"
            value={stageFilter}
            onChange={(e) => setStageFilter(e.target.value as SafePipelineStage | '')}
          >
            <option value="">Tümü</option>
            {STAGE_OPTIONS.map((s) => (
              <option key={s} value={s}>{STAGE_LABELS[s]}</option>
            ))}
          </select>
        </label>
      </div>

      {/* Loading */}
      {loading && <p data-testid="pipeline-loading">Yükleniyor…</p>}

      {/* Error */}
      {error && (
        <p data-testid="pipeline-error" className="error-message" role="alert">
          {safeText(error)}
        </p>
      )}

      {/* Content */}
      {!loading && !error && overview && (
        <>
          {/* Summary Bar */}
          <div data-testid="pipeline-summary-bar" className="pipeline-summary-bar">
            <span data-testid="pipeline-total-count">
              Toplam: {overview.totalCount}
            </span>
            {Object.entries(overview.countsByStage).map(([stage, count]) => (
              <span
                key={stage}
                data-testid={`pipeline-stage-count-${stage}`}
                className={STAGE_CLASSES[stage as SafePipelineStage] ?? ''}
              >
                {STAGE_LABELS[stage as SafePipelineStage] ?? stage}: {count}
              </span>
            ))}
          </div>

          {/* Warnings */}
          {overview.safeWarnings.length > 0 && (
            <div data-testid="pipeline-warnings">
              {overview.safeWarnings.map((w, i) => (
                <p key={i} className="pipeline-warning">{safeText(w)}</p>
              ))}
            </div>
          )}

          {/* Items */}
          {overview.items.length === 0 ? (
            <p data-testid="pipeline-empty">Pipeline öğesi bulunamadı.</p>
          ) : (
            <ul data-testid="pipeline-items-list" className="pipeline-items-list">
              {overview.items.map((item) => (
                <li
                  key={`${item.targetType}-${item.targetId}`}
                  data-testid="pipeline-item-card"
                  className="pipeline-item-card"
                  onClick={() => onSelectItem?.(item)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') onSelectItem?.(item)
                  }}
                >
                  <div className="pipeline-item-header">
                    <span data-testid="pipeline-item-targetType" className="pipeline-item-target-type">
                      [{TARGET_TYPE_LABELS[item.targetType] ?? safeText(item.targetType)}]
                    </span>
                    <span data-testid="pipeline-item-title" className="pipeline-item-title">
                      {safeText(item.safeTitle) || 'Başlıksız'}
                    </span>
                  </div>
                  <div className="pipeline-item-body">
                    <span
                      data-testid="pipeline-item-stage"
                      className={`pipeline-stage-badge ${STAGE_CLASSES[item.pipelineStage] ?? ''}`}
                    >
                      {STAGE_LABELS[item.pipelineStage] ?? safeText(item.pipelineStage)}
                    </span>
                    {item.previewAvailable && (
                      <span data-testid="pipeline-item-preview" className="pipeline-preview-badge">
                        Önizleme ✓
                      </span>
                    )}
                    {item.revisionRequested && (
                      <span data-testid="pipeline-item-revision" className="pipeline-revision-badge">
                        Revizyon İstendi
                      </span>
                    )}
                  </div>
                  {item.safeSummary && (
                    <p data-testid="pipeline-item-summary" className="pipeline-item-summary">
                      {safeText(item.safeSummary)}
                    </p>
                  )}
                  {item.safeBlockingReasons.length > 0 && (
                    <div data-testid="pipeline-item-blocking" className="pipeline-item-blocking">
                      <span className="blocking-label">Engelleyici nedenler:</span>
                      {item.safeBlockingReasons.map((r, i) => (
                        <span key={i} className="blocking-reason">{safeText(r)}</span>
                      ))}
                    </div>
                  )}
                  {item.safeNextInternalHints.length > 0 && (
                    <div data-testid="pipeline-item-hints" className="pipeline-item-hints">
                      <span className="hints-label">Dahili sonraki adımlar:</span>
                      {item.safeNextInternalHints.map((h, i) => (
                        <span key={i} className="hint-item">{safeText(h)}</span>
                      ))}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  )
}
