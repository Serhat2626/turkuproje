import { useState, useCallback, useEffect } from 'react'
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

// ── Müzik Upload Alt Bileşeni ──
function MusicUploadSection({ projectId }: { projectId: string }) {
  const [fonStatus, setFonStatus] = useState<'idle'|'uploading'|'done'|'error'>('idle')
  const [fonName, setFonName] = useState('')
  const [fonSize, setFonSize] = useState('')
  const [turkuStatus, setTurkuStatus] = useState<'idle'|'uploading'|'done'|'error'>('idle')
  const [turkuName, setTurkuName] = useState('')
  const [turkuSize, setTurkuSize] = useState('')

  useEffect(() => {
    turkuApi.getMusicStatus(projectId).then(res => {
      if (res.data?.fonUploaded) { setFonStatus('done'); setFonName('(önceden yüklendi)') }
      if (res.data?.turkuUploaded) { setTurkuStatus('done'); setTurkuName('(önceden yüklendi)') }
    }).catch(() => {})
  }, [projectId])

  const handleUpload = async (type: 'fon' | 'turku', file: File) => {
    const setStatus = type === 'fon' ? setFonStatus : setTurkuStatus
    const setName = type === 'fon' ? setFonName : setTurkuName
    const setSize = type === 'fon' ? setFonSize : setTurkuSize

    setName(file.name)
    setSize((file.size / (1024 * 1024)).toFixed(1) + ' MB')
    setStatus('uploading')
    try {
      const res = await turkuApi.uploadMusic(projectId, type, file)
      console.log('Upload response:', JSON.stringify(res))
      setStatus(res.data ? 'done' : 'error')
    } catch (err: any) {
      console.error('Upload error:', err)
      setStatus('error')
    }
  }

  const renderSlot = (
    type: 'fon' | 'turku',
    icon: string, label: string, subtitle: string,
    status: 'idle'|'uploading'|'done'|'error',
    name: string, size: string,
    gradient: string
  ) => (
    <div style={{
      padding: '16px', borderRadius: '12px',
      border: `1px solid ${status === 'done' ? 'rgba(34,197,94,0.4)' : status === 'error' ? 'rgba(239,68,68,0.4)' : 'rgba(255,255,255,0.1)'}`,
      background: status === 'done' ? 'rgba(34,197,94,0.05)' : status === 'error' ? 'rgba(239,68,68,0.05)' : 'rgba(255,255,255,0.03)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px', flexWrap: 'wrap' }}>
        <span>{icon}</span>
        <strong>{label}</strong>
        <span style={{ fontSize: '0.75rem', color: 'rgba(255,255,255,0.4)' }}>({subtitle})</span>
        {status === 'done' && <span style={{ fontSize: '0.8rem', color: '#22c55e', fontWeight: 700 }}>✅ Yüklendi</span>}
        {status === 'uploading' && <span style={{ fontSize: '0.8rem', color: '#f59e0b' }}>⏳ Yükleniyor...</span>}
        {status === 'error' && <span style={{ fontSize: '0.8rem', color: '#ef4444' }}>❌ Hata</span>}
      </div>
      {name && <div style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)', marginBottom: '8px' }}>📄 {name} {size && `(${size})`}</div>}
      <input type="file" accept="audio/*" id={`${type}-music-upload`} style={{ display: 'none' }}
        onChange={(e) => { const f = e.target.files?.[0]; if (f) handleUpload(type, f) }} />
      <label htmlFor={`${type}-music-upload`} style={{
        display: 'inline-block', padding: '8px 16px', borderRadius: '8px',
        background: status === 'uploading' ? '#666' : gradient, color: 'white',
        cursor: status === 'uploading' ? 'wait' : 'pointer', fontSize: '0.85rem', fontWeight: 600,
        pointerEvents: status === 'uploading' ? 'none' : 'auto'
      }}>
        {status === 'done' ? '🔄 Değiştir' : '📁 Dosya Seç'}
      </label>
    </div>
  )

  return (
    <div className="card" style={{ marginTop: '24px' }}>
      <h2 style={{ fontSize: '1.4rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '10px' }}>
        🎵 Müzik Yönetimi
      </h2>
      <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '0.85rem', marginBottom: '16px' }}>
        Video üretiminden ÖNCE müzik dosyalarını yükleyin. Fon müziği narration altında %12 sesle çalar, türkü kaydı videonun sonuna eklenir.
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
        {renderSlot('fon', '🎸', 'Fon Müziği', 'enstrümantal', fonStatus, fonName, fonSize, 'linear-gradient(135deg, #6366f1, #8b5cf6)')}
        {renderSlot('turku', '🎤', 'Türkü Kaydı', 'sözlü, telifsiz', turkuStatus, turkuName, turkuSize, 'linear-gradient(135deg, #f59e0b, #ef4444)')}
      </div>
    </div>
  )
}

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
  const [form, setForm] = useState<TurkuVideoBootstrapRequest>({
    turkuName: 'Mihriban',
    region: 'Erzurum',
    mood: 'hüzünlü, hasret, kavuşamama, platonik aşk',
    targetDurationMinutes: 10,
    sourceNotes: 'Âşık Sümmani\'nin Mihriban\'a duyduğu karşılıksız aşkın hikâyesi. 19. yüzyıl Erzurum. Âşıklık geleneği, atışma sahnesi, kavuşamama acısı ve türkünün yüzyıllardır yaşayan mirası.',
    narrationStyle: 'hikâyeci, duygusal belgesel',
    visualStyle: 'sinematik suluboya',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [activeAction, setActiveAction] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  // ── Project state ──
  const [project, setProject] = useState<TurkuVideoProjectResponse | null>(null)
  const [progress, setProgress] = useState<TurkuVideoProgressResponse | null>(null)
  const [safety, setSafety] = useState<TurkuGenerationSafetyResponse | null>(null)
  const [compositionPreview, setCompositionPreview] = useState<TurkuCompositionPreviewResponse | null>(null)
  const [readiness, setReadiness] = useState<TurkuRenderReadinessReport | null>(null)
  const [renderJob, setRenderJob] = useState<TurkuRenderJobResponse | null>(null)
  const [renderExecution, setRenderExecution] = useState<TurkuRenderExecutionResponse | null>(null)
  const [executionSafety, setExecutionSafety] = useState<TurkuRenderExecutionSafetyResponse | null>(null)
  const [content, setContent] = useState<any>(null)
  const [activeTab, setActiveTab] = useState<'story' | 'scenario' | 'storyboard'>('story')
  const [ttsLoading, setTtsLoading] = useState(false)
  const [autoRunning, setAutoRunning] = useState(false)
  const [autoRunProgress, setAutoRunProgress] = useState('')
  const [videoAssembling, setVideoAssembling] = useState(false)
  const [videoReady, setVideoReady] = useState(false)

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
      // Proje ID'yi localStorage'a kaydet — sayfa yenilendiğinde geri yüklensin
      localStorage.setItem('turkuActiveProjectId', id)
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
      // Content fetch
      try {
        const contentRes = await turkuApi.getTurkuVideoProjectContent(id)
        setContent(contentRes.data)
      } catch {
        // content not available yet
      }
      // Video availability check — MinIO'da video var mı?
      try {
        const videoUrl = turkuApi.getVideoUrl(id)
        // HEAD bazen proxy/CORS ile sorun yaratır, GET + Range header daha güvenilir
        const checkRes = await fetch(videoUrl, { method: 'GET', headers: { Range: 'bytes=0-0' } })
        if (checkRes.ok || checkRes.status === 206) {
          setVideoReady(true)
        }
      } catch {
        // video not available yet
      }
    } catch {
      // Proje backend'de bulunamazsa localStorage temizle
      localStorage.removeItem('turkuActiveProjectId')
    }
  }, [])

  // Sayfa açılışında localStorage'dan proje ID'yi geri yükle
  useEffect(() => {
    const savedId = localStorage.getItem('turkuActiveProjectId')
    if (savedId) {
      refreshProject(savedId)
    }
  }, [refreshProject])

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
    setActiveAction(action.label)
    setError(null)
    setSuccessMessage(null)
    try {
      await action.action(project.id)
      await refreshProject(project.id)
      setSuccessMessage(`✅ ${action.label} başarıyla tamamlandı!`)
      setTimeout(() => setSuccessMessage(null), 5000)
    } catch (e: unknown) {
      setError(typeof e === 'object' && e !== null && 'message' in e ? String((e as { message: string }).message) : 'İşlem başarısız')
    } finally {
      setActiveAction(null)
    }
  }

  const handleGenerateAction = async (label: string, apiCall: (id: string) => Promise<unknown>) => {
    if (!project) return
    setActiveAction(label)
    setError(null)
    setSuccessMessage(null)
    try {
      await apiCall(project.id)
      await refreshProject(project.id)
      setSuccessMessage(`✅ ${label} başarıyla tamamlandı!`)
      setTimeout(() => setSuccessMessage(null), 5000)
    } catch (e: unknown) {
      setError(typeof e === 'object' && e !== null && 'message' in e ? String((e as { message: string }).message) : 'İşlem başarısız: ' + label)
    } finally {
      setActiveAction(null)
    }
  }

  // ── Tümünü Otomatik Çalıştır ──
  // Akış: 1) OpenAI Planlama Paketi → 2) Kalan hazırlık adımları → 3) TTS Seslendirme
  const handleRunAll = async () => {
    if (!project) return
    setAutoRunning(true)
    setError(null)
    setSuccessMessage(null)

    const currentId = project.id

    try {
      // ── ADIM 1: OpenAI ile Planlama Paketi (Hikâye + Senaryo + Storyboard) ──
      const projRes = await turkuApi.getTurkuVideoProject(currentId)
      const currentStage = projRes.data?.workflowStage

      if (currentStage === 'DRAFT_CREATED') {
        setAutoRunProgress('⏳ 1/3: OpenAI ile Hikâye + Senaryo + Storyboard üretiliyor...')
        setActiveAction('Planlama Paketi Üret (OpenAI)')
        try {
          await turkuApi.generateTurkuPlanningBundle(currentId)
          await refreshProject(currentId)
        } catch (e: any) {
          setError(`Planlama Paketi hatası: ${e.message || 'Bilinmeyen hata'}`)
          setAutoRunning(false)
          setAutoRunProgress('')
          setActiveAction(null)
          return
        }
        setActiveAction(null)
      }

      // ── ADIM 2: Kalan hazırlık adımları (Varlık Planı → Render) ──
      // Storyboard sonrası adımlar: index 3-9
      const remainingActions = stageActions.slice(3) // Varlık Planı'ndan itibaren
      for (let i = 0; i < remainingActions.length; i++) {
        const sa = remainingActions[i]

        // Güncel durumu kontrol et
        const p = await turkuApi.getTurkuVideoProject(currentId)
        setProject(p.data)

        if (!p.data || p.data.workflowStage !== sa.enabledAt) continue

        setAutoRunProgress(`⏳ 2/3: ${i + 1}/${remainingActions.length} — ${sa.label}...`)
        setActiveAction(sa.label)

        try {
          await sa.action(currentId)
          await refreshProject(currentId)
        } catch (e: any) {
          setError(`${sa.label} hatası: ${e.message || 'Bilinmeyen hata'}`)
          setAutoRunning(false)
          setAutoRunProgress('')
          setActiveAction(null)
          return
        }
        setActiveAction(null)
      }

      // ── ADIM 3: Görseller + TTS Ses + FFmpeg Video ──
      setAutoRunProgress('⏳ 3/3: Görseller + TTS ses + FFmpeg video üretiliyor... (bu adım uzun sürebilir)')
      setActiveAction('Tam Video Pipeline')
      try {
        const fullRes = await turkuApi.generateFullVideo(currentId)
        if (fullRes.data?.success || fullRes.data?.videoUrl) {
          setVideoReady(true)
        }
      } catch {
        // Pipeline hatası — timeout olabilir ama video oluşmuş olabilir
      }
      setActiveAction(null)

      // Video kontrolü — pipeline başarılı da olsa başarısız da olsa kontrol et
      try {
        const videoUrl = turkuApi.getVideoUrl(currentId)
        const checkRes = await fetch(videoUrl, { method: 'GET', headers: { Range: 'bytes=0-0' } })
        if (checkRes.ok || checkRes.status === 206) {
          setVideoReady(true)
        }
      } catch {
        // video not available — ama yine de bir süre sonra tekrar dene
        setTimeout(async () => {
          try {
            const videoUrl2 = turkuApi.getVideoUrl(currentId)
            const retryRes = await fetch(videoUrl2, { method: 'GET', headers: { Range: 'bytes=0-0' } })
            if (retryRes.ok || retryRes.status === 206) setVideoReady(true)
          } catch { /* still not available */ }
        }, 5000)
      }

      // Son durumu güncelle
      await refreshProject(currentId)
      setSuccessMessage('🎉 Tam video pipeline tamamlandı! Hikâye, Senaryo, Storyboard içeriklerini okuyabilir ve videoyu izleyebilirsin.')
    } catch (e: any) {
      setError('Otomatik çalıştırma hatası: ' + (e.message || 'Bilinmeyen hata'))
    } finally {
      setAutoRunning(false)
      setAutoRunProgress('')
      setActiveAction(null)
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

      {successMessage && (
        <div className="alert alert-success" role="status" style={{
          background: 'rgba(16, 185, 129, 0.15)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          borderRadius: '12px',
          padding: '12px 20px',
          marginBottom: '16px',
          color: '#10b981',
          fontWeight: 600,
          fontSize: '0.95rem',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          animation: 'fadeIn 0.3s ease',
        }}>
          <span>{successMessage}</span>
        </div>
      )}

      {activeAction && (
        <div style={{
          background: 'rgba(99, 102, 241, 0.12)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          borderRadius: '12px',
          padding: '16px 20px',
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
          animation: 'fadeIn 0.3s ease',
        }}>
          <div style={{
            width: '24px', height: '24px',
            border: '3px solid rgba(99, 102, 241, 0.3)',
            borderTopColor: '#6366f1',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
          }} />
          <span style={{ color: '#a5b4fc', fontWeight: 600, fontSize: '0.95rem' }}>
            ⏳ {activeAction} çalışıyor...
          </span>
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

      {/* ── Üretilen İçerik ── */}
      {content && (content.story || content.scenario || content.storyboard) && (
        <div className="card" style={{ marginTop: '24px' }}>
          <h2 style={{ fontSize: '1.4rem', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            📖 Üretilen İçerik
          </h2>

          {/* Tab buttons */}
          <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
            {content.story && (
              <button
                className={`btn ${activeTab === 'story' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setActiveTab('story')}
                style={{ fontSize: '0.85rem' }}
              >📜 Hikâye</button>
            )}
            {content.scenario && (
              <button
                className={`btn ${activeTab === 'scenario' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setActiveTab('scenario')}
                style={{ fontSize: '0.85rem' }}
              >🎬 Senaryo</button>
            )}
            {content.storyboard && (
              <button
                className={`btn ${activeTab === 'storyboard' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setActiveTab('storyboard')}
                style={{ fontSize: '0.85rem' }}
              >🎨 Storyboard</button>
            )}
          </div>

          {/* TTS Button */}
          {content.storyboard && (
            <button
              className="btn btn-primary"
              disabled={ttsLoading}
              onClick={async () => {
                if (!project) return
                setTtsLoading(true)
                try {
                  const res = await turkuApi.generateTts(project.id)
                  alert(`🎙️ TTS Tamamlandı!\n✅ Başarılı: ${res.data?.successCount || 0} sahne\n❌ Hata: ${res.data?.failureCount || 0} sahne`)
                } catch (e: any) {
                  alert('TTS hatası: ' + (e.message || 'Bilinmeyen hata'))
                } finally {
                  setTtsLoading(false)
                }
              }}
              style={{ fontSize: '0.85rem', marginBottom: '20px' }}
            >
              {ttsLoading ? '⏳ Seslendirme üretiliyor...' : '🎙️ Tüm Sahneleri Seslendir (TTS)'}
            </button>
          )}

          {/* Story Tab */}
          {activeTab === 'story' && content.story && (
            <div style={{ background: 'rgba(255,255,255,0.03)', borderRadius: '12px', padding: '20px', border: '1px solid rgba(255,255,255,0.08)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ color: '#f59e0b', margin: 0 }}>📜 Hikâye Araştırması</h3>
                <span style={{ fontSize: '0.75rem', padding: '4px 10px', borderRadius: '20px', background: 'rgba(245,158,11,0.15)', color: '#f59e0b' }}>
                  Güvenilirlik: {content.story.sourceConfidence}
                </span>
              </div>
              <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.8, color: 'rgba(255,255,255,0.85)', fontSize: '0.95rem' }}>
                {content.story.storySummary}
              </div>
              {content.story.emotionalTheme && (
                <div style={{ marginTop: '16px', padding: '12px', borderRadius: '8px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.15)' }}>
                  <strong style={{ color: '#ef4444' }}>🎭 Duygusal Tema:</strong>
                  <span style={{ marginLeft: '8px', color: 'rgba(255,255,255,0.7)' }}>{content.story.emotionalTheme}</span>
                </div>
              )}
            </div>
          )}

          {/* Scenario Tab */}
          {activeTab === 'scenario' && content.scenario && (
            <div style={{ background: 'rgba(255,255,255,0.03)', borderRadius: '12px', padding: '20px', border: '1px solid rgba(255,255,255,0.08)' }}>
              <h3 style={{ color: '#6366f1', marginBottom: '12px' }}>🎬 {content.scenario.title}</h3>
              <div style={{ whiteSpace: 'pre-wrap', lineHeight: 1.8, color: 'rgba(255,255,255,0.85)', fontSize: '0.95rem' }}>
                {content.scenario.logline}
              </div>
              {content.scenario.scenarioBeats && content.scenario.scenarioBeats.length > 0 && (
                <div style={{ marginTop: '16px' }}>
                  <h4 style={{ color: '#818cf8', marginBottom: '8px' }}>Beat'ler:</h4>
                  {content.scenario.scenarioBeats.map((beat: string, i: number) => (
                    <div key={i} style={{ padding: '8px 12px', marginBottom: '4px', borderRadius: '6px', background: 'rgba(99,102,241,0.08)', border: '1px solid rgba(99,102,241,0.12)', color: 'rgba(255,255,255,0.75)', fontSize: '0.9rem' }}>
                      <strong>Beat {i+1}:</strong> {beat}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Storyboard Tab */}
          {activeTab === 'storyboard' && content.storyboard && (
            <div style={{ background: 'rgba(255,255,255,0.03)', borderRadius: '12px', padding: '20px', border: '1px solid rgba(255,255,255,0.08)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                <h3 style={{ color: '#10b981', margin: 0 }}>🎨 Storyboard ({content.storyboard.scenes?.length || 0} sahne)</h3>
                <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.5)' }}>
                  Toplam: {Math.floor((content.storyboard.totalDurationSeconds || 0) / 60)} dk
                </span>
              </div>
              {content.storyboard.scenes?.map((scene: any) => (
                <div key={scene.sceneNo} style={{ marginBottom: '16px', padding: '16px', borderRadius: '10px', background: 'rgba(16,185,129,0.05)', border: '1px solid rgba(16,185,129,0.12)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <strong style={{ color: '#10b981' }}>Sahne {scene.sceneNo}: {scene.sceneTitle}</strong>
                    <span style={{ fontSize: '0.8rem', color: 'rgba(255,255,255,0.4)' }}>{scene.durationSeconds}sn</span>
                  </div>

                  {/* Sahne Görseli */}
                  {project && (
                    <div style={{ marginBottom: '10px' }}>
                      <img
                        src={turkuApi.getSceneImageUrl(project.id, scene.sceneNo)}
                        alt={`Sahne ${scene.sceneNo}`}
                        style={{ maxWidth: '100%', borderRadius: '8px', border: '1px solid rgba(16,185,129,0.2)' }}
                        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none' }}
                      />
                    </div>
                  )}

                  {scene.narrationSummary && (
                    <div style={{ marginBottom: '6px', fontSize: '0.9rem', color: 'rgba(255,255,255,0.8)' }}>
                      🎙️ <em>{scene.narrationSummary}</em>
                    </div>
                  )}

                  {/* Audio Player */}
                  {project && (
                    <div style={{ marginBottom: '8px' }}>
                      <audio
                        controls
                        src={turkuApi.getSceneAudioUrl(project.id, scene.sceneNo)}
                        style={{ width: '100%', height: '36px', borderRadius: '8px' }}
                        onError={(e) => { (e.target as HTMLAudioElement).style.display = 'none' }}
                      />
                    </div>
                  )}

                  {scene.visualDescription && (
                    <div style={{ marginBottom: '6px', fontSize: '0.85rem', color: 'rgba(255,255,255,0.6)' }}>
                      🖼️ {scene.visualDescription}
                    </div>
                  )}
                  <div style={{ display: 'flex', gap: '12px', fontSize: '0.75rem', color: 'rgba(255,255,255,0.4)', flexWrap: 'wrap' }}>
                    {scene.mood && <span>🎭 {scene.mood}</span>}
                    {scene.cameraDirection && <span>📷 {scene.cameraDirection}</span>}
                    {scene.transitionHint && <span>↔️ {scene.transitionHint}</span>}
                    {scene.musicNote && <span>🎵 {scene.musicNote}</span>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── Müzik Yükleme ── */}
      {project && (
        <MusicUploadSection projectId={project.id} />
      )}

      {/* ── Video Üretimi ── */}
      {project && (
        <div className="card" style={{ marginTop: '24px' }}>
          <h2 style={{ fontSize: '1.4rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '10px' }}>
            🎬 Video
          </h2>

          <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap' }}>
            {/* TAM PIPELINE BUTONU */}
            <button
              className="btn btn-primary"
              disabled={videoAssembling}
              onClick={async () => {
                setVideoAssembling(true)
                setVideoReady(false)
                setAutoRunProgress('⏳ Tam video pipeline başlatılıyor...')
                try {
                  const res = await turkuApi.generateFullVideo(project.id)
                  if (res.data?.success || res.data?.videoUrl) {
                    setVideoReady(true)
                    const steps = [
                      `📜 İçerik: ${res.data.step1_content}`,
                      `🖼️ Görseller: ${res.data.step2_images}`,
                      res.data.step2_5_video ? `🎥 Kling: ${res.data.step2_5_video}` : '',
                      `🎙️ Ses: ${res.data.step3_tts}`,
                      `🎬 Video: ${res.data.step4_video}`,
                    ].filter(Boolean).join('\n')
                    alert(`🎬 Video hazır!\n\n${steps}`)
                    await refreshProject(project.id)
                  } else {
                    alert('Pipeline hatası: ' + (res.data?.error || 'Bilinmeyen hata'))
                  }
                } catch (e: any) {
                  // Timeout olsa bile video oluşmuş olabilir
                  try {
                    const videoUrl = turkuApi.getVideoUrl(project.id)
                    const checkRes = await fetch(videoUrl, { method: 'GET', headers: { Range: 'bytes=0-0' } })
                    if (checkRes.ok || checkRes.status === 206) {
                      setVideoReady(true)
                      alert('🎬 Video hazır! (Pipeline yanıt zaman aşımına uğradı ama video oluşturuldu)')
                      await refreshProject(project.id)
                      return
                    }
                  } catch { /* */ }
                  alert('Pipeline hatası: ' + (e.message || 'Bilinmeyen hata'))
                } finally {
                  setVideoAssembling(false)
                  setAutoRunProgress('')
                }
              }}
              style={{ background: 'linear-gradient(135deg, #f59e0b, #ef4444, #8b5cf6)', border: 'none', fontSize: '1rem', padding: '10px 24px' }}
            >
              {videoAssembling ? '⏳ Video üretiliyor (GPT-4o → DALL-E → TTS → FFmpeg)...' : '🚀 Tam Video Üret'}
            </button>

            {/* Sadece FFmpeg butonu */}
            <button
              className="btn btn-secondary"
              disabled={videoAssembling}
              onClick={async () => {
                setVideoAssembling(true)
                setVideoReady(false)
                try {
                  const res = await turkuApi.assembleVideo(project.id)
                  if (res.data?.success) {
                    setVideoReady(true)
                  } else {
                    alert('Video birleştirme hatası: ' + (res.data?.error || 'Bilinmeyen hata'))
                  }
                } catch (e: any) {
                  alert('Video birleştirme hatası: ' + (e.message || 'Bilinmeyen hata'))
                } finally {
                  setVideoAssembling(false)
                }
              }}
              style={{ fontSize: '0.85rem' }}
            >
              🎬 Sadece FFmpeg Birleştir
            </button>
          </div>

          {/* Video Player */}
          {videoReady && (
            <div style={{ borderRadius: '12px', overflow: 'hidden', border: '2px solid rgba(99,102,241,0.3)', marginTop: '12px' }}>
              <video
                key={project.id + '-' + Date.now()}
                controls
                src={turkuApi.getVideoUrl(project.id) + '?t=' + Date.now()}
                style={{ width: '100%', maxHeight: '600px', background: '#000' }}
              >
                Tarayıcınız video oynatmayı desteklemiyor.
              </video>
              <div style={{ padding: '12px', background: 'rgba(99,102,241,0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ color: 'rgba(255,255,255,0.7)', fontSize: '0.85rem' }}>🎬 {project.turkuName || 'Türkü'} — Belgesel Videosu</span>
                <a
                  href={turkuApi.getVideoUrl(project.id)}
                  download={`${project.turkuName || 'turku'}-video.mp4`}
                  className="btn btn-secondary"
                  style={{ fontSize: '0.8rem', padding: '4px 12px' }}
                >
                  ⬇️ İndir
                </a>
              </div>
            </div>
          )}

          {!videoReady && !videoAssembling && (
            <p style={{ color: 'rgba(255,255,255,0.4)', fontSize: '0.85rem' }}>
              🚀 "Tam Video Üret" butonu tek tıkla tüm pipeline'ı çalıştırır: OpenAI hikâye → DALL-E görseller → TTS ses → FFmpeg video
            </p>
          )}
        </div>
      )}

      {/* ── Stage Actions ─────────────────────── */}
      {project && (
        <div className="card" data-testid="stage-actions">
          <div className="card-header">
            <h2 className="card-title">⚡ Hazırlık Aksiyonları</h2>
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <span className="badge badge-info">Dahili Üretim</span>
              <button
                className="btn btn-primary"
                disabled={!!activeAction || autoRunning}
                onClick={handleRunAll}
                style={{ fontSize: '0.8rem', padding: '6px 16px', background: 'linear-gradient(135deg, #f59e0b, #ef4444)', border: 'none' }}
              >
                {autoRunning ? `${autoRunProgress}` : '🚀 Tümünü Otomatik Çalıştır'}
              </button>
            </div>
          </div>
          <div className="card-body">
            <div className="action-buttons">
              {stageActions.map((sa) => (
                <button
                  key={sa.label}
                  className={`btn ${project.workflowStage === sa.enabledAt ? 'btn-primary' : 'btn-secondary'}`}
                  disabled={!!activeAction || project.workflowStage !== sa.enabledAt}
                  onClick={() => handleStageAction(sa)}
                  data-testid={`btn-${sa.enabledAt}`}
                  style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
                >
                  {activeAction === sa.label && (
                    <span style={{
                      width: '16px', height: '16px',
                      border: '2px solid rgba(255,255,255,0.3)',
                      borderTopColor: '#fff',
                      borderRadius: '50%',
                      animation: 'spin 0.8s linear infinite',
                      display: 'inline-block',
                    }} />
                  )}
                  {activeAction === sa.label ? `${sa.label}...` : sa.label}
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
                disabled={!!activeAction || !safety.canGenerateStory}
                onClick={() => handleGenerateAction('Hikâye Üret', turkuApi.generateTurkuStory)}
                data-testid="btn-generate-story"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
              >
                {activeAction === 'Hikâye Üret' && (
                  <span style={{
                    width: '16px', height: '16px',
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                    display: 'inline-block',
                  }} />
                )}
                {activeAction === 'Hikâye Üret' ? '📝 Hikâye Üretiliyor...' : '📝 Hikâye Üret'}
              </button>
              <button
                className="btn btn-primary"
                disabled={!!activeAction || !safety.canGenerateScenario}
                onClick={() => handleGenerateAction('Senaryo Üret', turkuApi.generateTurkuScenario)}
                data-testid="btn-generate-scenario"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
              >
                {activeAction === 'Senaryo Üret' && (
                  <span style={{
                    width: '16px', height: '16px',
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                    display: 'inline-block',
                  }} />
                )}
                {activeAction === 'Senaryo Üret' ? '🎬 Senaryo Üretiliyor...' : '🎬 Senaryo Üret'}
              </button>
              <button
                className="btn btn-primary"
                disabled={!!activeAction || !safety.canGenerateStoryboard}
                onClick={() => handleGenerateAction('Storyboard Üret', turkuApi.generateTurkuStoryboard)}
                data-testid="btn-generate-storyboard"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
              >
                {activeAction === 'Storyboard Üret' && (
                  <span style={{
                    width: '16px', height: '16px',
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                    display: 'inline-block',
                  }} />
                )}
                {activeAction === 'Storyboard Üret' ? '🎨 Storyboard Üretiliyor...' : '🎨 Storyboard Üret'}
              </button>
              <button
                className="btn btn-accent"
                disabled={!!activeAction || !safety.canGeneratePlanningBundle}
                onClick={() => handleGenerateAction('Planlama Paketi Üret', turkuApi.generateTurkuPlanningBundle)}
                data-testid="btn-generate-planning-bundle"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
              >
                {activeAction === 'Planlama Paketi Üret' && (
                  <span style={{
                    width: '16px', height: '16px',
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                    display: 'inline-block',
                  }} />
                )}
                {activeAction === 'Planlama Paketi Üret' ? '📦 Paket Üretiliyor...' : '📦 Planlama Paketi Üret'}
              </button>
              <button
                className="btn btn-primary"
                disabled={!!activeAction || !safety.canRunImageBatch}
                onClick={() => handleGenerateAction('Görsel Batch Çalıştır', turkuApi.runTurkuImageBatch)}
                data-testid="btn-run-image-batch"
                style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
              >
                {activeAction === 'Görsel Batch Çalıştır' && (
                  <span style={{
                    width: '16px', height: '16px',
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: '#fff',
                    borderRadius: '50%',
                    animation: 'spin 0.8s linear infinite',
                    display: 'inline-block',
                  }} />
                )}
                {activeAction === 'Görsel Batch Çalıştır' ? '🖼️ Batch Çalışıyor...' : '🖼️ Görsel Batch Çalıştır'}
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

      {/* ── FP-029 Task 02: Trial ── */}
      {project && (
        <div className="card" data-testid="fp029-trial-section">
          <h2>🧪 İlk Dahili Video Denemesi (FP-029)</h2>
          <div className="preview-section" data-testid="fp029-trial-panel">
            <div className="detail-grid">
              <span className="detail-label">Deneme durumu:</span>
              <span className="detail-value" data-testid="fp029-trial-status">NOT_ATTEMPTED</span>
              <span className="detail-label">Neden:</span>
              <span className="detail-value" data-testid="fp029-trial-reason">Henüz denenmedi</span>
              <span className="detail-label">Kapı durumu:</span>
              <span className="detail-value" data-testid="fp029-trial-gate">Kapalı</span>
              <span className="detail-label">Doğrulama adayı:</span>
              <span className="detail-value" data-testid="fp029-trial-candidate">Bekliyor</span>
            </div>
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp029-trial-internal">Sadece dahili deneme</span>
              <span className="safety-flag" data-testid="fp029-trial-no-publish">Yayın yok</span>
              <span className="safety-flag" data-testid="fp029-trial-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp029-trial-simulated">Simüle edilmiş / atlanmış</span>
            </div>
          </div>
        </div>
      )}

      {/* ── FP-030: Real Local/Dev/Test FFmpeg Smoke Run ── */}
      {project && (
        <div className="card" data-testid="fp030-smokerun-section">
          <h2>⚡ Gerçek Local/Dev/Test FFmpeg Denemesi (FP-030)</h2>
          <div className="preview-section" data-testid="fp030-smokerun-panel">
            <div className="detail-grid">
              <span className="detail-label">Smoke run durumu:</span>
              <span className="detail-value" data-testid="fp030-smokerun-status">EXECUTION_SKIPPED_DISABLED</span>
              <span className="detail-label">Process runner:</span>
              <span className="detail-value" data-testid="fp030-process-enabled">Kapalı (varsayılan)</span>
              <span className="detail-label">Doğrulama adayı:</span>
              <span className="detail-value" data-testid="fp030-candidate">Bekliyor</span>
            </div>
            <div className="safety-flags">
              <span className="safety-flag" data-testid="fp030-label-local-only">Sadece LOCAL/DEV/TEST</span>
              <span className="safety-flag" data-testid="fp030-label-disabled-default">Varsayılan kapalı</span>
              <span className="safety-flag" data-testid="fp030-label-no-publish">Yayın/dışa aktarım yok</span>
              <span className="safety-flag" data-testid="fp030-label-no-player">Video oynatıcı yok</span>
              <span className="safety-flag" data-testid="fp030-label-no-command">Komut/dosya yolu gösterilmez</span>
            </div>
          </div>
        </div>
      )}
      {/* ── New project button ────────────────── */}
      {project && (
        <div className="form-actions">
          <button
            className="btn btn-ghost"
            onClick={() => { setProject(null); setProgress(null); setSafety(null); setCompositionPreview(null); setReadiness(null); setRenderJob(null); setRenderExecution(null); setExecutionSafety(null); setForm({ turkuName: '' }); setError(null); setActiveAction(null); setSuccessMessage(null) }}
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
