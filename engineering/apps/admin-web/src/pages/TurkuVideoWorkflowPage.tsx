import { useState, useCallback, useEffect } from 'react'
import type {
  TurkuVideoBootstrapRequest,
  TurkuVideoProjectResponse,
} from '../types'
import * as turkuApi from '../api/turkuVideoApi'

// ── Mevcut Proje Listesi ──
function ExistingProjectsList({ onSelect }: { onSelect: (id: string) => void }) {
  const [projects, setProjects] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    turkuApi.listTurkuVideoProjects()
      .then(res => setProjects(res.data || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <div style={{ textAlign: 'center', padding: '16px', color: '#94a3b8' }}>Projeler yükleniyor...</div>
  if (projects.length === 0) return null

  const stageColors: Record<string, string> = {
    'DRAFT_CREATED': '#64748b',
    'STORY_CONTEXT_PREPARED': '#f59e0b',
    'SCENARIO_PREPARED': '#f59e0b',
    'STORYBOARD_PREPARED': '#10b981',
    'SCENE_ASSETS_PREPARED': '#3b82f6',
    'IMAGE_BATCH_PREPARED': '#3b82f6',
    'IMAGE_BATCH_COMPLETED': '#8b5cf6',
    'TTS_COMPLETED': '#a855f7',
    'VIDEO_READY': '#ef4444',
  }

  return (
    <div className="card" style={{ marginTop: '16px' }}>
      <div className="card-body">
        <h3 style={{ margin: '0 0 12px 0', fontSize: '1rem', color: '#94a3b8' }}>📂 Mevcut Projeler ({projects.length})</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {projects.map((p: any) => (
            <div
              key={p.id}
              onClick={() => onSelect(p.id)}
              style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                background: '#0f172a', padding: '12px 16px', borderRadius: '8px',
                cursor: 'pointer', border: '1px solid #1e293b',
                transition: 'border-color 0.2s',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#3b82f6')}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = '#1e293b')}
            >
              <div>
                <strong style={{ color: '#e2e8f0' }}>🎵 {p.turkuName}</strong>
                <span style={{ marginLeft: '12px', color: '#64748b', fontSize: '0.75rem', fontFamily: 'monospace' }}>{p.id.substring(0, 8)}...</span>
              </div>
              <span style={{
                padding: '2px 10px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 'bold',
                background: (stageColors[p.workflowStage] || '#334155') + '22',
                color: stageColors[p.workflowStage] || '#94a3b8',
                border: `1px solid ${stageColors[p.workflowStage] || '#334155'}44`,
              }}>
                {p.workflowStage}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

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

function TurkuVideoWorkflowPage() {
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

  const [project, setProject] = useState<TurkuVideoProjectResponse | null>(null)
  const [content, setContent] = useState<any>(null)
  const [assetList, setAssetList] = useState<any[]>([])
  
  const [currentStep, setCurrentStep] = useState<number>(1)
  const [completedSteps, setCompletedSteps] = useState<Set<number>>(new Set())
  
  const [videoReady, setVideoReady] = useState(false)
  const [activeVideoMode, setActiveVideoMode] = useState<'' | 'preview' | 'full' | 'fullKling'>('')
  const [storyTab, setStoryTab] = useState<'research'|'draft'|'final'>('final')

  const refreshProject = useCallback(async (id: string) => {
    try {
      const projRes = await turkuApi.getTurkuVideoProject(id)
      setProject(projRes.data)
      localStorage.setItem('turkuActiveProjectId', id)

      const completed = new Set<number>()

      // Content kontrolü (hikâye, storyboard)
      try {
        const contentRes = await turkuApi.getTurkuVideoProjectContent(id)
        setContent(contentRes.data)
        
        const storyPayload = contentRes.data?.storyPayload || contentRes.data?.story
        const storyboardPayload = contentRes.data?.storyboardPayload || contentRes.data?.storyboard
        if (storyPayload) completed.add(1)
        if (storyboardPayload?.scenes?.length > 0) completed.add(2)
      } catch {}

      // Asset kontrolü (görseller, sesler)
      try {
        const assetsRes = await turkuApi.listAssets(id)
        const d = assetsRes.data || {}
        const flat: any[] = []
        if (d.images?.items) d.images.items.forEach((i: any) => flat.push({ ...i, type: 'image' }))
        if (d.videos?.items) d.videos.items.forEach((i: any) => flat.push({ ...i, type: 'video' }))
        if (d.audio?.items) d.audio.items.forEach((i: any) => flat.push({ ...i, type: 'audio' }))
        setAssetList(flat)
        
        if (flat.some(a => a.type === 'image')) completed.add(3)
        if (flat.some(a => a.type === 'audio')) completed.add(4)
      } catch {}

      // Video kontrolü
      try {
        const videoUrl = turkuApi.getVideoUrl(id)
        const checkRes = await fetch(videoUrl, { method: 'GET', headers: { Range: 'bytes=0-0' } })
        if (checkRes.ok || checkRes.status === 206) {
          setVideoReady(true)
          completed.add(5)
        }
      } catch {}

      // State'i güncelle
      setCompletedSteps(completed)

      // İlk tamamlanmamış adıma git
      if (completed.size > 0) {
        let nextStep = 1
        for (let i = 1; i <= 5; i++) {
          if (!completed.has(i)) { nextStep = i; break }
          if (i === 5) nextStep = 5
        }
        setCurrentStep(nextStep)
      }
    } catch (err) {
      console.error('refreshProject failed:', err)
      // Sadece 404 ise temizle, geçici hatada silme
    }
  }, [])

  useEffect(() => {
    const savedId = localStorage.getItem('turkuActiveProjectId')
    console.log('🔄 Sayfa yüklendi, localStorage turkuActiveProjectId:', savedId)
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
    } catch (e: any) {
      setError(e.message || 'Bir hata oluştu')
    } finally {
      setLoading(false)
    }
  }

  const markStepCompleted = (step: number) => {
    setCompletedSteps(prev => new Set(prev).add(step))
    if (currentStep === step && currentStep < 5) {
      setCurrentStep(step + 1)
    }
  }

  const renderStepIndicator = () => {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '32px', marginTop: '16px' }}>
        {[1, 2, 3, 4, 5].map((step, index) => {
          const isCompleted = completedSteps.has(step)
          const isActive = currentStep === step
          const isLocked = !isCompleted && step > 1 && !completedSteps.has(step - 1)
          
          let bgColor = '#334155' // locked (gray)
          if (isCompleted) bgColor = '#10b981' // completed (green)
          if (isActive) bgColor = '#f59e0b' // active (amber)

          return (
            <div key={step} style={{ display: 'flex', alignItems: 'center' }}>
              <div 
                onClick={() => { if (!isLocked) setCurrentStep(step) }}
                style={{ 
                  width: '36px', height: '36px', borderRadius: '50%', 
                  background: bgColor, color: 'white', 
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  cursor: isLocked ? 'not-allowed' : 'pointer',
                  fontWeight: 'bold', fontSize: '1.1rem',
                  border: isActive ? '3px solid white' : 'none',
                  boxShadow: isActive ? '0 0 10px rgba(245,158,11,0.5)' : 'none',
                  zIndex: 2
                }}
              >
                {step}
              </div>
              {index < 4 && (
                <div style={{ width: '60px', height: '4px', background: isCompleted ? '#10b981' : '#334155', marginLeft: '-2px', marginRight: '-2px', zIndex: 1 }} />
              )}
            </div>
          )
        })}
      </div>
    )
  }

  return (
    <div className="page-content" style={{ maxWidth: '1200px', margin: '0 auto', padding: '20px' }}>
      <div className="page-header">
        <h1 className="page-title">🎵 Türkü Video İş Akışı</h1>
        <p className="page-subtitle">Dahili üretim iş akışı — Yayın/dışa aktarım yok</p>
      </div>

      {error && (
        <div className="alert alert-error" role="alert" style={{ marginBottom: '16px' }}>
          <span className="alert-icon">⚠️</span><span>{error}</span>
        </div>
      )}
      {successMessage && (
        <div className="alert alert-success" role="status" style={{ marginBottom: '16px' }}>
          <span>{successMessage}</span>
        </div>
      )}

      {/* Bootstrap Form */}
      {!project && (
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Yeni Türkü Projesi Başlat</h2>
          </div>
          <div className="card-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label" htmlFor="turkuName">Türkü Adı *</label>
                <input id="turkuName" className="form-input" type="text" value={form.turkuName} onChange={(e) => setForm({ ...form, turkuName: e.target.value })} required />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="region">Bölge</label>
                <input id="region" className="form-input" type="text" value={form.region || ''} onChange={(e) => setForm({ ...form, region: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="mood">Atmosfer</label>
                <input id="mood" className="form-input" type="text" value={form.mood || ''} onChange={(e) => setForm({ ...form, mood: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="targetDuration">Hedef Süre (dk)</label>
                <input id="targetDuration" className="form-input" type="number" min={1} max={30} value={form.targetDurationMinutes || ''} onChange={(e) => setForm({ ...form, targetDurationMinutes: Number(e.target.value) })} />
              </div>
              <div className="form-group form-group-full">
                <label className="form-label" htmlFor="sourceNotes">Kaynak Notları</label>
                <textarea id="sourceNotes" className="form-input" rows={2} value={form.sourceNotes || ''} onChange={(e) => setForm({ ...form, sourceNotes: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="narrationStyle">Anlatım Tarzı</label>
                <input id="narrationStyle" className="form-input" type="text" value={form.narrationStyle || ''} onChange={(e) => setForm({ ...form, narrationStyle: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="visualStyle">Görsel Tarz</label>
                <input id="visualStyle" className="form-input" type="text" value={form.visualStyle || ''} onChange={(e) => setForm({ ...form, visualStyle: e.target.value })} />
              </div>
            </div>
            <div className="form-actions" style={{ marginTop: '20px' }}>
              <button className="btn btn-primary" onClick={handleBootstrap} disabled={loading || !form.turkuName.trim()}>
                {loading ? 'Başlatılıyor…' : '🎬 Projeyi Başlat'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Mevcut Projeler */}
      {!project && (
        <ExistingProjectsList onSelect={(id: string) => refreshProject(id)} />
      )}

      {/* Project Summary */}
      {project && (
        <div className="card" style={{ marginBottom: '24px' }}>
          <div className="card-header">
            <h2 className="card-title">📋 Proje Özeti</h2>
            <span className="badge badge-info">{project.id}</span>
          </div>
          <div className="card-body">
            <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
              <div><strong>Türkü:</strong> {project.turkuName}</div>
              <div><strong>Aşama:</strong> {project.workflowStage}</div>
            </div>
          </div>
        </div>
      )}

      {/* Wizard */}
      {project && (
        <div className="wizard-container" style={{ background: '#1e293b', borderRadius: '12px', padding: '24px', border: '1px solid #334155' }}>
          {renderStepIndicator()}

          {/* STEP 1: Hikâye Üret */}
          {currentStep === 1 && (
            <div className="step-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0, color: '#f59e0b' }}>📜 Adım 1: Hikâye Üret (3 Aşamalı)</h2>
                <button 
                  className="btn btn-primary" 
                  disabled={!!activeAction}
                  onClick={async () => {
                    setActiveAction('Hikâye Pipeline Çalışıyor')
                    try {
                      await turkuApi.generateTurkuStoryPipeline(project.id)
                      await refreshProject(project.id)
                      setSuccessMessage('3 aşamalı hikâye pipeline tamamlandı!')
                      setTimeout(() => setSuccessMessage(null), 5000)
                    } catch (e: any) {
                      setError(e.message)
                    } finally {
                      setActiveAction(null)
                    }
                  }}
                >
                  {activeAction === 'Hikâye Pipeline Çalışıyor' ? '⏳ 1.Araştırma → 2.Taslak → 3.Editör...' : '🚀 Hikâye Üret (3 Aşamalı Pipeline)'}
                </button>
              </div>

              {content && (content.storyPayload || content.story) && (() => {
                const storyData = content.storyPayload || content.story
                return (
                  <div style={{ background: '#0f172a', padding: '20px', borderRadius: '8px' }}>
                    {/* Tab Bar */}
                    <div style={{ display: 'flex', gap: '4px', marginBottom: '16px', borderBottom: '1px solid #334155', paddingBottom: '8px' }}>
                      {[
                        { key: 'research' as const, label: '📚 Araştırma', color: '#8b5cf6' },
                        { key: 'draft' as const, label: '📝 Taslak', color: '#f59e0b' },
                        { key: 'final' as const, label: '✅ Nihai Hikâye', color: '#10b981' },
                      ].map(tab => (
                        <button
                          key={tab.key}
                          onClick={() => setStoryTab(tab.key)}
                          style={{
                            padding: '8px 16px',
                            borderRadius: '6px 6px 0 0',
                            border: 'none',
                            cursor: 'pointer',
                            fontWeight: storyTab === tab.key ? 700 : 400,
                            background: storyTab === tab.key ? tab.color + '22' : 'transparent',
                            color: storyTab === tab.key ? tab.color : '#94a3b8',
                            borderBottom: storyTab === tab.key ? `2px solid ${tab.color}` : '2px solid transparent',
                          }}
                        >
                          {tab.label}
                        </button>
                      ))}
                    </div>

                    {/* Tab Content */}
                    {storyTab === 'research' && (
                      <div>
                        <h4 style={{ color: '#8b5cf6', marginTop: 0 }}>📚 Kaynaklı Araştırma Raporu</h4>
                        <p style={{ whiteSpace: 'pre-wrap', color: '#cbd5e1', lineHeight: '1.6', maxHeight: '500px', overflowY: 'auto' }}>
                          {storyData.researchReport || 'Araştırma raporu henüz üretilmedi. Pipeline çalıştırın.'}
                        </p>
                      </div>
                    )}

                    {storyTab === 'draft' && (
                      <div>
                        <h4 style={{ color: '#f59e0b', marginTop: 0 }}>📝 Hikâye Taslağı</h4>
                        <p style={{ whiteSpace: 'pre-wrap', color: '#cbd5e1', lineHeight: '1.6', maxHeight: '500px', overflowY: 'auto' }}>
                          {storyData.storyDraft || 'Hikâye taslağı henüz üretilmedi. Pipeline çalıştırın.'}
                        </p>
                      </div>
                    )}

                    {storyTab === 'final' && (
                      <div>
                        {storyData.qualityStatus && (
                          <div style={{ background: '#064e3b', padding: '12px', borderRadius: '6px', marginBottom: '12px', fontSize: '13px' }}>
                            <strong style={{ color: '#34d399' }}>Editör Kontrolü:</strong>
                            <pre style={{ whiteSpace: 'pre-wrap', color: '#a7f3d0', margin: '8px 0 0', fontSize: '12px' }}>{storyData.qualityStatus}</pre>
                          </div>
                        )}
                        <h4 style={{ color: '#10b981', marginTop: 0 }}>✅ Nihai Hikâye (Editörden Geçmiş)</h4>
                        <p style={{ whiteSpace: 'pre-wrap', color: '#cbd5e1', lineHeight: '1.6', maxHeight: '500px', overflowY: 'auto' }}>
                          {storyData.finalStory || storyData.storySummary || storyData.summary || 'Nihai hikâye henüz üretilmedi.'}
                        </p>
                      </div>
                    )}
                    
                    <div style={{ marginTop: '24px', textAlign: 'right' }}>
                      <button className="btn btn-success" onClick={() => markStepCompleted(1)} style={{ background: '#10b981', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '8px', cursor: 'pointer' }}>
                        ✅ Beğendim, Sonraki Adım
                      </button>
                    </div>
                  </div>
                )
              })()}
            </div>
          )}

          {/* STEP 2: Storyboard Üret */}
          {currentStep === 2 && (
            <div className="step-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0, color: '#10b981' }}>🎨 Adım 2: Storyboard Üret</h2>
                <button 
                  className="btn btn-primary" 
                  disabled={!!activeAction}
                  onClick={async () => {
                    setActiveAction('Storyboard Üretiyor')
                    try {
                      // Backend state machine: önce senaryo → sonra storyboard
                      setSuccessMessage('⏳ Senaryo hazırlanıyor...')
                      await turkuApi.generateTurkuScenario(project.id)
                      setSuccessMessage('⏳ Storyboard üretiliyor...')
                      await turkuApi.generateTurkuStoryboard(project.id)
                      await refreshProject(project.id)
                      setSuccessMessage('Storyboard başarıyla üretildi!')
                      setTimeout(() => setSuccessMessage(null), 3000)
                    } catch (e: any) {
                      setError(e.message)
                    } finally {
                      setActiveAction(null)
                    }
                  }}
                >
                  {activeAction === 'Storyboard Üretiyor' ? '⏳ Üretiliyor...' : '⚙️ Storyboard Üret (AI)'}
                </button>
              </div>

              {content && (content.storyboardPayload || content.storyboard) && (
                <div style={{ display: 'grid', gap: '16px' }}>
                  {(() => {
                    const sbData = content.storyboardPayload || content.storyboard
                    return (
                      <>
                        {sbData.scenes?.map((scene: any) => (
                          <div key={scene.sceneNo} style={{ background: '#0f172a', padding: '16px', borderRadius: '8px', borderLeft: '4px solid #10b981' }}>
                            <h4 style={{ margin: '0 0 12px 0', color: '#10b981' }}>Sahne {scene.sceneNo}: {scene.sceneTitle}</h4>
                            <div style={{ marginBottom: '8px', color: '#e2e8f0' }}><strong>🎙️ Anlatım:</strong> {scene.narrationSummary}</div>
                            <div style={{ marginBottom: '8px', color: '#cbd5e1' }}><strong>🖼️ Görsel:</strong> {scene.visualDescription}</div>
                            <div style={{ color: '#94a3b8', fontSize: '0.9rem' }}><strong>🎭 Mood:</strong> {scene.mood}</div>
                          </div>
                        ))}
                        <div style={{ marginTop: '16px', textAlign: 'right' }}>
                          <button className="btn btn-success" onClick={() => markStepCompleted(2)} style={{ background: '#10b981', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '8px', cursor: 'pointer' }}>
                            ✅ Beğendim, Sonraki Adım
                          </button>
                        </div>
                      </>
                    )
                  })()}
                </div>
              )}
            </div>
          )}

          {/* STEP 3: Görselleri Üret */}
          {currentStep === 3 && (
            <div className="step-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0, color: '#3b82f6' }}>🖼️ Adım 3: Görselleri Üret</h2>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button 
                    className="btn btn-primary" 
                    disabled={!!activeAction}
                    onClick={async () => {
                      setActiveAction('Görseller Üretiyor')
                      try {
                        // Backend state machine: hazırlık adımları
                        setSuccessMessage('⏳ Varlık planı hazırlanıyor...')
                        await turkuApi.prepareTurkuSceneAssets(project.id)
                        setSuccessMessage('⏳ Görsel batch hazırlanıyor...')
                        await turkuApi.prepareTurkuImageBatch(project.id)
                        setSuccessMessage('⏳ Görseller üretiliyor...')
                        await turkuApi.runTurkuImageBatch(project.id)
                        await refreshProject(project.id)
                        setSuccessMessage('Görseller başarıyla üretildi!')
                        setTimeout(() => setSuccessMessage(null), 3000)
                      } catch (e: any) {
                        // Timeout olsa bile görseller arka planda üretilmiş olabilir
                        if (e.message?.includes('zaman aşımı') || e.message?.includes('timeout') || e.message?.includes('120')) {
                          setError('⚠️ İstek zaman aşımına uğradı (120s). Video üretimi hâlâ devam ediyor olabilir — sayfayı yenileyin.')
                        } else {
                          setError(e.message)
                        }
                        // Her durumda projeyi yenile — kısmi sonuçları göster
                        await refreshProject(project.id)
                      } finally {
                        setActiveAction(null)
                      }
                    }}
                  >
                    {activeAction === 'Görseller Üretiyor' ? '⏳ Üretiliyor...' : '⚙️ Görselleri Üret'}
                  </button>
                  <button 
                    className="btn" 
                    style={{ background: '#7f1d1d', color: 'white', border: 'none', cursor: 'pointer' }}
                    onClick={async () => {
                      if(confirm('Tüm görseller silinecek. Emin misiniz?')) {
                        await turkuApi.deleteAssets(project.id, 'images')
                        await refreshProject(project.id)
                      }
                    }}
                  >
                    🗑️ Tümünü Sil
                  </button>
                </div>
              </div>

              {assetList.filter(a => a.type === 'image').length > 0 && (
                <>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: '16px' }}>
                    {assetList.filter(a => a.type === 'image').sort((a, b) => (a.sceneNo || 0) - (b.sceneNo || 0)).map((asset) => (
                      <div key={asset.path} style={{ background: '#0f172a', borderRadius: '8px', overflow: 'hidden' }}>
                        <img 
                          src={`/api/admin/turku-video-projects/${project.id}/scenes/${asset.sceneNo}/image`}
                          alt={`Sahne ${asset.sceneNo}`}
                          style={{ width: '100%', height: '180px', objectFit: 'cover' }}
                          onError={(e) => { (e.target as HTMLImageElement).src = turkuApi.getAssetPreviewUrl(project.id, 'image', asset.sceneNo) }}
                        />
                        <div style={{ padding: '12px', textAlign: 'center', color: '#cbd5e1' }}>
                          Sahne {asset.sceneNo}
                        </div>
                      </div>
                    ))}
                  </div>
                  <div style={{ marginTop: '24px', textAlign: 'right' }}>
                    <button className="btn btn-success" onClick={() => markStepCompleted(3)} style={{ background: '#10b981', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '8px', cursor: 'pointer' }}>
                      ✅ Beğendim, Sonraki Adım
                    </button>
                  </div>
                </>
              )}
            </div>
          )}

          {/* STEP 4: Seslendirme Üret */}
          {currentStep === 4 && (
            <div className="step-content">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0, color: '#a855f7' }}>🎙️ Adım 4: Seslendirme Üret</h2>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <button 
                    className="btn btn-primary" 
                    disabled={!!activeAction}
                    onClick={async () => {
                      setActiveAction('Seslendirme Üretiyor')
                      try {
                        await turkuApi.generateTts(project.id)
                        await refreshProject(project.id)
                        setSuccessMessage('Seslendirme başarıyla üretildi!')
                        setTimeout(() => setSuccessMessage(null), 3000)
                      } catch (e: any) {
                        setError(e.message)
                      } finally {
                        setActiveAction(null)
                      }
                    }}
                  >
                    {activeAction === 'Seslendirme Üretiyor' ? '⏳ Üretiliyor...' : '⚙️ Seslendirme Üret (TTS)'}
                  </button>
                  <button 
                    className="btn" 
                    style={{ background: '#7f1d1d', color: 'white', border: 'none', cursor: 'pointer' }}
                    onClick={async () => {
                      if(confirm('Tüm ses dosyaları silinecek. Emin misiniz?')) {
                        await turkuApi.deleteAssets(project.id, 'audio')
                        await refreshProject(project.id)
                      }
                    }}
                  >
                    🗑️ Tümünü Sil
                  </button>
                </div>
              </div>

              {assetList.filter(a => a.type === 'audio').length > 0 && (
                <>
                  <div style={{ display: 'grid', gap: '16px' }}>
                    {assetList.filter(a => a.type === 'audio').sort((a, b) => (a.sceneNo || 0) - (b.sceneNo || 0)).map((asset) => (
                      <div key={asset.path} style={{ background: '#0f172a', padding: '16px', borderRadius: '8px', display: 'flex', alignItems: 'center', gap: '16px' }}>
                        <div style={{ fontWeight: 'bold', color: '#a855f7', minWidth: '80px' }}>Sahne {asset.sceneNo}</div>
                        <audio 
                          controls 
                          src={`/api/admin/turku-video-projects/${project.id}/scenes/${asset.sceneNo}/audio`}
                          style={{ flex: 1, height: '40px' }}
                          onError={(e) => { (e.target as HTMLAudioElement).style.display = 'none' }}
                        />
                      </div>
                    ))}
                  </div>
                  <div style={{ marginTop: '24px', textAlign: 'right' }}>
                    <button className="btn btn-success" onClick={() => markStepCompleted(4)} style={{ background: '#10b981', color: 'white', border: 'none', padding: '10px 20px', borderRadius: '8px', cursor: 'pointer' }}>
                      ✅ Beğendim, Sonraki Adım
                    </button>
                  </div>
                </>
              )}
            </div>
          )}

          {/* STEP 5: Video Oluştur */}
          {currentStep === 5 && (
            <div className="step-content">
              <h2 style={{ color: '#ef4444', marginBottom: '20px' }}>🎬 Adım 5: Video Oluştur</h2>
              
              <MusicUploadSection projectId={project.id} />

              <div style={{ marginTop: '32px', display: 'flex', gap: '16px', flexWrap: 'wrap', justifyContent: 'center' }}>
                <button
                  className="btn btn-primary"
                  disabled={activeVideoMode !== ''}
                  onClick={async () => {
                    setActiveVideoMode('preview')
                    try {
                      await turkuApi.generateFullVideo(project.id, { previewMode: true })
                      await refreshProject(project.id)
                    } catch(e: any) {
                      setError(e.message)
                    } finally {
                      setActiveVideoMode('')
                    }
                  }}
                  style={{ background: 'linear-gradient(135deg, #10b981, #3b82f6)', padding: '12px 24px', fontSize: '1.1rem' }}
                >
                  {activeVideoMode === 'preview' ? '⏳ Üretiliyor...' : '🎬 Preview (5 sahne, ~$3)'}
                </button>
                <button
                  className="btn btn-primary"
                  disabled={activeVideoMode !== ''}
                  onClick={async () => {
                    setActiveVideoMode('full')
                    try {
                      await turkuApi.generateFullVideo(project.id, { previewMode: false, includeKling: false })
                      await refreshProject(project.id)
                    } catch(e: any) {
                      setError(e.message)
                    } finally {
                      setActiveVideoMode('')
                    }
                  }}
                  style={{ background: 'linear-gradient(135deg, #3b82f6, #6366f1)', padding: '12px 24px', fontSize: '1.1rem' }}
                >
                  {activeVideoMode === 'full' ? '⏳ Üretiliyor...' : '🎬 Tam Video (~$5)'}
                </button>
                <button
                  className="btn btn-primary"
                  disabled={activeVideoMode !== ''}
                  onClick={async () => {
                    setActiveVideoMode('fullKling')
                    try {
                      await turkuApi.generateFullVideo(project.id, { previewMode: false, includeKling: true })
                      await refreshProject(project.id)
                    } catch(e: any) {
                      setError(e.message)
                    } finally {
                      setActiveVideoMode('')
                    }
                  }}
                  style={{ background: 'linear-gradient(135deg, #f59e0b, #ef4444)', padding: '12px 24px', fontSize: '1.1rem' }}
                >
                  {activeVideoMode === 'fullKling' ? '⏳ Üretiliyor...' : '🚀 Tam Video + Kling (~$15)'}
                </button>
              </div>

              {videoReady && (
                <div style={{ borderRadius: '12px', overflow: 'hidden', border: '2px solid rgba(99,102,241,0.3)', marginTop: '32px' }}>
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
            </div>
          )}
        </div>
      )}

      {/* Yeni Proje Başlat */}
      {project && (
        <div style={{ textAlign: 'center', marginTop: '32px' }}>
          <button
            className="btn"
            style={{ background: '#334155', color: '#94a3b8', border: '1px solid #475569', cursor: 'pointer' }}
            onClick={() => {
              localStorage.removeItem('turkuActiveProjectId')
              setProject(null)
              setContent(null)
              setAssetList([])
              setCurrentStep(1)
              setCompletedSteps(new Set())
              setVideoReady(false)
            }}
          >
            ➕ Yeni Proje Başlat
          </button>
        </div>
      )}
    </div>
  )
}

export default TurkuVideoWorkflowPage
