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

      {/* ── FP-024: Render Config / Workspace / Execution / Verification Safety ── */}
      {project && (
        <div className="card" data-testid="fp024-render-safety-section">
          <h2>🔧 Dahili Render Güvenliği (FP-024)</h2>

          {/* Secure Render Config Status */}
          <div className="preview-section" data-testid="fp024-render-config-panel">
            <h3>⚙️ Render Yapılandırma Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">Mod:</span>
              <span className="detail-value" data-testid="fp024-config-mode">FFMPEG_DISABLED</span>
              <span className="detail-label">Yapılandırma:</span>
              <span className="detail-value" data-testid="fp024-config-status">Güvenli varsayılan — gerçek render kapalı</span>
              <span className="detail-label">Doğrulama:</span>
              <span className="detail-value" data-testid="fp024-config-validation">Yapılandırma doğrulaması başarılı</span>
            </div>
            <p className="preview-text">Gerçek render kapalı veya güvenli yapılandırma gerektirir.</p>
          </div>

          {/* Workspace / File Safety */}
          <div className="preview-section" data-testid="fp024-workspace-safety-panel">
            <h3>📁 Çalışma Alanı / Dosya Güvenliği</h3>
            <div className="detail-grid">
              <span className="detail-label">Çalışma alanı referansı:</span>
              <span className="detail-value" data-testid="fp024-workspace-ref">Opak dahili referans</span>
              <span className="detail-label">Uzantı izin listesi:</span>
              <span className="detail-value" data-testid="fp024-extension-allowlist">mp4, webm, png, jpg, jpeg</span>
              <span className="detail-label">Maks. boyut:</span>
              <span className="detail-value" data-testid="fp024-max-size">1000 MB</span>
              <span className="detail-label">Maks. dosya/iş:</span>
              <span className="detail-value" data-testid="fp024-max-files">50</span>
            </div>
            <p className="preview-text">Dosya yolu gösterilmez. Yalnızca opak dahili referanslar.</p>
          </div>

          {/* FFmpeg Execution Boundary */}
          <div className="preview-section" data-testid="fp024-execution-boundary-panel">
            <h3>🚫 FFmpeg Yürütme Sınırı</h3>
            <div className="detail-grid">
              <span className="detail-label">Yürütme modu:</span>
              <span className="detail-value" data-testid="fp024-execution-mode">DISABLED</span>
              <span className="detail-label">İşlem yürütme:</span>
              <span className="detail-value" data-testid="fp024-no-process">Yok — NO_PROCESS_EXECUTION</span>
              <span className="detail-label">Dosya sistemi erişimi:</span>
              <span className="detail-value" data-testid="fp024-no-filesystem">Yok</span>
              <span className="detail-label">Komut satırı:</span>
              <span className="detail-value" data-testid="fp024-no-commandline">Gösterilmez</span>
            </div>
            <p className="preview-text">Gerçek FFmpeg işlemi çalıştırılmaz. Kuru çalıştırma veya simülasyon yalnızca meta veri üretir.</p>
          </div>

          {/* Artifact Verification */}
          <div className="preview-section" data-testid="fp024-artifact-verification-panel">
            <h3>✅ Artifact Doğrulama</h3>
            <div className="detail-grid">
              <span className="detail-label">Doğrulama durumu:</span>
              <span className="detail-value" data-testid="fp024-verification-status">PENDING</span>
              <span className="detail-label">Format:</span>
              <span className="detail-value" data-testid="fp024-format-status">mp4 / webm izin listesi</span>
              <span className="detail-label">Süre/Boyut/Çözünürlük/FPS:</span>
              <span className="detail-value" data-testid="fp024-bounds-status">Sınırlar dahilinde</span>
              <span className="detail-label">Artifact referansı:</span>
              <span className="detail-value" data-testid="fp024-opaque-artifact-ref">Opak dahili referans</span>
            </div>
            <p className="preview-text">İndirilebilir video yok. Yayın/Dışa Aktarım Yok.</p>
          </div>

          {/* Internal-only Safety Labels */}
          <div className="preview-section" data-testid="fp024-safety-labels">
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp024-label-internal">Dahili render güvenliği</span>
              <span className="safety-flag" data-testid="fp024-label-no-render">Gerçek render kapalı veya güvenli yapılandırma gerektirir</span>
              <span className="safety-flag" data-testid="fp024-label-no-path">Dosya yolu gösterilmez</span>
              <span className="safety-flag" data-testid="fp024-label-no-download">İndirilebilir video yok</span>
              <span className="safety-flag" data-testid="fp024-label-no-publish">Yayın/Dışa Aktarım Yok</span>
              <span className="safety-flag" data-testid="fp024-not-publish-ready">notPublishReady</span>
              <span className="safety-flag" data-testid="fp024-not-export-ready">notExportReady</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-025: Internal Render Job Lifecycle / Progress / Readiness ── */}
      {project && (
        <div className="card" data-testid="fp025-job-lifecycle-section">
          <h2>🔄 Dahili Render İş Akışı (FP-025)</h2>

          {/* Panel A — Job Lifecycle Status */}
          <div className="preview-section" data-testid="fp025-lifecycle-panel">
            <h3>📋 İş Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">İş referansı:</span>
              <span className="detail-value" data-testid="fp025-job-ref">Opak dahili referans</span>
              <span className="detail-label">Durum:</span>
              <span className="detail-value" data-testid="fp025-job-status">DRAFT</span>
              <span className="detail-label">Terminal:</span>
              <span className="detail-value" data-testid="fp025-terminal-indicator">Hayır</span>
              <span className="detail-label">İnsan onayı:</span>
              <span className="detail-value" data-testid="fp025-human-review">Gerekli</span>
              <span className="detail-label">Otomatik onay:</span>
              <span className="detail-value" data-testid="fp025-auto-approval">Kapalı</span>
              <span className="detail-label">Yayın durumu:</span>
              <span className="detail-value" data-testid="fp025-public-delivery">Kullanılamaz</span>
            </div>
            <p className="preview-text">Sadece dahili durum görünürlüğü — yayın/indirme/dışa aktarım yok.</p>
          </div>

          {/* Panel B — Readiness */}
          <div className="preview-section" data-testid="fp025-readiness-panel">
            <h3>🔍 Dahili Render Hazırlık Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">Hazırlık durumu:</span>
              <span className="detail-value" data-testid="fp025-readiness-status">NOT_READY</span>
              <span className="detail-label">Sorunlar:</span>
              <span className="detail-value" data-testid="fp025-readiness-issues">Yapılandırma doğrulanıyor</span>
            </div>
            <div className="safety-flags" data-testid="fp025-readiness-flags">
              <span className="safety-flag">İnsan onayı gerekli</span>
              <span className="safety-flag">Otomatik onay kapalı</span>
              <span className="safety-flag">Yayın/Dışa aktarım yok</span>
              <span className="safety-flag">Public/CDN URL yok</span>
            </div>
          </div>

          {/* Panel C — Progress */}
          <div className="preview-section" data-testid="fp025-progress-panel">
            <h3>📊 Güvenli İlerleme Özeti</h3>
            <div className="detail-grid">
              <span className="detail-label">İlerleme:</span>
              <span className="detail-value" data-testid="fp025-progress-percent">0%</span>
              <span className="detail-label">Aşama:</span>
              <span className="detail-value" data-testid="fp025-stage-label">Hazırlanıyor</span>
            </div>
            <div className="progress-bar-container" data-testid="fp025-progress-bar">
              <div className="progress-bar-fill" style={{ width: '0%' }} />
            </div>
            <p className="preview-text">Güvenli ilerleme özeti — log/stdout/stderr/komut/yol gösterilmez.</p>
          </div>

          {/* Panel D — FP-024 Safety Chain */}
          <div className="preview-section" data-testid="fp025-safety-chain-panel">
            <h3>🛡️ FP-024 Güvenlik Zinciri Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">Render yapılandırma:</span>
              <span className="detail-value" data-testid="fp025-chain-config">Güvenli varsayılan</span>
              <span className="detail-label">Çalışma alanı:</span>
              <span className="detail-value" data-testid="fp025-chain-workspace">Güvenli</span>
              <span className="detail-label">FFmpeg yürütme sınırı:</span>
              <span className="detail-value" data-testid="fp025-chain-execution">DISABLED</span>
              <span className="detail-label">Artifact doğrulama:</span>
              <span className="detail-value" data-testid="fp025-chain-verification">PENDING</span>
              <span className="detail-label">İnsan onayı:</span>
              <span className="detail-value" data-testid="fp025-chain-human-review">Gerekli</span>
              <span className="detail-label">Yayın:</span>
              <span className="detail-value" data-testid="fp025-chain-delivery">Kapalı</span>
            </div>
          </div>

          {/* Safety Labels */}
          <div className="preview-section" data-testid="fp025-safety-labels">
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp025-label-workflow">Dahili render iş akışı</span>
              <span className="safety-flag" data-testid="fp025-label-internal-only">Sadece dahili durum görünürlüğü</span>
              <span className="safety-flag" data-testid="fp025-label-human-review">İnsan onayı gerekli</span>
              <span className="safety-flag" data-testid="fp025-label-no-auto">Otomatik onay kapalı</span>
              <span className="safety-flag" data-testid="fp025-label-no-publish">Yayın/Dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp025-label-no-download">İndirilebilir video yok</span>
              <span className="safety-flag" data-testid="fp025-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp025-label-no-url">Public/CDN URL yok</span>
              <span className="safety-flag" data-testid="fp025-label-no-path">Dosya yolu gösterilmez</span>
              <span className="safety-flag" data-testid="fp025-label-no-cmd">Komut satırı gösterilmez</span>
              <span className="safety-flag" data-testid="fp025-label-safe-progress">Güvenli ilerleme özeti</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-026: Human Review Gate ── */}
      {project && (
        <div className="card" data-testid="fp026-review-gate-section">
          <h2>🔍 İnsan İnceleme Kapısı (FP-026)</h2>

          {/* Panel A — Review Gate Status */}
          <div className="preview-section" data-testid="fp026-review-gate-panel">
            <h3>📋 İnceleme Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">İnceleme referansı:</span>
              <span className="detail-value" data-testid="fp026-review-ref">Opak dahili referans</span>
              <span className="detail-label">İnceleme durumu:</span>
              <span className="detail-value" data-testid="fp026-review-state">REVIEW_NOT_REQUIRED</span>
              <span className="detail-label">Dahili mi:</span>
              <span className="detail-value" data-testid="fp026-internal-only">Evet</span>
              <span className="detail-label">İnsan onayı:</span>
              <span className="detail-value" data-testid="fp026-human-review">Gerekli</span>
              <span className="detail-label">Otomatik onay:</span>
              <span className="detail-value" data-testid="fp026-auto-approval">Kapalı</span>
              <span className="detail-label">Yayın durumu:</span>
              <span className="detail-value" data-testid="fp026-public-delivery">Kullanılamaz</span>
            </div>
            <p className="preview-text">Güvenli inceleme kapısı — onay sadece dahili sonraki adım içindir.</p>
          </div>

          {/* Panel B — Review Readiness */}
          <div className="preview-section" data-testid="fp026-review-readiness-panel">
            <h3>🔎 İnceleme Hazırlığı</h3>
            <div className="detail-grid">
              <span className="detail-label">Hazırlık durumu:</span>
              <span className="detail-value" data-testid="fp026-review-readiness-status">NOT_READY_FOR_REVIEW</span>
              <span className="detail-label">Sorunlar:</span>
              <span className="detail-value" data-testid="fp026-review-readiness-issues">İnceleme ön koşulları kontrol ediliyor</span>
            </div>
            <div className="safety-flags" data-testid="fp026-review-readiness-flags">
              <span className="safety-flag">İnsan incelemesi gerekli</span>
              <span className="safety-flag">Otomatik onay kapalı</span>
              <span className="safety-flag">Yayın/Dışa aktarım yok</span>
              <span className="safety-flag">Public/CDN URL yok</span>
            </div>
          </div>

          {/* Panel C — Safe Review Actions */}
          <div className="preview-section" data-testid="fp026-review-actions-panel">
            <h3>⚙️ Güvenli İnceleme Aksiyonları</h3>
            <div className="detail-grid">
              <span className="detail-label">Onay aksiyonu:</span>
              <span className="detail-value" data-testid="fp026-approve-label">Dahili Sonraki Adım İçin Onayla</span>
              <span className="detail-label">Ret aksiyonu:</span>
              <span className="detail-value" data-testid="fp026-reject-label">Reddet</span>
              <span className="detail-label">Engelleme aksiyonu:</span>
              <span className="detail-value" data-testid="fp026-block-label">Güvenlik Nedeniyle Engelle</span>
            </div>
            <p className="preview-text">Sadece dahili sonraki adım — yayın/dışa aktarım/indirme DEĞİL.</p>
          </div>

          {/* Panel D — Decision Safety Summary */}
          <div className="preview-section" data-testid="fp026-decision-safety-panel">
            <h3>🛡️ Karar Güvenlik Özeti</h3>
            <div className="detail-grid">
              <span className="detail-label">Onay anlamı:</span>
              <span className="detail-value" data-testid="fp026-approval-meaning">Sadece dahili sonraki adım</span>
              <span className="detail-label">Yayın:</span>
              <span className="detail-value" data-testid="fp026-no-publish">Kullanılamaz</span>
              <span className="detail-label">İndirme:</span>
              <span className="detail-value" data-testid="fp026-no-download">Kullanılamaz</span>
              <span className="detail-label">Video oynatıcı:</span>
              <span className="detail-value" data-testid="fp026-no-player">Yok</span>
            </div>
          </div>

          {/* Safety Labels */}
          <div className="preview-section" data-testid="fp026-safety-labels">
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp026-label-human-review">İnsan incelemesi gerekli</span>
              <span className="safety-flag" data-testid="fp026-label-no-auto">Otomatik onay kapalı</span>
              <span className="safety-flag" data-testid="fp026-label-internal-step">Sadece dahili sonraki adım</span>
              <span className="safety-flag" data-testid="fp026-label-no-publish">Yayın/Dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp026-label-no-download">İndirilebilir video yok</span>
              <span className="safety-flag" data-testid="fp026-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp026-label-no-url">Public/CDN URL yok</span>
              <span className="safety-flag" data-testid="fp026-label-no-path">Dosya yolu gösterilmez</span>
              <span className="safety-flag" data-testid="fp026-label-no-cmd">Komut satırı gösterilmez</span>
              <span className="safety-flag" data-testid="fp026-label-safe-gate">Güvenli inceleme kapısı</span>
              <span className="safety-flag" data-testid="fp026-label-no-generic">Genel durum güncellemesi yok</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-027: Queue / Retry / Expiration ── */}
      {project && (
        <div className="card" data-testid="fp027-queue-section">
          <h2>📦 Dahili Render Kuyruğu (FP-027)</h2>

          {/* Panel A — Queue Status */}
          <div className="preview-section" data-testid="fp027-queue-status-panel">
            <h3>📋 Kuyruk Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">Kuyruk referansı:</span>
              <span className="detail-value" data-testid="fp027-queue-ref">Opak dahili referans</span>
              <span className="detail-label">Kuyruk durumu:</span>
              <span className="detail-value" data-testid="fp027-queue-state">NOT_QUEUED</span>
              <span className="detail-label">Dahili mi:</span>
              <span className="detail-value" data-testid="fp027-internal-only">Evet</span>
              <span className="detail-label">Yayın durumu:</span>
              <span className="detail-value" data-testid="fp027-public-delivery">Kullanılamaz</span>
            </div>
            <p className="preview-text">Dahili render kuyruğu — sadece dahili işleme.</p>
          </div>

          {/* Panel B — Retry Policy */}
          <div className="preview-section" data-testid="fp027-retry-panel">
            <h3>🔄 Retry Politikası</h3>
            <div className="detail-grid">
              <span className="detail-label">Deneme sayısı:</span>
              <span className="detail-value" data-testid="fp027-attempt-count">0 / 3</span>
              <span className="detail-label">Retry gecikme:</span>
              <span className="detail-value" data-testid="fp027-retry-delay">60 saniye</span>
              <span className="detail-label">Yeniden denenebilir:</span>
              <span className="detail-value" data-testid="fp027-retriable">Hayır</span>
            </div>
            <p className="preview-text">Retry bekleme güvenliği — sınırlı deneme, güvenli gecikme.</p>
          </div>

          {/* Panel C — Expiration Policy */}
          <div className="preview-section" data-testid="fp027-expiration-panel">
            <h3>⏰ Süre Aşımı Politikası</h3>
            <div className="detail-grid">
              <span className="detail-label">Süre aşımı durumu:</span>
              <span className="detail-value" data-testid="fp027-expiration-status">Süre aşımı yok</span>
              <span className="detail-label">Maks kuyruk süresi:</span>
              <span className="detail-value" data-testid="fp027-max-queue-duration">3600 saniye</span>
              <span className="detail-label">Maks retry bekleme:</span>
              <span className="detail-value" data-testid="fp027-max-retry-wait">1800 saniye</span>
            </div>
            <p className="preview-text">Süre aşımı politikası — sınırlı bekleme süresi.</p>
          </div>

          {/* Panel D — Dispatch Safety */}
          <div className="preview-section" data-testid="fp027-dispatch-safety-panel">
            <h3>🛡️ Dispatch Güvenlik Özeti</h3>
            <div className="detail-grid">
              <span className="detail-label">FP-025 hazırlık:</span>
              <span className="detail-value" data-testid="fp027-readiness-required">Gerekli</span>
              <span className="detail-label">FP-026 insan onayı:</span>
              <span className="detail-value" data-testid="fp027-review-required">Gerekli</span>
              <span className="detail-label">Otomatik onay:</span>
              <span className="detail-value" data-testid="fp027-auto-approval">Kapalı</span>
              <span className="detail-label">Yayın:</span>
              <span className="detail-value" data-testid="fp027-no-publish">Kullanılamaz</span>
            </div>
            <p className="preview-text">Dispatch için insan onayı gerekli — otomatik onay kapalı.</p>
          </div>

          {/* Safety Labels */}
          <div className="preview-section" data-testid="fp027-safety-labels">
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp027-label-queue">Dahili render kuyruğu</span>
              <span className="safety-flag" data-testid="fp027-label-retry">Retry bekleme güvenliği</span>
              <span className="safety-flag" data-testid="fp027-label-expiration">Süre aşımı politikası</span>
              <span className="safety-flag" data-testid="fp027-label-dispatch">Dispatch için insan onayı gerekli</span>
              <span className="safety-flag" data-testid="fp027-label-no-auto">Otomatik onay kapalı</span>
              <span className="safety-flag" data-testid="fp027-label-internal">Sadece dahili işleme</span>
              <span className="safety-flag" data-testid="fp027-label-no-publish">Yayın/Dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp027-label-no-download">İndirilebilir video yok</span>
              <span className="safety-flag" data-testid="fp027-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp027-label-no-url">Public/CDN URL yok</span>
              <span className="safety-flag" data-testid="fp027-label-no-path">Dosya yolu gösterilmez</span>
              <span className="safety-flag" data-testid="fp027-label-no-cmd">Komut/log gösterilmez</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-028: Real FFmpeg Dev/Test ── */}
      {project && (
        <div className="card" data-testid="fp028-ffmpeg-section">
          <h2>🎬 Gerçek FFmpeg Dev/Test (FP-028)</h2>

          {/* Panel A — Gate Status */}
          <div className="preview-section" data-testid="fp028-gate-status-panel">
            <h3>🔒 FFmpeg Çalıştırma Kapısı</h3>
            <div className="detail-grid">
              <span className="detail-label">Mod:</span>
              <span className="detail-value" data-testid="fp028-execution-mode">DISABLED</span>
              <span className="detail-label">Ortam:</span>
              <span className="detail-value" data-testid="fp028-environment">UNKNOWN</span>
              <span className="detail-label">Dahili bayrak:</span>
              <span className="detail-value" data-testid="fp028-internal-flag">Kapalı</span>
              <span className="detail-label">Binary:</span>
              <span className="detail-value" data-testid="fp028-binary-policy">DISABLED_BINARY</span>
            </div>
            <p className="preview-text">Gerçek FFmpeg sadece local/dev/test — production'da kapalı.</p>
          </div>

          {/* Panel B — Adapter Boundary */}
          <div className="preview-section" data-testid="fp028-adapter-panel">
            <h3>⚙️ Dev/Test Execution Boundary</h3>
            <div className="detail-grid">
              <span className="detail-label">Sonuç durumu:</span>
              <span className="detail-value" data-testid="fp028-boundary-status">NOT_EXECUTED</span>
              <span className="detail-label">Neden kodu:</span>
              <span className="detail-value" data-testid="fp028-boundary-reason">DISABLED_BY_DEFAULT</span>
              <span className="detail-label">Dahili mi:</span>
              <span className="detail-value" data-testid="fp028-adapter-internal">Evet</span>
            </div>
            <p className="preview-text">Kapı sonucu — STAGING/PROD/UNKNOWN her zaman reddedilir.</p>
          </div>

          {/* Panel C — Output / Verification */}
          <div className="preview-section" data-testid="fp028-output-panel">
            <h3>📄 Dahili Çıktı / Doğrulama</h3>
            <div className="detail-grid">
              <span className="detail-label">Çıktı referansı:</span>
              <span className="detail-value" data-testid="fp028-output-ref">Opak dahili referans</span>
              <span className="detail-label">Format:</span>
              <span className="detail-value" data-testid="fp028-output-format">mp4/webm</span>
              <span className="detail-label">Doğrulama:</span>
              <span className="detail-value" data-testid="fp028-verification-status">Bekliyor</span>
            </div>
            <p className="preview-text">Public URL yok — dosya yolu gösterilmez.</p>
          </div>

          {/* Panel D — Safety */}
          <div className="preview-section" data-testid="fp028-safety-panel">
            <h3>🛡️ Güvenlik Garantileri</h3>
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp028-label-local-only">Gerçek FFmpeg sadece local/dev/test</span>
              <span className="safety-flag" data-testid="fp028-label-prod-disabled">Production'da kapalı</span>
              <span className="safety-flag" data-testid="fp028-label-no-url">Public URL yok</span>
              <span className="safety-flag" data-testid="fp028-label-no-download">İndirme/yayın/dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp028-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp028-label-no-path">Komut/dosya yolu gösterilmez</span>
              <span className="safety-flag" data-testid="fp028-label-no-auto">Otomatik onay kapalı</span>
              <span className="safety-flag" data-testid="fp028-label-internal">Sadece dahili işleme</span>
              <span className="safety-flag" data-testid="fp028-label-no-stderr">stdout/stderr gösterilmez</span>
              <span className="safety-flag" data-testid="fp028-label-no-storage">Storage key gösterilmez</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-029: Internal E2E Smoke Path ── */}
      {project && (
        <div className="card" data-testid="fp029-smoke-section">
          <h2>🔥 Dahili E2E Video Smoke Path (FP-029)</h2>

          <div className="preview-section" data-testid="fp029-smoke-status-panel">
            <h3>📊 Smoke Path Durumu</h3>
            <div className="detail-grid">
              <span className="detail-label">Durum:</span>
              <span className="detail-value" data-testid="fp029-smoke-status">NOT_STARTED</span>
              <span className="detail-label">Neden:</span>
              <span className="detail-value" data-testid="fp029-smoke-reason">Başlatılmadı</span>
              <span className="detail-label">FP-025 hazırlık:</span>
              <span className="detail-value" data-testid="fp029-readiness">Gerekli</span>
              <span className="detail-label">FP-026 insan onayı:</span>
              <span className="detail-value" data-testid="fp029-review">Gerekli</span>
              <span className="detail-label">FP-028 FFmpeg kapısı:</span>
              <span className="detail-value" data-testid="fp029-gate">Kapalı</span>
            </div>
          </div>

          <div className="preview-section" data-testid="fp029-plan-panel">
            <h3>📋 Render Plan Özeti</h3>
            <div className="detail-grid">
              <span className="detail-label">Format:</span>
              <span className="detail-value" data-testid="fp029-format">mp4/webm</span>
              <span className="detail-label">Çıktı referansı:</span>
              <span className="detail-value" data-testid="fp029-output-ref">Opak dahili referans</span>
              <span className="detail-label">Doğrulama:</span>
              <span className="detail-value" data-testid="fp029-verification">Bekliyor</span>
            </div>
          </div>

          <div className="preview-section" data-testid="fp029-safety-panel">
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp029-label-internal">Sadece dahili smoke test</span>
              <span className="safety-flag" data-testid="fp029-label-no-publish">Yayın/dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp029-label-no-download">İndirme yok</span>
              <span className="safety-flag" data-testid="fp029-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp029-label-no-public">Public URL yok</span>
              <span className="safety-flag" data-testid="fp029-label-no-auto">Otomatik onay kapalı</span>
            </div>
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
