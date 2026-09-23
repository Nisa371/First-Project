import { useTradeText } from '../trade/useTradeText'
import { useCallback } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { managed } from '../placement/api'
import { api } from '../../services/api'
import { useLoad } from './useLoad'
import { LoadState } from './shared'
export function ActivitySummary() {
  const tr = useTradeText()

  const { user } = useAuth(); const candidate = user?.role === 'CANDIDATE'; const root = `/${user?.role.toLowerCase()}`
  const loader = useCallback(async () => {
    const [placements, notifications, activity] = await Promise.all([managed.placements(), managed.notifications(), candidate ? api.get<{ id: number }[]>('/referrals').then(r => r.data) : managed.replacements()])
    return { placements, notifications, activity }
  }, [candidate])
  const state = useLoad(loader)
  return <section className="mb-6"><LoadState {...state} />{state.data && <div className="grid gap-4 sm:grid-cols-3">{[
    ['placements', tr("Active placements"), state.data.placements.filter(p => p.status === 'ACTIVE').length],
    [candidate ? 'training' : 'replacements', candidate ? tr("Training referrals") : tr("Replacement requests"), state.data.activity.length],
    ['notifications', tr("Unread updates"), state.data.notifications.filter(n => !n.readAt).length],
  ].map(([path, label, count]) => <Link key={path} to={`${root}/${path}`} className="surface hover:border-indigo-300"><p className="text-sm text-slate-600">{label}</p><p className="mt-3 text-3xl font-bold">{count}</p><p className="mt-3 text-sm font-semibold text-indigo-700">{tr("View details →")}</p></Link>)}</div>}</section>
}
