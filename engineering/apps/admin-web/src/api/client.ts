import type { ApiResponse } from '../types'
import { getAuthToken } from '../contexts/AuthContext'

const API_BASE = '/api'

/**
 * Ortak fetch wrapper — tüm API çağrıları buradan geçer.
 * JWT token varsa Authorization header otomatik eklenir.
 * 401 yanıtında kullanıcı login sayfasına yönlendirilir.
 */
async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const token = getAuthToken()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  }

  // JWT token varsa Authorization header ekle
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers,
  })

  // 401 — Token geçersiz veya süresi dolmuş → login'e yönlendir
  if (response.status === 401) {
    localStorage.removeItem('aa_auth_token')
    localStorage.removeItem('aa_auth_user')
    window.location.href = '/login'
    throw new Error('Oturum süresi dolmuş. Lütfen tekrar giriş yapın.')
  }

  const body: ApiResponse<T> = await response.json()

  if (!response.ok) {
    throw body
  }

  return body
}

export default request
