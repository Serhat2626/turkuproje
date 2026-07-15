import type { ApiResponse } from '../types'
import { getAuthToken } from '../contexts/AuthContext'

const API_BASE = '/api'

/**
 * Ortak fetch wrapper — tüm API çağrıları buradan geçer.
 * JWT token varsa Authorization header otomatik eklenir.
 * 401 yanıtında kullanıcı login sayfasına yönlendirilir.
 *
 * @param timeoutMs — İstek zaman aşımı (ms). Default: 120000 (2dk).
 *                     Video üretimi gibi uzun işlemler için artırılmalı.
 */
async function request<T>(
  endpoint: string,
  options: RequestInit & { timeoutMs?: number } = {}
): Promise<ApiResponse<T>> {
  const { timeoutMs = 120_000, ...fetchOptions } = options
  const token = getAuthToken()

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(fetchOptions.headers as Record<string, string>),
  }

  // JWT token varsa Authorization header ekle
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  // Timeout kontrolü — AbortController ile
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs)

  try {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      ...fetchOptions,
      headers,
      signal: controller.signal,
    })

    clearTimeout(timeoutId)

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
  } catch (err: any) {
    clearTimeout(timeoutId)
    if (err.name === 'AbortError') {
      throw new Error(`İstek zaman aşımına uğradı (${Math.round(timeoutMs / 1000)}s). Video üretimi hâlâ devam ediyor olabilir — sayfayı yenileyin.`)
    }
    throw err
  }
}

export default request
