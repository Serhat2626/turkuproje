import { NavLink } from 'react-router-dom'

interface NavItem {
  to: string
  icon: string
  label: string
  disabled?: boolean
}

const navItems: NavItem[] = [
  { to: '/', icon: '🏠', label: 'Dashboard' },
  { to: '/projects', icon: '📚', label: 'İçerik Projeleri', disabled: true },
  { to: '/knowledge', icon: '🔍', label: 'Bilgi Bankası', disabled: true },
  { to: '/media', icon: '🎨', label: 'Medya', disabled: true },
  { to: '/artists', icon: '🎵', label: 'Sanatçılar', disabled: true },
  { to: '/prompts', icon: '⚡', label: 'Prompt Şablonları', disabled: true },
]

function Sidebar() {
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
              end
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

      {/* ── Footer ──────────────────────── */}
      <div className="sidebar-footer">
        <span className="sidebar-version">v0.0.1</span>
        <span className="sidebar-status-dot" title="Sistem Aktif"></span>
      </div>
    </aside>
  )
}

export default Sidebar
