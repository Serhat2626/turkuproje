import { useEffect, useState, useCallback } from 'react'
import type { HealthResponse } from '../types'

type HealthStatus = 'loading' | 'up' | 'down'

interface HealthState {
  status: HealthStatus
  service: string
  lastCheck: string | null
  error: string | null
}

function DashboardPage() {
  const [health, setHealth] = useState<HealthState>({
    status: 'loading',
    service: '',
    lastCheck: null,
    error: null,
  })

  const checkHealth = useCallback(async () => {
    try {
      const response = await fetch('/api/health')
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const data: HealthResponse = await response.json()
      setHealth({
        status: data.status === 'UP' ? 'up' : 'down',
        service: data.service || 'TürküProje Backend',
        lastCheck: new Date().toLocaleTimeString('tr-TR'),
        error: null,
      })
    } catch (_err) {
      setHealth((prev) => ({
        ...prev,
        status: 'down',
        lastCheck: new Date().toLocaleTimeString('tr-TR'),
        error: 'Bağlantı Kurulamadı',
      }))
    }
  }, [])

  useEffect(() => {
    checkHealth()
    const interval = setInterval(checkHealth, 30_000)
    return () => clearInterval(interval)
  }, [checkHealth])

  const statusText =
    health.status === 'loading'
      ? 'Kontrol Ediliyor…'
      : health.status === 'up'
        ? 'Çalışıyor'
        : health.error || 'Servis Yanıt Vermiyor'

  const stats = [
    { icon: '📚', label: 'İçerik Projeleri', value: '—', color: 'gold' },
    { icon: '🔍', label: 'Bilgi Bankası', value: '—', color: 'teal' },
    { icon: '🎨', label: 'Medya Varlıkları', value: '—', color: 'blue' },
    { icon: '🎵', label: 'Sanatçılar', value: '—', color: 'purple' },
  ]

  const actions = [
    {
      icon: '📝',
      title: 'Yeni İçerik Projesi',
      desc: 'Yeni bir konu belirleyerek içerik üretim sürecini başlatın.',
    },
    {
      icon: '📖',
      title: 'Bilgi Kaydı Ekle',
      desc: 'Bilgi bankasına yeni araştırma notları veya kaynak ekleyin.',
    },
    {
      icon: '⚡',
      title: 'Yeni Prompt Şablonu',
      desc: 'AI üretim süreçleri için yeni prompt şablonu oluşturun.',
    },
  ]

  return (
    <div className="fade-in">
      {/* ── Page Header ──────────────────── */}
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">
          Anadolu Atlası içerik üretim platformu yönetim paneli
        </p>
      </div>

      {/* ── Health Status ────────────────── */}
      <div className="section fade-in-delay-1">
        <div className="section-title">
          Sistem Durumu
          <span className="section-title-line" />
        </div>

        <div className="status-card">
          <div className="status-card-header">
            <span className="status-card-title">Backend Servisi</span>
            {health.status !== 'loading' && (
              <button
                className="btn btn-secondary"
                onClick={checkHealth}
                style={{ padding: '6px 12px', fontSize: '12px' }}
              >
                ↻ Yenile
              </button>
            )}
          </div>

          <div className="status-card-body">
            <span className={`status-indicator ${health.status}`} />
            <div className="status-info">
              <span className="status-label">{statusText}</span>
              <span className="status-sublabel">
                {health.service || 'Bağlantı bekleniyor...'}
              </span>
            </div>
          </div>

          {health.lastCheck && (
            <div className="status-meta">
              <div className="status-meta-item">
                <span className="status-meta-label">Son Kontrol</span>
                <span className="status-meta-value">{health.lastCheck}</span>
              </div>
              <div className="status-meta-item">
                <span className="status-meta-label">Otomatik Yenileme</span>
                <span className="status-meta-value">30 saniye</span>
              </div>
              <div className="status-meta-item">
                <span className="status-meta-label">Endpoint</span>
                <span className="status-meta-value">/api/health</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── Stats ────────────────────────── */}
      <div className="section fade-in-delay-2">
        <div className="section-title">
          Genel Bakış
          <span className="section-title-line" />
        </div>

        <div className="stats-grid">
          {stats.map((stat) => (
            <div className="stat-card" key={stat.label}>
              <div className={`stat-card-icon ${stat.color}`}>
                {stat.icon}
              </div>
              <div className="stat-card-value">{stat.value}</div>
              <div className="stat-card-label">{stat.label}</div>
            </div>
          ))}
        </div>
      </div>

      {/* ── Quick Actions ────────────────── */}
      <div className="section fade-in-delay-3">
        <div className="section-title">
          Hızlı İşlemler
          <span className="section-title-line" />
        </div>

        <div className="actions-grid">
          {actions.map((action) => (
            <div className="action-card" key={action.title}>
              <div className="action-card-icon">{action.icon}</div>
              <div className="action-card-content">
                <div className="action-card-title">{action.title}</div>
                <div className="action-card-desc">{action.desc}</div>
                <span className="badge badge-soon">Yakında</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default DashboardPage
