import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectApi } from '../api/client'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'
import { Plus, Calendar, Clock, FolderOpen } from 'lucide-react'

export default function DashboardPage() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => { loadProjects() }, [])
  const loadProjects = async () => {
    try { const res = await projectApi.list(); setProjects(res.data || []) }
    catch (err) { console.error('Projeler yüklenemedi:', err) }
    finally { setLoading(false) }
  }
  const formatDate = (d) => d ? new Date(d).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short', year: 'numeric' }) : ''

  if (loading) return <LoadingSpinner />
  return (
    <div>
      <div className="page-header"><div><h1 className="page-title">Dashboard</h1><p className="page-subtitle">İçerik üretim projelerinizi yönetin</p></div><button className="btn btn-primary" onClick={() => navigate('/projects/new')}><Plus size={18} /> Yeni Proje</button></div>
      <div className="stats-row">
        <div className="stat-card glass-card"><div className="stat-value">{projects.length}</div><div className="stat-label">Toplam Proje</div></div>
        <div className="stat-card glass-card"><div className="stat-value">{projects.filter(p => p.status === 'DRAFT').length}</div><div className="stat-label">Taslak</div></div>
        <div className="stat-card glass-card"><div className="stat-value">{projects.filter(p => !['DRAFT','PUBLISHED'].includes(p.status)).length}</div><div className="stat-label">Üretimde</div></div>
        <div className="stat-card glass-card"><div className="stat-value">{projects.filter(p => p.status === 'PUBLISHED').length}</div><div className="stat-label">Yayında</div></div>
      </div>
      {projects.length === 0 ? (
        <EmptyState icon="🎵" title="Henüz proje yok" message="İlk türkü hikâyesi projenizi oluşturun" action={<button className="btn btn-primary btn-lg" onClick={() => navigate('/projects/new')}><Plus size={18} /> İlk Projeyi Oluştur</button>} />
      ) : (
        <div className="projects-grid">
          {projects.map((p) => (
            <div key={p.id} className="project-card glass-card" onClick={() => navigate(`/projects/${p.id}`)}>
              <div className="project-card-header"><div><div className="project-card-title">{p.title}</div><div className="project-card-topic">{p.topic}</div></div><StatusBadge status={p.status} /></div>
              <div className="project-card-meta"><span style={{ display:'flex', alignItems:'center', gap:'4px' }}><Calendar size={12} /> {formatDate(p.createdAt)}</span><span style={{ display:'flex', alignItems:'center', gap:'4px' }}><Clock size={12} /> {p.videoDuration} dk</span><span style={{ display:'flex', alignItems:'center', gap:'4px' }}><FolderOpen size={12} /> {p.niche}</span></div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
