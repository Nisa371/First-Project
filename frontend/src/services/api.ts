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

api.interceptors.response.use(response => {
  const { method, url, headers } = response.config
  if (headers.Authorization === `Bearer ${session.token()}` &&
      (url === '/notifications/me' || ['post', 'put', 'patch', 'delete'].includes(method ?? ''))) {
    window.dispatchEvent(new Event('notifications-refresh'))
  }
  return response
}, error => {
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
  if (axios.isAxiosError<ApiFailure>(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return { error: 'REQUEST_TIMEOUT', message: 'The AI service is taking longer than expected. Please try again.' }
    }
    if (!error.response) {
      return { error: 'NETWORK_ERROR', message: 'Cannot connect to the server. Check your connection and try again.' }
    }
    const { status, data } = error.response
    if (status === 429) {
      return { error: 'AI_RATE_LIMIT', message: 'The AI service usage limit was reached. Please try again later.' }
    }
    if ([502, 503, 504].includes(status) || ['AI_ASSESSMENT_FAILED', 'AI_PROVIDER_UNAVAILABLE'].includes(data?.error)) {
      return { error: 'AI_UNAVAILABLE', message: 'The AI service is temporarily unavailable. Please try again.' }
    }
    if (typeof data?.error === 'string' && typeof data.message === 'string' && data.message) return data
  }
  return { error: 'REQUEST_FAILED', message: 'The request could not be completed. Please try again.' }
}
