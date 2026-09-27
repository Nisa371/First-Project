import type { Profile } from './api'
import { candidateReadiness } from './profileReadinessUtils'
import { useTradeText } from '../trade/useTradeText'

export function ProfileReadiness({ profile }: { profile: Profile }) {
  const tr = useTradeText()
  const readiness = candidateReadiness(profile)
  return <section className="surface mb-6" aria-label={tr('Profile Readiness')}>
    <div className="flex items-center justify-between gap-3"><h2 className="text-lg font-bold">{tr('Profile Readiness')}</h2><strong>{readiness.percent}%</strong></div>
    <progress aria-label={tr('Profile Readiness')} className="my-3 h-2 w-full accent-indigo-600" max={100} value={readiness.percent} />
    <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">{readiness.items.map(item => <li key={item.id} className="text-sm"><span aria-label={item.done ? tr('Complete') : tr('Incomplete')}>{item.done ? '✓' : '○'}</span> {tr(item.label)} <span className="text-xs text-slate-500">· {tr(item.required ? 'Required' : 'Recommended')}</span></li>)}</ul>
    <p className="mt-3 text-sm text-slate-600" role="status">{tr(readiness.cvMessage)}</p>
    <p className="mt-2 text-xs text-slate-500">{tr('Based on saved details. Only verification and Available status are required to apply. CV presence contributes to completion; AI readiness is shown separately.')}</p>
  </section>
}
