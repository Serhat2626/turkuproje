import { NavLink } from 'react-router-dom'
import { LayoutDashboard, FolderPlus, Settings, BookOpen, Sparkles } from 'lucide-react'

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <Sparkles size={24} color="#f59e0b" />
        <h1>TürkuProje</h1>
      </div>
      <nav className="sidebar-nav">
        <span className="nav-section-title">Ana Menü</span>
        <NavLink to="/" end className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <LayoutDashboard size={18} /> Dashboard
        </NavLink>
        <NavLink to="/projects/new" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <FolderPlus size={18} /> Yeni Proje
        </NavLink>
        <span className="nav-section-title">Yönetim</span>
        <NavLink to="/templates" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <BookOpen size={18} /> Prompt Şablonları
        </NavLink>
        <NavLink to="/settings" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <Settings size={18} /> Ayarlar
        </NavLink>
      </nav>
      <div style={{ padding: 'var(--space-md)', borderTop: '1px solid var(--border-color)', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
        TürkuProje v0.1.0
      </div>
    </aside>
  )
}
