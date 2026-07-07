import { useState, useCallback } from 'react'
import type {
  TurkuVideoBootstrapRequest,
  TurkuVideoProjectResponse,
  TurkuVideoProgressResponse,
  TurkuVideoWorkflowStage,
} from '../types'
import * as turkuApi from '../api/turkuVideoApi'

/**
 * FP-019 Task 05: Türkü Video Workflow admin page.
 *
 * Internal admin production workflow only.
 * No publish/export/public delivery.
 * No unsafe HTML rendering.
 * actorRole is workflow context only — TEST_FALLBACK compatibility.
 */

const STAGE_ORDER: TurkuVideoWorkflowStage[] = [
  'DRAFT_CREATED',
  'STORY_CONTEXT_PREPARED',
  'SCENARIO_PREPARED',
  'STORYBOARD_PREPARED',
  'SCENE_ASSET_PLAN_PREPARED',
  'IMAGE_BATCH_PREPARED',
  'IMAGE_BATCH_COMPLETED',
]

const STAGE_LABELS: Record<string, string> = {
  DRAFT_CREATED: 'Taslak Oluşturuldu',
  STORY_CONTEXT_PREPARED: 'Hikâye Paketi Hazır',
  SCENARIO_PREPARED: 'Senaryo Hazır',
  STORYBOARD_PREPARED: 'Storyboard Hazır',
  SCENE_ASSET_PLAN_PREPARED: 'Varlık Planı Hazır',
  IMAGE_BATCH_PREPARED: 'Görsel Batch Hazır',
  IMAGE_BATCH_COMPLETED: 'Görseller Tamamlandı',
  IMAGE_BATCH_PARTIAL: 'Görseller Kısmi',
  INTERNAL_PREVIEW_READY: 'Dahili Önizleme Hazır',
  NEEDS_REVISION: 'Revizyon Gerekli',
  BLOCKED: 'Engellendi',
  ARCHIVED: 'Arşivlendi',
}

interface StageAction {
  label: string
  enabledAt: TurkuVideoWorkflowStage
  action: (id: string) => Promise<unknown>
}

