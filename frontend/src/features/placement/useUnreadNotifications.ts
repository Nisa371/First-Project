import { useEffect, useState } from 'react'
import { useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { managed } from './api'

export function useUnreadNotifications() {
  const { user } = useAuth()
  const { pathname, search } = useLocation()
  const [count, setCount] = useState<{ userId: number; value: number } | null>(null)
  useEffect(() => {
    if (!user) return
    const userId = user.id
    let active = true
    let request = 0
    let timer: ReturnType<typeof setTimeout> | undefined
    const refresh = async () => {
      const latest = ++request
      try {
        const value = await managed.unreadCount()
        if (active && latest === request) setCount({ userId, value })
      } catch {
        if (active && latest === request) setCount(null)
      }
    }
    const schedule = () => {
      // Coalesce concurrent successful actions; this is not periodic polling.
      ++request
      clearTimeout(timer)
      timer = setTimeout(() => void refresh(), 100)
    }
    void refresh()
    window.addEventListener('notifications-refresh', schedule)
    window.addEventListener('focus', schedule)
    return () => {
      active = false
      clearTimeout(timer)
      window.removeEventListener('notifications-refresh', schedule)
      window.removeEventListener('focus', schedule)
    }
  }, [user?.id, pathname, search])
  return count?.userId === user?.id ? count?.value : null
}
