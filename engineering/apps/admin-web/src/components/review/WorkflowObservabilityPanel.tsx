import { useState, useEffect, useCallback } from 'react'
import type {
  SafeWorkflowObservabilitySummary,
  SafeWorkflowDiagnosticType,
  SafeOperationalControlHint,
  ReviewTargetType,
} from '../../types'
import * as reviewApi from '../../api/reviewApi'

/**
 * FP-017 Task 04: Internal-only workflow observability panel.
 *
 * Read-only. No mutation. No publish/export. No unsafe HTML.
 * Turkish safe labels. No provider/storage/URL/credential exposure.
 */

// ── Safe labels ────────────────────────────────────────────────

const DIAGNOSTIC_TYPE_LABELS: Record<SafeWorkflowDiagnosticType, string> = {
  STALE_REVIEW: 'Eski İnceleme',
  PREVIEW_UNAVAILABLE: 'Önizleme Yok',
  READINESS_UNAVAILABLE: 'Hazırlık Belirlenemedi',
  PIPELINE_UNAVAILABLE: 'Pipeline Belirlenemedi',
  REVISION_REQUESTED: 'Revizyon İstendi',
  MISSING_TARGET: 'Hedef Bulunamadı',
  BLOCKED_INTERNAL_WORKFLOW: 'İş Akışı Engeli',
  DEGRADED_VISIBILITY: 'Kısıtlı Görünürlük',
}

const CONTROL_HINT_LABELS: Record<SafeOperationalControlHint, string> = {
  REFRESH_OVERVIEW: 'Genel görünümü yenile',
  RECHECK_READINESS: 'Hazırlık durumunu kontrol et',
  VIEW_SAFE_PREVIEW: 'Güvenli önizlemeyi görüntüle',
  REVIEW_PIPELINE_DETAIL: 'Pipeline detayını incele',
  REQUEST_MANUAL_INVESTIGATION: 'Manuel inceleme öner',
}

const TARGET_TYPE_LABELS: Record<ReviewTargetType, string> = {
  CONTENT_PROJECT: 'İçerik Projesi',
  SCENARIO: 'Senaryo',
  STORYBOARD: 'Hikâye Tahtası',
  PACKAGE: 'Platform Paketi',
  IMAGE_ASSET: 'Görsel Varlık',
}

const TARGET_TYPE_OPTIONS: ReviewTargetType[] = [
  'CONTENT_PROJECT', 'SCENARIO', 'STORYBOARD', 'PACKAGE', 'IMAGE_ASSET',
]

