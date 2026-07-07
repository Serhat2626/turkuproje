import { useState, useCallback } from 'react'
import type {
  TurkuVideoBootstrapRequest,
  TurkuVideoProjectResponse,
  TurkuVideoProgressResponse,
  TurkuVideoWorkflowStage,
  TurkuGenerationSafetyResponse,
  TurkuProviderMode,
  TurkuCompositionPreviewResponse,
  TurkuRenderReadinessReport,
  TurkuRenderJobResponse,
  TurkuRenderExecutionResponse,
  TurkuRenderExecutionSafetyResponse,
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
  'COMPOSITION_PLAN_PREPARED',
  'RENDER_MANIFEST_PREPARED',
  'RENDER_JOB_PREPARED',
  'RENDER_JOB_RUNNING',
  'RENDER_JOB_COMPLETED_INTERNAL',
  'RENDER_JOB_FAILED_SAFE',
  'INTERNAL_RENDER_ARTIFACT_READY',
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
  COMPOSITION_PLAN_PREPARED: 'Kompozisyon Planı Hazır',
  RENDER_MANIFEST_PREPARED: 'Render Manifest Hazır',
  RENDER_JOB_PREPARED: 'Render Job Hazır',
  RENDER_JOB_RUNNING: 'Render Çalışıyor',
  RENDER_JOB_COMPLETED_INTERNAL: 'Dahili Render Tamamlandı',
  RENDER_JOB_FAILED_SAFE: 'Render Güvenli Kapandı',
  INTERNAL_RENDER_ARTIFACT_READY: 'Dahili Artifact Hazır',
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

const PROVIDER_MODE_LABELS: Record<TurkuProviderMode, string> = {
  DETERMINISTIC_STUB: 'Deterministik Stub',
  PROVIDER_DRAFT: 'Sağlayıcı Taslak',
  PROVIDER_DISABLED: 'Sağlayıcı Devre Dışı',
  PROVIDER_FAILED_SAFE: 'Güvenli Hata Modu',
  HUMAN_REVIEW_REQUIRED: 'İnsan İncelemesi Gerekli',
}

const PROVIDER_MODE_BADGE_CLASS: Record<TurkuProviderMode, string> = {
  DETERMINISTIC_STUB: 'badge-info',
  PROVIDER_DRAFT: 'badge-warning',
  PROVIDER_DISABLED: 'badge-error',
  PROVIDER_FAILED_SAFE: 'badge-error',
  HUMAN_REVIEW_REQUIRED: 'badge-warning',
}

function TurkuVideoWorkflowPage() {
  // ── Bootstrap form state ──
  const [form, setForm] = useState<TurkuVideoBootstrapRequest>({ turkuName: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // ── Project state ──
  const [project, setProject] = useState<TurkuVideoProjectResponse | null>(null)
  const [progress, setProgress] = useState<TurkuVideoProgressResponse | null>(null)
  const [safety, setSafety] = useState<TurkuGenerationSafetyResponse | null>(null)
  const [compositionPreview, setCompositionPreview] = useState<TurkuCompositionPreviewResponse | null>(null)
  const [readiness, setReadiness] = useState<TurkuRenderReadinessReport | null>(null)
  const [renderJob, setRenderJob] = useState<TurkuRenderJobResponse | null>(null)
  const [renderExecution, setRenderExecution] = useState<TurkuRenderExecutionResponse | null>(null)
  const [executionSafety, setExecutionSafety] = useState<TurkuRenderExecutionSafetyResponse | null>(null)

  // ── Stage actions ──
  const stageActions: StageAction[] = [
    { label: 'Hikâye Hazırla', enabledAt: 'DRAFT_CREATED', action: (id) => turkuApi.prepareTurkuStory(id) },
    { label: 'Senaryo Hazırla', enabledAt: 'STORY_CONTEXT_PREPARED', action: (id) => turkuApi.prepareTurkuScenario(id) },
    { label: 'Storyboard Hazırla', enabledAt: 'SCENARIO_PREPARED', action: (id) => turkuApi.prepareTurkuStoryboard(id) },
    { label: 'Varlık Planı Hazırla', enabledAt: 'STORYBOARD_PREPARED', action: (id) => turkuApi.prepareTurkuSceneAssets(id) },
    { label: 'Görsel Batch Hazırla', enabledAt: 'SCENE_ASSET_PLAN_PREPARED', action: (id) => turkuApi.prepareTurkuImageBatch(id) },
    { label: 'Batch Tamamla', enabledAt: 'IMAGE_BATCH_PREPARED', action: (id) => turkuApi.completeTurkuImageBatch(id) },
    { label: 'Kompozisyon Planı Hazırla', enabledAt: 'IMAGE_BATCH_COMPLETED', action: (id) => turkuApi.prepareTurkuCompositionPlan(id) },
    { label: 'Render Manifest Hazırla', enabledAt: 'COMPOSITION_PLAN_PREPARED', action: (id) => turkuApi.prepareTurkuRenderManifest(id) },
    { label: 'Render Job Hazırla', enabledAt: 'RENDER_MANIFEST_PREPARED', action: (id) => turkuApi.prepareTurkuRenderJob(id) },
    { label: 'Render Job Çalıştır', enabledAt: 'RENDER_JOB_PREPARED', action: (id) => turkuApi.runTurkuRenderJob(id) },
  ]

  const refreshProject = useCallback(async (id: string) => {
    try {
      const [projRes, progRes, safetyRes] = await Promise.all([
        turkuApi.getTurkuVideoProject(id),
        turkuApi.getTurkuVideoProjectProgress(id),
        turkuApi.getTurkuGenerationSafety(id),
      ])
      setProject(projRes.data)
      setProgress(progRes.data)
      setSafety(safetyRes.data)
      // Composition/readiness — safe to fail
      try {
        const [compRes, readRes] = await Promise.all([
          turkuApi.getTurkuCompositionPreview(id),
          turkuApi.getTurkuRenderReadiness(id),
        ])
        setCompositionPreview(compRes.data)
        setReadiness(readRes.data)
      } catch {
        // composition/readiness not available yet
      }
      // Render job / execution safety — safe to fail
      try {
        const [jobRes, execSafetyRes] = await Promise.all([
          turkuApi.getTurkuRenderJob(id),
          turkuApi.getTurkuRenderExecutionSafety(id),
        ])
        setRenderJob(jobRes.data)
        setExecutionSafety(execSafetyRes.data)
      } catch {
        // render job not available yet
      }
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

  const handleGenerateAction = async (label: string, apiCall: (id: string) => Promise<unknown>) => {
    if (!project) return
    setLoading(true)
    setError(null)
    try {
      await apiCall(project.id)
      await refreshProject(project.id)
    } catch (e: unknown) {
      setError(typeof e === 'object' && e !== null && 'message' in e ? String((e as { message: string }).message) : 'İşlem başarısız: ' + label)
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

      {/* ── Provider Mode & Safety Panel ──────────────── */}
      {project && safety && (
        <div className="card" data-testid="safety-panel">
          <div className="card-header">
            <h2 className="card-title">🛡️ Üretim Güvenlik Durumu</h2>
            <span
              className={`badge ${PROVIDER_MODE_BADGE_CLASS[safety.providerMode]}`}
              data-testid="provider-mode-badge"
            >
              {PROVIDER_MODE_LABELS[safety.providerMode]}
            </span>
          </div>
          <div className="card-body">
            <div className="detail-grid">
              <div className="detail-item">
                <span className="detail-label">Sağlayıcı Modu</span>
                <span className="detail-value" data-testid="safety-provider-mode">{safety.providerMode}</span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Üretim Uygun</span>
                <span className="detail-value" data-testid="safety-generation-available">
                  {safety.generationAvailable ? '✅ Evet' : '❌ Hayır'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">İnsan İncelemesi</span>
                <span className="detail-value" data-testid="safety-human-review">
                  {safety.humanReviewRequired ? '✅ Gerekli' : 'Gerekmez'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Otomatik Onay</span>
                <span className="detail-value" data-testid="safety-auto-approved">
                  {safety.autoApproved ? 'Evet' : '❌ Hayır / Asla'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Telif İncelemesi</span>
                <span className="detail-value" data-testid="safety-copyright-review">
                  {safety.copyrightReviewRequired ? '✅ Gerekli' : 'Gerekmez'}
                </span>
              </div>
              <div className="detail-item">
                <span className="detail-label">Söz (Şarkı Sözü)</span>
                <span className="detail-value" data-testid="safety-lyrics">
                  {safety.lyricsIncluded ? 'Dahil' : '❌ Dahil Değil'}
                </span>
              </div>
            </div>

            {/* Blocked state */}
            {!safety.generationAvailable && safety.blockedReasonSummary && (
              <div className="alert alert-error" data-testid="safety-blocked-alert">
                <span className="alert-icon">🚫</span>
                <span>{safety.blockedReasonSummary}</span>
                {safety.safeFailureCode && (
                  <span className="badge badge-error" data-testid="safety-failure-code">
                    {safety.safeFailureCode}
                  </span>
                )}
              </div>
            )}

            {/* Review flags */}
            {safety.reviewFlags && safety.reviewFlags.length > 0 && (
              <div className="review-flags" data-testid="safety-review-flags">
                {safety.reviewFlags.map((flag) => (
                  <span key={flag} className="badge badge-warning">{flag}</span>
                ))}
              </div>
            )}

            {/* Safe warnings */}
            {safety.safeWarnings && safety.safeWarnings.length > 0 && (
              <div className="safe-warnings" data-testid="safety-warnings">
                {safety.safeWarnings.map((warn, i) => (
                  <p key={i} className="warning-text">⚠️ {warn}</p>
                ))}
              </div>
            )}

            {/* Safe trace reference */}
            {safety.lastTraceRef && (
              <div className="detail-item" data-testid="safety-trace-ref">
                <span className="detail-label">İz Referansı</span>
                <span className="detail-value">
                  {safety.lastTraceRef.providerAlias} / {safety.lastTraceRef.generationStage}
                </span>
              </div>
            )}
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
            <h2 className="card-title">⚡ Hazırlık Aksiyonları</h2>
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

      {/* ── Generate Actions (FP-020) ───────────── */}
      {project && safety && (
        <div className="card" data-testid="generate-actions">
          <div className="card-header">
            <h2 className="card-title">🧪 Sağlayıcı Üretim Aksiyonları</h2>
            {safety.providerMode && (
              <span
                className={`badge ${PROVIDER_MODE_BADGE_CLASS[safety.providerMode]}`}
              >
                {PROVIDER_MODE_LABELS[safety.providerMode]}
              </span>
            )}
          </div>
          <div className="card-body">
            {!safety.generationAvailable && (
              <div className="alert alert-error" data-testid="generate-blocked-alert">
                <span className="alert-icon">🚫</span>
                <span>Üretim engellendi: {safety.blockedReasonSummary || safety.safeFailureCode}</span>
              </div>
            )}
            <div className="action-buttons">
              <button
                className="btn btn-primary"
                disabled={loading || !safety.canGenerateStory}
                onClick={() => handleGenerateAction('Hikâye Üret', turkuApi.generateTurkuStory)}
                data-testid="btn-generate-story"
              >
                📝 Hikâye Üret
              </button>
              <button
                className="btn btn-primary"
                disabled={loading || !safety.canGenerateScenario}
                onClick={() => handleGenerateAction('Senaryo Üret', turkuApi.generateTurkuScenario)}
                data-testid="btn-generate-scenario"
              >
                🎬 Senaryo Üret
              </button>
              <button
                className="btn btn-primary"
                disabled={loading || !safety.canGenerateStoryboard}
                onClick={() => handleGenerateAction('Storyboard Üret', turkuApi.generateTurkuStoryboard)}
                data-testid="btn-generate-storyboard"
              >
                🎨 Storyboard Üret
              </button>
              <button
                className="btn btn-accent"
                disabled={loading || !safety.canGeneratePlanningBundle}
                onClick={() => handleGenerateAction('Planlama Paketi Üret', turkuApi.generateTurkuPlanningBundle)}
                data-testid="btn-generate-planning-bundle"
              >
                📦 Planlama Paketi Üret
              </button>
              <button
                className="btn btn-primary"
                disabled={loading || !safety.canRunImageBatch}
                onClick={() => handleGenerateAction('Görsel Batch Çalıştır', turkuApi.runTurkuImageBatch)}
                data-testid="btn-run-image-batch"
              >
                🖼️ Görsel Batch Çalıştır
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-021: Composition / Render Planning ─── */}
      {project && compositionPreview && (
        <div className="card" data-testid="composition-planning-section">
          <div className="card-header">
            <h2 className="card-title">🎬 Kompozisyon / Render Planlaması</h2>
            <span className="badge badge-warning">Dahili Planlama — Yayın/Dışa Aktarım Yok</span>
          </div>
          <div className="card-body">
            <p className="preview-disclaimer" data-testid="composition-disclaimer">
              ⚠️ Bu bölüm dahili render planlaması içindir. Gerçek video üretimi, dışa aktarım veya yayın yoktur.
            </p>

            {/* Readiness Panel */}
            <div className="preview-section" data-testid="readiness-panel">
              <h3>📊 Render Hazırlık Durumu</h3>
              <div className="status-grid">
                <div className="status-item">
                  <span className="status-label">Durum:</span>
                  <span className={`badge ${compositionPreview.readinessStatus === 'BLOCKED' ? 'badge-error' : 'badge-warning'}`}
                        data-testid="readiness-status">
                    {compositionPreview.readinessStatus}
                  </span>
                </div>
                <div className="status-item">
                  <span className="status-label">Kompozisyon:</span>
                  <span className={`badge ${compositionPreview.compositionReady ? 'badge-success' : 'badge-error'}`}>
                    {compositionPreview.compositionReady ? '✅ Hazır' : '❌ Hazır Değil'}
                  </span>
                </div>
                <div className="status-item">
                  <span className="status-label">Manifest:</span>
                  <span className={`badge ${compositionPreview.manifestReady ? 'badge-success' : 'badge-error'}`}>
                    {compositionPreview.manifestReady ? '✅ Hazır' : '❌ Hazır Değil'}
                  </span>
                </div>
                {readiness && (
                  <div className="status-item">
                    <span className="status-label">Puan:</span>
                    <span className="badge badge-info" data-testid="readiness-score">{readiness.readinessScore}/100</span>
                  </div>
                )}
              </div>

              {/* Readiness Issues */}
              {compositionPreview.readinessIssues.length > 0 && (
                <div data-testid="readiness-issues">
                  <h4>⚠️ Sorunlar</h4>
                  <ul className="issue-list">
                    {compositionPreview.readinessIssues.map((issue, idx) => (
                      <li key={idx} className="issue-item">{issue}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Review Flags */}
              <div className="flag-list" data-testid="review-flags">
                {compositionPreview.reviewFlags.map((flag, idx) => (
                  <span key={idx} className="badge badge-outline">{flag}</span>
                ))}
              </div>
            </div>

            {/* Composition Timeline */}
            {compositionPreview.timelineScenes.length > 0 && (
              <div className="preview-section" data-testid="composition-timeline">
                <h3>🎞️ Kompozisyon Timeline ({compositionPreview.sceneCount} sahne — {compositionPreview.totalDurationSeconds.toFixed(1)}s)</h3>
                <div className="timeline-list">
                  {compositionPreview.timelineScenes.map((scene) => (
                    <div key={scene.sceneNo} className="timeline-scene" data-testid={`timeline-scene-${scene.sceneNo}`}>
                      <div className="scene-header">
                        <span className="scene-number">#{scene.sceneNo}</span>
                        <span className="scene-title">{scene.sceneTitle}</span>
                        <span className="badge badge-info">{scene.durationSeconds.toFixed(1)}s</span>
                      </div>
                      <div className="scene-details">
                        <div className="scene-detail"><span className="detail-label">Anlatım:</span> {scene.narrationSummary}</div>
                        <div className="scene-detail"><span className="detail-label">Görsel:</span> {scene.imageRefLabel}</div>
                        <div className="scene-detail"><span className="detail-label">Ruh hali:</span> {scene.mood}</div>
                        {scene.transitionIn && <div className="scene-detail"><span className="detail-label">Geçiş giriş:</span> {scene.transitionIn}</div>}
                        {scene.transitionOut && <div className="scene-detail"><span className="detail-label">Geçiş çıkış:</span> {scene.transitionOut}</div>}
                        {scene.cameraDirection && <div className="scene-detail"><span className="detail-label">Kamera:</span> {scene.cameraDirection}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Safety Summary */}
            <div className="preview-section" data-testid="composition-safety-summary">
              <h3>🛡️ Güvenlik Özeti</h3>
              <ul className="safety-list">
                <li>İnsan incelemesi gerekli: <strong>Evet</strong></li>
                <li>Otomatik onay: <strong>Hayır</strong></li>
                <li>Telif incelemesi gerekli: <strong>Evet</strong></li>
                <li>Yayın hazır: <strong>Hayır — sadece dahili planlama</strong></li>
                <li>Dışa aktarım hazır: <strong>Hayır</strong></li>
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-022: Render Job & Execution Safety ── */}
      {project && progress && currentStageIndex >= 8 && (
        <div className="card" data-testid="render-execution-section">
          <div className="card-header">
            <h2 className="card-title">🔧 Dahili Render İşlemi</h2>
            <span className="badge badge-warning">Internal Stub Render</span>
          </div>
          <div className="card-body">
            <p className="preview-disclaimer" data-testid="render-no-publish-label">
              ⚠️ Dahili render hazırlığı — Yayın/Dışa Aktarım Yok — İndirilebilir video yok
            </p>

            {/* Render Job Status */}
            {renderJob && (
              <div className="preview-section" data-testid="render-job-status-panel">
                <h3>📋 Render Job Durumu</h3>
                <ul className="safety-list">
                  <li>Job ID: <strong>{renderJob.jobId ?? '—'}</strong></li>
                  <li>Durum: <strong>{renderJob.status ?? 'Hazırlanmadı'}</strong></li>
                  <li>Aşama: <strong>{STAGE_LABELS[renderJob.currentStage] ?? renderJob.currentStage}</strong></li>
                  <li>Manifest Ref: <strong>{renderJob.manifestRef ?? '—'}</strong></li>
                  <li>Deneme Sayısı: <strong>{renderJob.attemptCount}</strong></li>
                  <li>Human Review Required: <strong>true</strong></li>
                  <li>Auto Approved: <strong>false</strong></li>
                  <li>Not Publish Ready: <strong>true</strong></li>
                  <li>Not Export Ready: <strong>true</strong></li>
                </ul>
                <div className="review-flags" data-testid="render-job-review-flags">
                  {renderJob.reviewFlags.map((flag, i) => (
                    <span key={i} className="badge badge-info">{flag}</span>
                  ))}
                </div>
              </div>
            )}

            {/* Execution Result */}
            {renderExecution && (
              <div className="preview-section" data-testid="render-execution-result-panel">
                <h3>🎬 Render Çalıştırma Sonucu</h3>
                <ul className="safety-list">
                  <li>Attempt ID: <strong>{renderExecution.attemptId ?? '—'}</strong></li>
                  <li>Durum: <strong>{renderExecution.status ?? '—'}</strong></li>
                  <li>Trace Ref: <strong>{renderExecution.safeTraceRef ?? '—'}</strong></li>
                  <li>Artifact Ref: <strong>{renderExecution.artifactRef ?? '—'}</strong></li>
                  {renderExecution.failureCode && (
                    <li>Hata Kodu: <strong>{renderExecution.failureCode}</strong></li>
                  )}
                </ul>
              </div>
            )}

            {/* Execution Safety Envelope */}
            {executionSafety && (
              <div className="preview-section" data-testid="render-execution-safety-panel">
                <h3>🛡️ Render Güvenlik Zarfı</h3>
                <ul className="safety-list">
                  <li>Render Mode: <strong>{executionSafety.safetyEnvelope.renderMode}</strong></li>
                  <li>Human Review Required: <strong>{String(executionSafety.safetyEnvelope.humanReviewRequired)}</strong></li>
                  <li>Copyright Review Required: <strong>{String(executionSafety.safetyEnvelope.copyrightReviewRequired)}</strong></li>
                  <li>Auto Approved: <strong>{String(executionSafety.safetyEnvelope.autoApproved)}</strong></li>
                  <li>Lyrics Included: <strong>{String(executionSafety.safetyEnvelope.lyricsIncluded)}</strong></li>
                  <li>Copyright Clearance: <strong>{String(executionSafety.safetyEnvelope.copyrightClearanceVerified)}</strong></li>
                </ul>
                <div className="review-flags" data-testid="execution-safety-flags">
                  {executionSafety.safetyEnvelope.safetyFlags.map((flag, i) => (
                    <span key={i} className="badge badge-success">{flag}</span>
                  ))}
                </div>
                <div className="review-flags" data-testid="execution-review-flags">
                  {executionSafety.safetyEnvelope.reviewFlags.map((flag, i) => (
                    <span key={i} className="badge badge-warning">{flag}</span>
                  ))}
                </div>
              </div>
            )}

            {/* Safety Summary */}
            <div className="preview-section" data-testid="render-safety-summary">
              <h3>🛡️ Güvenlik Özeti</h3>
              <ul className="safety-list">
                <li>Internal Stub Render: <strong>Evet</strong></li>
                <li>Gerçek video rendering: <strong>Hayır</strong></li>
                <li>İndirilebilir dosya: <strong>Hayır</strong></li>
                <li>Yayın hazır: <strong>Hayır — sadece dahili</strong></li>
                <li>Dışa aktarım hazır: <strong>Hayır</strong></li>
                <li>Human Review Required: <strong>Evet</strong></li>
                <li>Auto Approved: <strong>false</strong></li>
              </ul>
            </div>

            {/* FP-023: Renderer Mode & Adapter Status */}
            <div className="preview-section" data-testid="renderer-adapter-panel">
              <h3>⚙️ Renderer Adapter Durumu</h3>
              <ul className="safety-list">
                <li>Adapter Mode: <strong data-testid="renderer-mode-badge">STUB</strong></li>
                <li>FFmpeg Durumu: <strong data-testid="ffmpeg-sandbox-label">Kapalı (Varsayılan)</strong></li>
                <li>Sandbox Render: <strong>Evet — Gerçek render kapalı</strong></li>
                <li>Process Execution: <strong>Hayır — No ProcessBuilder</strong></li>
                <li>Gerçek render: <strong>Kapalı veya güvenli yapılandırma gerektirir</strong></li>
              </ul>
              <div className="review-flags" data-testid="adapter-safety-flags">
                <span className="badge badge-info">NO_REAL_RENDER</span>
                <span className="badge badge-info">NO_SHELL_EXECUTION</span>
                <span className="badge badge-info">NO_USER_ARGS</span>
                <span className="badge badge-info">STUB_DEFAULT</span>
              </div>
            </div>

            {/* FP-023: Internal Artifact Metadata */}
            <div className="preview-section" data-testid="artifact-metadata-panel">
              <h3>📦 Dahili Render Artifact</h3>
              <ul className="safety-list">
                <li>Artifact Ref: <strong data-testid="artifact-opaque-ref">{renderExecution?.artifactRef ?? '—'}</strong></li>
                <li>Artifact Status: <strong data-testid="artifact-status-label">Dahili</strong></li>
                <li>Format: <strong>mp4</strong></li>
                <li>Opaque Ref Only: <strong>Evet</strong></li>
                <li>Public URL: <strong>Yok</strong></li>
                <li>İndirilebilir dosya: <strong>Yok</strong></li>
              </ul>
            </div>

            {/* FP-023: Retention & Cleanup */}
            <div className="preview-section" data-testid="retention-cleanup-panel">
              <h3>🗑️ Retention / Cleanup</h3>
              <ul className="safety-list">
                <li>Retention Policy: <strong data-testid="retention-policy-label">default-24h</strong></li>
                <li>Retention Süresi: <strong>24 saat</strong></li>
                <li>Auto Cleanup: <strong>Kapalı</strong></li>
                <li>Cleanup Eligibility: <strong>Metadata-only — dosya silinmez</strong></li>
              </ul>
            </div>

            {/* FP-023: Dahili Render Güvenliği */}
            <div className="preview-section" data-testid="render-adapter-safety-summary">
              <h3>🛡️ Dahili Render Güvenliği</h3>
              <ul className="safety-list">
                <li>Stub/Sandbox render: <strong>Evet</strong></li>
                <li>Gerçek render kapalı veya güvenli yapılandırma gerektirir: <strong>Evet</strong></li>
                <li>Yayın/Dışa Aktarım Yok: <strong>Evet</strong></li>
                <li>İndirilebilir dosya yok: <strong>Evet</strong></li>
                <li>Public URL yok: <strong>Evet</strong></li>
                <li>Not Publish Ready: <strong>true</strong></li>
                <li>Not Export Ready: <strong>true</strong></li>
                <li>Human Review Required: <strong>true</strong></li>
                <li>Copyright Review Required: <strong>true</strong></li>
                <li>Auto Approved: <strong>false</strong></li>
              </ul>
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
            onClick={() => { setProject(null); setProgress(null); setSafety(null); setCompositionPreview(null); setReadiness(null); setRenderJob(null); setRenderExecution(null); setExecutionSafety(null); setForm({ turkuName: '' }); setError(null) }}
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
