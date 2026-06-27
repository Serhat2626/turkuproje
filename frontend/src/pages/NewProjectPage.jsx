import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectApi } from '../api/client'
import { Sparkles, ArrowLeft } from 'lucide-react'

export default function NewProjectPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({ topic: '', title: '', niche: 'turku_hikayeleri', language: 'tr', targetPlatforms: 'youtube', videoDuration: 8 })
  const handleChange = (e) => { const { name, value } = e.target; setForm(prev => ({ ...prev, [name]: value })) }
  const handleSubmit = async (e) => {
    e.preventDefault(); if (!form.topic.trim()) return; setLoading(true)
    try { const res = await projectApi.create(form); navigate(`/projects/${res.data.id}`) }
    catch (err) { console.error('Proje oluşturulamadı:', err) }
    finally { setLoading(false) }
  }
  return (
    <div>
      <div className="page-header"><div><button className="btn btn-ghost btn-sm" onClick={() => navigate('/')} style={{ marginBottom: '8px' }}><ArrowLeft size={16} /> Geri</button><h1 className="page-title">Yeni Proje Oluştur</h1><p className="page-subtitle">Yeni bir içerik üretim projesi başlatın</p></div></div>
      <form onSubmit={handleSubmit} style={{ maxWidth: '640px' }}>
        <div className="content-block glass-card">
          <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-lg)' }}>
            <div className="form-group"><label className="form-label">Konu *</label><input className="input" name="topic" value={form.topic} onChange={handleChange} placeholder="Örn: Çanakkale Türküsünün Hikâyesi" required /></div>
            <div className="form-group"><label className="form-label">Başlık (opsiyonel)</label><input className="input" name="title" value={form.title} onChange={handleChange} placeholder="Boş bırakılırsa konu başlık olarak kullanılır" /></div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
              <div className="form-group"><label className="form-label">Niş</label><select className="input" name="niche" value={form.niche} onChange={handleChange}><option value="turku_hikayeleri">Türkü Hikâyeleri</option><option value="anadolu_efsaneleri">Anadolu Efsaneleri</option><option value="osmanli_hikayeleri">Osmanlı Hikâyeleri</option><option value="kurtulus_savasi">Kurtuluş Savaşı</option><option value="cocuk_masallari">Çocuk Masalları</option></select></div>
              <div className="form-group"><label className="form-label">Dil</label><select className="input" name="language" value={form.language} onChange={handleChange}><option value="tr">Türkçe</option><option value="en">English</option></select></div>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-md)' }}>
              <div className="form-group"><label className="form-label">Hedef Platform</label><select className="input" name="targetPlatforms" value={form.targetPlatforms} onChange={handleChange}><option value="youtube">YouTube</option><option value="youtube,instagram">YouTube + Instagram</option><option value="youtube,instagram,facebook">Tümü</option></select></div>
              <div className="form-group"><label className="form-label">Video Süresi (dk)</label><input className="input" type="number" name="videoDuration" value={form.videoDuration} onChange={handleChange} min={3} max={30} /></div>
            </div>
          </div>
        </div>
        <div style={{ marginTop: 'var(--space-lg)', display: 'flex', gap: 'var(--space-sm)' }}>
          <button type="submit" className="btn btn-primary btn-lg" disabled={loading || !form.topic.trim()}>{loading ? 'Oluşturuluyor...' : <><Sparkles size={18} /> Projeyi Oluştur</>}</button>
          <button type="button" className="btn btn-secondary btn-lg" onClick={() => navigate('/')}>İptal</button>
        </div>
      </form>
    </div>
  )
}
