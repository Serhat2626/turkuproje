import { useState, useEffect } from 'react'
import { scriptApi } from '../../api/client'
import LoadingSpinner from '../common/LoadingSpinner'
import EmptyState from '../common/EmptyState'
import { FileText, RefreshCw } from 'lucide-react'

const SECTION_LABELS = { hook: '🎯 Hook', intro: '📖 Giriş', story: '📜 Hikâye', climax: '💔 Duygusal Zirve', closing: '🎬 Kapanış', callToAction: '📢 Abone Çağrısı' }

export default function ScriptTab({ projectId, onStatusChange }) {
  const [script, setScript] = useState(null)
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)

  useEffect(() => { loadScript() }, [projectId])

  const loadScript = async () => {
    setLoading(true)
    try { const res = await scriptApi.list(projectId); if (res.data && res.data.length > 0) setScript(res.data[0]) }
    catch (err) { console.error('Senaryo yüklenemedi:', err) }
    finally { setLoading(false) }
  }

  const handleGenerate = async () => {
    setGenerating(true)
    try { const res = await scriptApi.generate(projectId); setScript(res.data); onStatusChange?.() }
    catch (err) { console.error('Senaryo üretilemedi:', err) }
    finally { setGenerating(false) }
  }

  if (loading) return <LoadingSpinner text="Senaryo yükleniyor..." />
  if (!script) return (
    <div className="tab-content">
      <div className="tab-header"><div><h2 className="tab-title">📝 Senaryo</h2><p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>YouTube videosu için belgesel tarzında senaryo üretilecek</p></div></div>
      <EmptyState icon="📝" title="Henüz senaryo üretilmedi" message="Araştırma verileri kullanılarak AI ile senaryo oluşturun" action={<button className="btn btn-primary btn-lg" onClick={handleGenerate} disabled={generating}>{generating ? 'Üretiliyor...' : <><FileText size={18} /> Senaryo Üret</>}</button>} />
    </div>
  )

  return (
    <div className="tab-content">
      <div className="tab-header"><div><h2 className="tab-title">📝 Senaryo</h2><span className="version-selector">v{script.version}</span></div><div className="tab-actions"><button className="btn btn-secondary btn-sm" onClick={handleGenerate} disabled={generating}><RefreshCw size={14} /> Yeniden Üret</button></div></div>
      {Object.entries(SECTION_LABELS).map(([key, label]) => script[key] && (<div key={key} className="content-block glass-card"><h3>{label}</h3><div className="content-text">{script[key]}</div></div>))}
      {!script.hook && script.content && <div className="content-block glass-card"><h3>Tam Senaryo</h3><div className="content-text">{script.content}</div></div>}
    </div>
  )
}
