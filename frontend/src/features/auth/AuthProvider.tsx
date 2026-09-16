import { AuthContext } from './useAuth'
import { useEffect, useState, useCallback, type ReactNode } from 'react'
import { api, apiFailure } from '../../services/api'
import { session } from './session'
import type { AuthResponse, CurrentUser } from './types'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [loading, setLoading] = useState(!!session.token())
  const [error, setError] = useState<string | null>(null)
  const [expired, setExpired] = useState(false)
  const [attempt, setAttempt] = useState(0)
  const logout = useCallback(() => { session.clear(); setUser(null); setError(null); setLoading(false); setExpired(false) }, [])
  useEffect(() => {
    const expire = () => { setUser(null); setError(null); setLoading(false); setExpired(true) }
    window.addEventListener('auth-expired', expire)
    return () => window.removeEventListener('auth-expired', expire)
  }, [])
  useEffect(() => {
    if (!session.token()) return
    let active = true
    const token = session.token()
    api.get<CurrentUser>('/auth/me').then(({ data }) => {
      if (active && token === session.token()) { setUser(data); setError(null) }
    }).catch(cause => {
      if (active && session.token() === token) setError(apiFailure(cause).message)
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt])
  useEffect(() => {
    if (!user) return
    const token = session.token()
    let expiresAt = 0
    try {
      // Only controls the UI timer; the backend verifies signature and expiration.
      const payload: { exp: number } = JSON.parse(atob(token!.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
      expiresAt = payload.exp * 1000
    } catch { expiresAt = Date.now() }
    const timer = window.setTimeout(() => {
      if (token === session.token()) {
        session.clear()
        window.dispatchEvent(new Event('auth-expired'))
      }
    }, Math.max(0, expiresAt - Date.now()))
    return () => window.clearTimeout(timer)
  }, [user])
  const accept = (response: AuthResponse) => {
    session.save(response.token); setUser(response.user); setExpired(false); setError(null); setLoading(false)
  }
  return <AuthContext.Provider value={{ user, loading, error, expired, accept, logout,
    retry: () => { setLoading(true); setError(null); setAttempt(value => value + 1) } }}>{children}</AuthContext.Provider>
}
