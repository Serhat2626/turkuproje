import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react'

// ── Auth Types ──

interface AuthUser {
  username: string
  displayName: string
  role: string
}

interface AuthResponse {
  token: string
  tokenType: string
  expiresInMs: number
  username: string
  displayName: string
  role: string
}

interface AuthContextType {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  error: string | null
}

// ── Storage Keys ──

const TOKEN_KEY = 'aa_auth_token'
const USER_KEY = 'aa_auth_user'

// ── Context ──

const AuthContext = createContext<AuthContextType | undefined>(undefined)

/**
 * Auth provider — JWT token ve kullanıcı bilgisi yönetimi.
 * localStorage'da token ve user info saklar.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Sayfa yüklendiğinde localStorage'dan oku
  useEffect(() => {
    try {
      const savedToken = localStorage.getItem(TOKEN_KEY)
      const savedUser = localStorage.getItem(USER_KEY)
      if (savedToken && savedUser) {
        setToken(savedToken)
        setUser(JSON.parse(savedUser))
      }
    } catch {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    } finally {
      setIsLoading(false)
    }
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    setError(null)
    setIsLoading(true)

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })

      if (!response.ok) {
        const body = await response.json().catch(() => null)
        const message = body?.message || 'Giriş başarısız. Kullanıcı adı veya şifre hatalı.'
        throw new Error(message)
      }

      const data: AuthResponse = await response.json()

      // Token ve user bilgisini kaydet
      localStorage.setItem(TOKEN_KEY, data.token)
      const authUser: AuthUser = {
        username: data.username,
        displayName: data.displayName,
        role: data.role,
      }
      localStorage.setItem(USER_KEY, JSON.stringify(authUser))

      setToken(data.token)
      setUser(authUser)
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Beklenmeyen hata oluştu'
      setError(message)
      throw err
    } finally {
      setIsLoading(false)
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
    setError(null)
  }, [])

  const isAuthenticated = token !== null && user !== null

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, isLoading, login, logout, error }}>
      {children}
    </AuthContext.Provider>
  )
}

/**
 * Auth context hook — login/logout/user bilgisi erişimi.
 */
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

/**
 * Auth token getter — API client tarafından kullanılır.
 */
export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}
