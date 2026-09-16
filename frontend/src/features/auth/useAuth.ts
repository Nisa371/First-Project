import { createContext, useContext } from 'react'
import type { CurrentUser, AuthResponse } from './types'

export type AuthState = {
  user: CurrentUser | null
  loading: boolean
  error: string | null
  expired: boolean
  retry: () => void
  accept: (response: AuthResponse) => void
  logout: () => void
}
export const AuthContext = createContext<AuthState | null>(null)
export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is required')
  return value
}