function TurkuVideoWorkflowPage() {
  // ── Bootstrap form state ──
  const [form, setForm] = useState<TurkuVideoBootstrapRequest>({ turkuName: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // ── Project state ──
  const [project, setProject] = useState<TurkuVideoProjectResponse | null>(null)
  const [progress, setProgress] = useState<TurkuVideoProgressResponse | null>(null)

  // ── Stage actions ──
  const stageActions: StageAction[] = [
    { label: 'Hikâye Hazırla', enabledAt: 'DRAFT_CREATED', action: (id) => turkuApi.prepareTurkuStory(id) },
    { label: 'Senaryo Hazırla', enabledAt: 'STORY_CONTEXT_PREPARED', action: (id) => turkuApi.prepareTurkuScenario(id) },
    { label: 'Storyboard Hazırla', enabledAt: 'SCENARIO_PREPARED', action: (id) => turkuApi.prepareTurkuStoryboard(id) },
    { label: 'Varlık Planı Hazırla', enabledAt: 'STORYBOARD_PREPARED', action: (id) => turkuApi.prepareTurkuSceneAssets(id) },
    { label: 'Görsel Batch Hazırla', enabledAt: 'SCENE_ASSET_PLAN_PREPARED', action: (id) => turkuApi.prepareTurkuImageBatch(id) },
    { label: 'Batch Tamamla', enabledAt: 'IMAGE_BATCH_PREPARED', action: (id) => turkuApi.completeTurkuImageBatch(id) },
  ]

  const refreshProject = useCallback(async (id: string) => {
    try {
      const [projRes, progRes] = await Promise.all([
        turkuApi.getTurkuVideoProject(id),
        turkuApi.getTurkuVideoProjectProgress(id),
      ])
      setProject(projRes.data)
      setProgress(progRes.data)
    } catch {
      // keep current state
    }
  }, [])

  const handleBootstrap = async () => {
    if (!form.turkuName.trim()) {
      setError('Türkü adı zorunludur')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await turkuApi.bootstrapTurkuVideoProject(form)
      if (res.data) {
        await refreshProject(res.data.id)
      }
    } catch (e: unknown) {
      setError(typeof e === 'object' && e !== null && 'message' in e ? String((e as { message: string }).message) : 'Bir hata oluştu')
    } finally {
      setLoading(false)
    }
  }

  const handleStageAction = async (action: StageAction) => {
    if (!project) return
    setLoading(true)
    setError(null)
    try {
      await action.action(project.id)
      await refreshProject(project.id)
    } catch (e: unknown) {
      setError(typeof e === 'object' && e !== null && 'message' in e ? String((e as { message: string }).message) : 'İşlem başarısız')
    } finally {
      setLoading(false)
    }
  }

  const currentStageIndex = project
    ? STAGE_ORDER.indexOf(project.workflowStage)
    : -1

  return (
    <div className="page-content">
      <div className="page-header">
        <h1 className="page-title">🎵 Türkü Video İş Akışı</h1>
        <p className="page-subtitle">Dahili üretim iş akışı — Yayın/dışa aktarım yok</p>
      </div>

      {error && (
        <div className="alert alert-error" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {/* ── Bootstrap Form ─────────────────────── */}
      {!project && (
        <div className="card" data-testid="bootstrap-form">
          <div className="card-header">
            <h2 className="card-title">Yeni Türkü Projesi Başlat</h2>
            <span className="badge badge-warning">Dahili İş Akışı</span>
          </div>
          <div className="card-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label" htmlFor="turkuName">Türkü Adı *</label>
                <input
                  id="turkuName"
                  className="form-input"
                  type="text"
                  value={form.turkuName}
                  onChange={(e) => setForm({ ...form, turkuName: e.target.value })}
                  placeholder="ör. Mihriban"
                  maxLength={200}
                  required
                  data-testid="input-turku-name"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="region">Bölge</label>
                <input
                  id="region"
                  className="form-input"
                  type="text"
                  value={form.region || ''}
                  onChange={(e) => setForm({ ...form, region: e.target.value || undefined })}
                  placeholder="ör. Erzurum"
                  data-testid="input-region"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="mood">Atmosfer</label>
                <input
                  id="mood"
                  className="form-input"
                  type="text"
                  value={form.mood || ''}
                  onChange={(e) => setForm({ ...form, mood: e.target.value || undefined })}
                  placeholder="ör. hüzünlü, özlem"
                  data-testid="input-mood"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="targetDuration">Hedef Süre (dk)</label>
                <input
                  id="targetDuration"
                  className="form-input"
                  type="number"
                  min={1}
                  max={30}
                  value={form.targetDurationMinutes || ''}
                  onChange={(e) => setForm({ ...form, targetDurationMinutes: e.target.value ? Number(e.target.value) : undefined })}
                  data-testid="input-duration"
                />
              </div>
              <div className="form-group form-group-full">
                <label className="form-label" htmlFor="sourceNotes">Kaynak Notları</label>
                <textarea
                  id="sourceNotes"
                  className="form-input"
                  rows={2}
                  value={form.sourceNotes || ''}
                  onChange={(e) => setForm({ ...form, sourceNotes: e.target.value || undefined })}
                  placeholder="Türkünün bilinen kaynağı hakkında kısa not"
                  data-testid="input-source-notes"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="narrationStyle">Anlatım Tarzı</label>
                <input
                  id="narrationStyle"
                  className="form-input"
                  type="text"
                  value={form.narrationStyle || ''}
                  onChange={(e) => setForm({ ...form, narrationStyle: e.target.value || undefined })}
                  placeholder="ör. hikâyeci"
                  data-testid="input-narration-style"
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="visualStyle">Görsel Tarz</label>
                <input
                  id="visualStyle"
                  className="form-input"
                  type="text"
                  value={form.visualStyle || ''}
                  onChange={(e) => setForm({ ...form, visualStyle: e.target.value || undefined })}
                  placeholder="ör. suluboya"
                  data-testid="input-visual-style"
                />
              </div>
            </div>
            <div className="form-actions">
              <button
                className="btn btn-primary"
                onClick={handleBootstrap}
                disabled={loading || !form.turkuName.trim()}
                data-testid="btn-bootstrap"
              >
                {loading ? 'Başlatılıyor…' : '🎬 Projeyi Başlat'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Project Summary ───────────────────── */}
      {project && (
        <div className="card" data-testid="project-summary">
          <div className="card-header">
            <h2 className="card-title">📋 Proje Özeti</h2>
            <span className="badge badge-info">Dahili Taslak</span>
          </div>
          <div className="card-body">
            <div className="detail-grid">
              <div className="detail-item">
                <span className="detail-label">Proje ID</span>
                <span className="detail-value" data-testid="project-id">{project.id}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Türkü</span>
                <span className="detail-value" data-testid="project-name">{project.turkuName}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Aşama</span>
                <span className="detail-value" data-testid="project-stage">
                  {STAGE_LABELS[project.workflowStage] || project.workflowStage}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Kaynak Güvenilirliği</span>
                <span className="detail-value" data-testid="source-confidence">{project.sourceConfidence}</span>
              </div>
            </div>

            {/* Review flags */}
            <div className="review-flags" data-testid="review-flags">
              {project.needsHumanReview && (
                <span className="badge badge-warning" data-testid="flag-human-review">👤 İnsan İncelemesi Gerekli</span>
              )}
              {project.copyrightReviewRequired && (
                <span className="badge badge-error" data-testid="flag-copyright">©️ Telif İncelemesi Gerekli</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Progress Panel ────────────────────── */}
      {progress && (
        <div className="card" data-testid="progress-panel">
          <div className="card-header">
            <h2 className="card-title">📊 İlerleme Durumu</h2>
          </div>
          <div className="card-body">
            <div className="stage-timeline" data-testid="stage-timeline">
              {STAGE_ORDER.map((stage, i) => {
                const isDone = i < currentStageIndex
                const isCurrent = i === currentStageIndex
                return (
                  <div
                    key={stage}
                    className={`stage-step ${isDone ? 'done' : ''} ${isCurrent ? 'current' : ''}`}
                    data-testid={`stage-${stage}`}
                  >
                    <span className="stage-dot">{isDone ? '✅' : isCurrent ? '🔵' : '⚪'}</span>
                    <span className="stage-label">{STAGE_LABELS[stage]}</span>
                  </div>
                )
              })}
            </div>
            <p className="progress-summary" data-testid="progress-summary">
              {progress.safeProgressSummary}
            </p>
            <p className="next-action" data-testid="next-action">
              Sonraki: <strong>{progress.nextAvailableAction}</strong>
            </p>
          </div>
        </div>
      )}

      {/* ── Stage Actions ─────────────────────── */}
      {project && (
        <div className="card" data-testid="stage-actions">
          <div className="card-header">
            <h2 className="card-title">⚡ İş Akışı Aksiyonları</h2>
            <span className="badge badge-info">Dahili Üretim</span>
          </div>
          <div className="card-body">
            <div className="action-buttons">
              {stageActions.map((sa) => (
                <button
                  key={sa.label}
                  className="btn btn-secondary"
                  disabled={loading || project.workflowStage !== sa.enabledAt}
                  onClick={() => handleStageAction(sa)}
                  data-testid={`btn-${sa.enabledAt}`}
                >
                  {sa.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── Internal Preview ──────────────────── */}
      {project && progress && currentStageIndex >= 1 && (
        <div className="card" data-testid="internal-preview">
          <div className="card-header">
            <h2 className="card-title">🔍 Dahili Önizleme</h2>
            <span className="badge badge-warning">Sadece İç Kullanım</span>
          </div>
          <div className="card-body">
            <p className="preview-disclaimer" data-testid="preview-disclaimer">
              ⚠️ Bu önizleme yalnızca dahili inceleme içindir. Herkese açık yayın, dışa aktarım veya teslimat değildir.
            </p>

            {/* Story summary */}
            {currentStageIndex >= 1 && (
              <div className="preview-section" data-testid="preview-story">
                <h3>📖 Hikâye Özeti</h3>
                <p className="preview-text">Hikâye paketi hazırlandı.</p>
              </div>
            )}

            {/* Scenario summary */}
            {currentStageIndex >= 2 && (
              <div className="preview-section" data-testid="preview-scenario">
                <h3>🎬 Senaryo</h3>
                <p className="preview-text">6 beat senaryo planı hazır.</p>
              </div>
            )}

            {/* Storyboard summary */}
            {currentStageIndex >= 3 && (
              <div className="preview-section" data-testid="preview-storyboard">
                <h3>🎨 Storyboard</h3>
                <p className="preview-text">Sahne bazlı storyboard planı hazır.</p>
              </div>
            )}

            {/* Asset plan summary */}
            {currentStageIndex >= 4 && (
              <div className="preview-section" data-testid="preview-assets">
                <h3>🖼️ Varlık Planı</h3>
                <p className="preview-text">Sahne görsel varlık planı hazır.</p>
              </div>
            )}

            {/* Image batch summary */}
            {currentStageIndex >= 5 && (
              <div className="preview-section" data-testid="preview-image-batch">
                <h3>🖌️ Görsel Batch</h3>
                <p className="preview-text">Görsel üretim batch'i hazır — dahili referanslar.</p>
              </div>
            )}

            {/* Completion */}
            {currentStageIndex >= 6 && (
              <div className="preview-section" data-testid="preview-completed">
                <h3>✅ Görseller Tamamlandı</h3>
                <p className="preview-text">Tüm sahne görselleri dahili olarak hazır.</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── New project button ────────────────── */}
      {project && (
        <div className="form-actions">
          <button
            className="btn btn-ghost"
            onClick={() => { setProject(null); setProgress(null); setForm({ turkuName: '' }); setError(null) }}
            data-testid="btn-new-project"
          >
            ➕ Yeni Proje Başlat
          </button>
        </div>
      )}
    </div>
  )
}

export default TurkuVideoWorkflowPage