function safeText(text: string): string {
  if (!text) return ''
  return text
    .replace(/[<>]/g, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .slice(0, 2000)
}

export default function WorkflowObservabilityPanel() {
  const [summary, setSummary] = useState<SafeWorkflowObservabilitySummary | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [diagnosticFilter, setDiagnosticFilter] = useState<ReviewTargetType | ''>('')

  const loadSummary = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await reviewApi.getWorkflowObservabilitySummary()
      if (res.success && res.data) {
        setSummary(res.data)
      }
    } catch (err: unknown) {
      const msg = err && typeof err === 'object' && 'message' in err
        ? String((err as { message: string }).message)
        : 'Gözlemlenebilirlik özeti yüklenemedi'
      setError(safeText(msg))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSummary()
  }, [loadSummary])

  const filteredDiagnostics = summary?.diagnostics?.filter(d =>
    !diagnosticFilter || d.targetType === diagnosticFilter
  ) ?? []

  return (
    <div className="workflow-observability-panel" data-testid="workflow-observability-panel">
      <h3 className="workflow-observability-panel__title">
        Dahili İş Akışı Gözlemi
      </h3>
      <p className="workflow-observability-panel__disclaimer">
        Sadece İç Kullanım — Bu panel işlem çalıştırmaz. Yayın/export değildir. Sağlayıcı/depolama detayı gösterilmez.
      </p>

      {/* Loading */}
      {loading && (
        <div className="workflow-observability-panel__loading" data-testid="observability-loading">
          Yükleniyor…
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="workflow-observability-panel__error" data-testid="observability-error" role="alert">
          {safeText(error)}
        </div>
      )}

      {/* Summary Cards */}
      {summary && !loading && (
        <div className="workflow-observability-panel__content">
          {/* Operasyonel Özet */}
          <div className="workflow-observability-panel__summary" data-testid="observability-summary">
            <h4>Operasyonel Özet</h4>

            {/* Review State Counts */}
            <div className="workflow-observability-panel__counts" data-testid="review-state-counts">
              <strong>İnceleme Durumu:</strong>
              {Object.keys(summary.countsByReviewState).length > 0 ? (
                <ul>
                  {Object.entries(summary.countsByReviewState).map(([state, count]) => (
                    <li key={state}>{safeText(state)}: {count}</li>
                  ))}
                </ul>
              ) : (
                <span> Veri yok</span>
              )}
            </div>

            {/* Pipeline Stage Counts */}
            <div className="workflow-observability-panel__counts" data-testid="pipeline-stage-counts">
              <strong>Pipeline Aşaması:</strong>
              {Object.keys(summary.countsByPipelineStage).length > 0 ? (
                <ul>
                  {Object.entries(summary.countsByPipelineStage).map(([stage, count]) => (
                    <li key={stage}>{safeText(stage)}: {count}</li>
                  ))}
                </ul>
              ) : (
                <span> Veri yok</span>
              )}
            </div>

            {/* Readiness Counts */}
            <div className="workflow-observability-panel__counts" data-testid="readiness-counts">
              <strong>Hazırlık Durumu:</strong>
              {Object.keys(summary.countsByReadinessAvailability).length > 0 ? (
                <ul>
                  {Object.entries(summary.countsByReadinessAvailability).map(([avail, count]) => (
                    <li key={avail}>{safeText(avail)}: {count}</li>
                  ))}
                </ul>
              ) : (
                <span> Veri yok</span>
              )}
            </div>

            {/* Quick Counts */}
            <div className="workflow-observability-panel__quick-counts" data-testid="quick-counts">
              <span>Önizleme: {summary.previewAvailableCount} mevcut / {summary.previewUnavailableCount} yok</span>
              <span> | Revizyon: {summary.revisionRequestedCount}</span>
              <span> | Kullanılamıyor: {summary.unavailableCount}</span>
            </div>
          </div>

          {/* Safe Warnings */}
          {summary.safeWarnings.length > 0 && (
            <div className="workflow-observability-panel__warnings" data-testid="observability-warnings">
              <strong>Güvenli Uyarılar:</strong>
              <ul>
                {summary.safeWarnings.map((w, i) => (
                  <li key={i}>{safeText(w)}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Diagnostics */}
          <div className="workflow-observability-panel__diagnostics" data-testid="observability-diagnostics">
            <h4>Güvenli Tanılar</h4>

            {/* Diagnostic Filter */}
            <div className="workflow-observability-panel__diagnostic-filter" data-testid="diagnostic-filter">
              <select
                data-testid="diagnostic-filter-target-type"
                value={diagnosticFilter}
                onChange={e => setDiagnosticFilter(e.target.value as ReviewTargetType | '')}
              >
                <option value="">Tümü</option>
                {TARGET_TYPE_OPTIONS.map(t => (
                  <option key={t} value={t}>{TARGET_TYPE_LABELS[t]}</option>
                ))}
              </select>
            </div>

            {/* Diagnostic Cards */}
            {filteredDiagnostics.length > 0 ? (
              <ul className="workflow-observability-panel__diagnostic-list" data-testid="diagnostic-list">
                {filteredDiagnostics.map((d, i) => (
                  <li key={`${d.targetType}-${d.targetId}-${i}`}
                      className="workflow-observability-panel__diagnostic-card"
                      data-testid="diagnostic-card">
                    <div className="diagnostic-card__header">
                      <span className="diagnostic-card__type-badge" data-testid="diagnostic-type-badge">
                        {DIAGNOSTIC_TYPE_LABELS[d.diagnosticType] || safeText(d.diagnosticType)}
                      </span>
                      <span className="diagnostic-card__target">
                        {TARGET_TYPE_LABELS[d.targetType] || safeText(d.targetType)}
                      </span>
                    </div>
                    <div className="diagnostic-card__body">
                      <p className="diagnostic-card__message">{safeText(d.safeMessage)}</p>
                      <p className="diagnostic-card__reason">
                        Kod: {safeText(d.safeReasonCode)}
                      </p>
                      {d.pipelineStage && (
                        <p className="diagnostic-card__stage">
                          Aşama: {safeText(d.pipelineStage)}
                        </p>
                      )}
                    </div>
                    {d.safeRecommendedHints.length > 0 && (
                      <div className="diagnostic-card__hints" data-testid="diagnostic-hints">
                        <strong>Öneriler:</strong>
                        <ul>
                          {d.safeRecommendedHints.map((h, hi) => (
                            <li key={hi} className="diagnostic-card__hint-label">
                              {CONTROL_HINT_LABELS[h] || safeText(h)}
                            </li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              <p data-testid="diagnostic-empty">Tanı öğesi bulunamadı.</p>
            )}
          </div>

          {/* Updated At */}
          {summary.updatedAt && (
            <div className="workflow-observability-panel__updated" data-testid="observability-updated">
              Son güncelleme: {new Date(summary.updatedAt).toLocaleString('tr-TR')}
            </div>
          )}
        </div>
      )}

      {/* Empty */}
      {!summary && !loading && !error && (
        <div data-testid="observability-empty">Gözlemlenebilirlik verisi bulunamadı.</div>
      )}
    </div>
  )
}
