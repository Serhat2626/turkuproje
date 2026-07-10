import { NavLink } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'

interface NavItem {
  to: string
  icon: string
  label: string
  disabled?: boolean
}

const navItems: NavItem[] = [
  { to: '/', icon: '🏠', label: 'Dashboard' },
  { to: '/knowledge', icon: '🔍', label: 'Bilgi Bankası' },
  { to: '/projects', icon: '📚', label: 'İçerik Projeleri' },
  { to: '/media', icon: '🎨', label: 'Medya' },
  { to: '/turku-video', icon: '🎵', label: 'Türkü Video' },
  { to: '/artists', icon: '🎵', label: 'Sanatçılar', disabled: true },
  { to: '/prompts', icon: '⚡', label: 'Prompt Şablonları', disabled: true },
]

function Sidebar() {
  const { user, logout } = useAuth()

  return (
    <aside className="sidebar">
      {/* ── Brand ────────────────────────── */}
      <div className="sidebar-logo">
        <div className="sidebar-logo-inner">
          <div className="sidebar-logo-icon">🧭</div>
          <div className="sidebar-logo-text">
            <span className="sidebar-logo-title">Anadolu Atlası</span>
            <span className="sidebar-logo-subtitle">Admin Panel</span>
          </div>
        </div>
      </div>

      {/* ── Navigation ──────────────────── */}
      <nav className="sidebar-nav">
        {navItems.map((item) =>
          item.disabled ? (
            <div
              key={item.to}
              className="sidebar-nav-item disabled"
              title="Yakında"
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              <span className="sidebar-nav-label">{item.label}</span>
              <span className="sidebar-nav-badge">Yakında</span>
            </div>
          ) : (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              className={({ isActive }) =>
                `sidebar-nav-item${isActive ? ' active' : ''}`
              }
            >
              <span className="sidebar-nav-icon">{item.icon}</span>
              <span className="sidebar-nav-label">{item.label}</span>
            </NavLink>
          )
        )}
      </nav>

      {/* ── User & Logout ────────────────── */}
      <div className="sidebar-footer">
        <div className="sidebar-user-info">
          <span className="sidebar-user-name">{user?.displayName || user?.username || '—'}</span>
          <span className="sidebar-user-role">{user?.role || ''}</span>
        </div>
        <button
          className="sidebar-logout-btn"
          onClick={logout}
          title="Çıkış Yap"
        >
          🚪
        </button>
      </div>
    </aside>
  )
}

export default Sidebar
