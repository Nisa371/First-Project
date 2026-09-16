import { session } from '../features/auth/session'
import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8080/api',
  timeout: 8000,
  headers: { Accept: 'application/json' },
})

api.interceptors.request.use(config => {
  const token = session.token()
  if (token && !['/auth/login', '/auth/register'].includes(config.url ?? '')) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(response => response, error => {
  if (axios.isAxiosError(error) && error.config?.headers.Authorization
      && (error.response?.status === 401 ||
          (error.response?.status === 403 && error.response.data?.error === 'ACCOUNT_INACTIVE'))) {
    // A late response from an older session must not clear a newer login.
    if (error.config.headers.Authorization === `Bearer ${session.token()}`) {
      session.clear()
      window.dispatchEvent(new Event('auth-expired'))
    }
  }
  return Promise.reject(error)
})

export interface ApiFailure { error: string; message: string; fieldErrors?: Record<string, string> }
export function apiFailure(error: unknown): ApiFailure {
  if (axios.isAxiosError<ApiFailure>(error) && error.response?.data?.message) return error.response.data
  return { error: 'NETWORK_ERROR', message: 'We could not connect. Check your connection and try again.' }
}
