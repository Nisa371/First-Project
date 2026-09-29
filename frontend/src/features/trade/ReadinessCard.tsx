import { useTradeText } from './useTradeText'
import { Link } from 'react-router'
import { tradeApi } from './api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState } from '../marketplace/shared'
const labels: Record<string, [string, string]> = {
  TRADE: ['Trade track', '/candidate/profile'], ACTIVE_ACCOUNT: ['Active account', '/candidate/profile'],
  VERIFIED: ['Platform verified', '/candidate/verification'], TRADE_SKILL: ['Trade skill', '/candidate/profile'],
  AVAILABLE: ['Available', '/candidate/profile'],
  NOT_RESERVED: ['Not reserved', '/candidate/dashboard'],
}
export function ReadinessCard() {
  const tr = useTradeText()

  const state = useLoad(tradeApi.readiness), r = state.data
  return <section className="surface mb-6"><div className="mb-4 flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">{tr("Readiness")}</h2><button className="button-secondary" disabled={state.loading} onClick={state.reload}>{tr("Refresh")}</button></div><LoadState {...state} />{!state.loading && !state.error && r && <><p className={`mb-5 rounded-xl p-4 font-semibold ${r.eligible ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-900'}`}>{r.eligible ? tr("Readiness requirements met") : tr("A few steps to go")}</p><ul className="grid gap-3 sm:grid-cols-2">{r.checks.map(c => <li key={c.code} className="flex items-start gap-3 rounded-xl bg-slate-50 p-3"><span aria-label={c.passed ? tr("Complete") : tr("Incomplete")} className={c.passed ? 'text-emerald-700' : 'text-slate-400'}>{c.passed ? '✓' : '○'}</span><Link className="text-sm leading-relaxed hover:underline" to={labels[c.code]?.[1] ?? '/candidate/dashboard'}>{tr(labels[c.code]?.[0] ?? c.code)}</Link></li>)}</ul><p className="mt-4 text-sm leading-relaxed text-slate-500">{tr("Eligible workers are queued automatically; a job is not guaranteed.")}</p></>}</section>
}
