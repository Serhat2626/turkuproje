import { useState, type FormEvent } from 'react'
import { useAuth } from '../contexts/AuthContext'

/**
 * Admin login sayfası.
 * Anadolu Atlası markasına uygun premium tasarım.
 */
export default function LoginPage() {
  const { login, error, isLoading } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setLocalError(null)

    if (!username.trim() || !password.trim()) {
      setLocalError('Kullanıcı adı ve şifre gereklidir.')
      return
    }

    try {
      await login(username, password)
    } catch {
      // Error is handled in AuthContext
    }
  }

  const displayError = localError || error

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {/* Logo / Branding */}
        <div style={styles.logoSection}>
          <div style={styles.logoIcon}>🏔️</div>
          <h1 style={styles.title}>Anadolu Atlası</h1>
          <p style={styles.subtitle}>Admin Panel</p>
        </div>

        {/* Error message */}
        {displayError && (
          <div style={styles.errorBox}>
            <span style={styles.errorIcon}>⚠️</span>
            {displayError}
          </div>
        )}

        {/* Login form */}
        <form onSubmit={handleSubmit} style={styles.form}>
          <div style={styles.inputGroup}>
            <label htmlFor="username" style={styles.label}>Kullanıcı Adı</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              autoComplete="username"
              autoFocus
              disabled={isLoading}
              style={styles.input}
            />
          </div>

          <div style={styles.inputGroup}>
            <label htmlFor="password" style={styles.label}>Şifre</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              disabled={isLoading}
              style={styles.input}
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            style={{
              ...styles.button,
              opacity: isLoading ? 0.7 : 1,
              cursor: isLoading ? 'not-allowed' : 'pointer',
            }}
          >
            {isLoading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
          </button>
        </form>

        <p style={styles.footer}>
          TürküProje — Story-to-Video SaaS
        </p>
      </div>
    </div>
  )
}

// ── Inline Styles ──

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #334155 100%)',
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  },
  card: {
    width: '100%',
    maxWidth: '420px',
    padding: '40px',
    borderRadius: '16px',
    background: 'rgba(30, 41, 59, 0.8)',
    backdropFilter: 'blur(20px)',
    border: '1px solid rgba(148, 163, 184, 0.1)',
    boxShadow: '0 25px 50px rgba(0, 0, 0, 0.3)',
  },
  logoSection: {
    textAlign: 'center' as const,
    marginBottom: '32px',
  },
  logoIcon: {
    fontSize: '48px',
    marginBottom: '12px',
  },
  title: {
    fontSize: '24px',
    fontWeight: 700,
    color: '#f1f5f9',
    margin: '0 0 4px 0',
    letterSpacing: '-0.5px',
  },
  subtitle: {
    fontSize: '14px',
    color: '#94a3b8',
    margin: 0,
  },
  errorBox: {
    padding: '12px 16px',
    borderRadius: '8px',
    background: 'rgba(239, 68, 68, 0.15)',
    border: '1px solid rgba(239, 68, 68, 0.3)',
    color: '#fca5a5',
    fontSize: '14px',
    marginBottom: '24px',
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  errorIcon: {
    fontSize: '16px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '20px',
  },
  inputGroup: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '6px',
  },
  label: {
    fontSize: '13px',
    fontWeight: 500,
    color: '#94a3b8',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.5px',
  },
  input: {
    padding: '12px 16px',
    borderRadius: '8px',
    border: '1px solid rgba(148, 163, 184, 0.2)',
    background: 'rgba(15, 23, 42, 0.6)',
    color: '#f1f5f9',
    fontSize: '15px',
    outline: 'none',
    transition: 'border-color 0.2s',
  },
  button: {
    padding: '14px',
    borderRadius: '8px',
    border: 'none',
    background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
    color: '#ffffff',
    fontSize: '15px',
    fontWeight: 600,
    marginTop: '8px',
    transition: 'all 0.2s',
  },
  footer: {
    textAlign: 'center' as const,
    fontSize: '12px',
    color: '#475569',
    marginTop: '24px',
    marginBottom: 0,
  },
}
