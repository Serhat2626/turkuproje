import { useState, useEffect } from 'react'
import { researchApi } from '../../api/client'
import LoadingSpinner from '../common/LoadingSpinner'
import EmptyState from '../common/EmptyState'
import { Search, CheckCircle, RefreshCw, Edit3 } from 'lucide-react'

export default function ResearchTab({ projectId, onStatusChange }) {
  const [research, setResearch] = useState(null)
  const [loading, setLoading] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [userNotes, setUserNotes] = useState('')
  const [editing, setEditing] = useState(false)

  useEffect(() => { loadResearch() }, [projectId])

  const loadResearch = async () => {
    setLoading(true)
    try {
      const res = await researchApi.list(projectId)
      if (res.data && res.data.length > 0) { setResearch(res.data[0]); setUserNotes(res.data[0].userNotes || '') }
    } catch (err) { console.error('Araştırma yüklenemedi:', err) }
    finally { setLoading(false) }
  }

  const handleGenerate = async () => {
    setGenerating(true)
    try { const res = await researchApi.generate(projectId); setResearch(res.data); onStatusChange?.() }
    catch (err) { console.error('Araştırma üretilemedi:', err) }
    finally { setGenerating(false) }
  }

  const handleApprove = async () => {
    try { const res = await researchApi.approve(projectId, research.id, { approved: true, userNotes: userNotes || null }); setResearch(res.data); setEditing(false) }
    catch (err) { console.error('Araştırma onaylanamadı:', err) }
  }

  if (loading) return <LoadingSpinner text="Araştırma yükleniyor..." />
  if (!research) return (
    <div className="tab-content">
      <div className="tab-header"><div><h2 className="tab-title">🔍 Araştırma</h2><p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '4px' }}>AI konuyu araştırıp tarihsel ve kültürel bağlamı çıkaracak</p></div></div>
      <EmptyState icon="🔍" title="Henüz araştırma yapılmadı" message="AI ile konu hakkında araştırma başlatın" action={<button className="btn btn-primary btn-lg" onClick={handleGenerate} disabled={generating}>{generating ? <>Araştırılıyor...</> : <><Search size={18} /> Araştırma Başlat</>}</button>} />
    </div>
  )

  return (
    <div className="tab-content">
      <div className="tab-header">
        <div><h2 className="tab-title">🔍 Araştırma</h2><div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}><span className={`status-badge ${research.approved ? 'complete' : 'draft'}`}>{research.approved ? 'Onaylandı' : 'Onay Bekliyor'}</span><span className="version-selector">v{research.version}</span></div></div>
        <div className="tab-actions"><button className="btn btn-secondary btn-sm" onClick={handleGenerate} disabled={generating}><RefreshCw size={14} /> Yeniden Üret</button>{!research.approved && <button className="btn btn-primary btn-sm" onClick={handleApprove}><CheckCircle size={14} /> Onayla</button>}</div>
      </div>
      <div className="content-block glass-card"><h3>Araştırma Sonucu</h3><div className="content-text">{research.content}</div></div>
      <div className="content-block glass-card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}><h3>Notlarım / Düzeltmelerim</h3><button className="btn btn-ghost btn-sm" onClick={() => setEditing(!editing)}><Edit3 size={14} /> {editing ? 'Kapat' : 'Düzenle'}</button></div>
        {editing ? (<div style={{ marginTop: 'var(--space-sm)' }}><textarea className="input" value={userNotes} onChange={(e) => setUserNotes(e.target.value)} placeholder="Düzeltmelerinizi buraya yazın..." rows={4} /><div style={{ marginTop: 'var(--space-sm)', display: 'flex', gap: 'var(--space-sm)' }}><button className="btn btn-primary btn-sm" onClick={handleApprove}>Kaydet & Onayla</button><button className="btn btn-ghost btn-sm" onClick={() => setEditing(false)}>İptal</button></div></div>) : (<p className="content-text" style={{ color: userNotes ? 'var(--text-secondary)' : 'var(--text-muted)' }}>{userNotes || 'Henüz not eklenmedi'}</p>)}
      </div>
    </div>
  )
}
