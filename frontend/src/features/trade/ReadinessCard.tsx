import { useTradeText } from './useTradeText'
import { Link } from 'react-router'
import { tradeApi } from './api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState } from '../marketplace/shared'
const labels: Record<string, [string, string]> = {
  TRADE: ['TRADE পরিচিতি · Trade track', '/candidate/onboarding'], ACTIVE_ACCOUNT: ['সক্রিয় অ্যাকাউন্ট · Active account', '/candidate/profile'],
  VERIFIED: ['প্ল্যাটফর্ম যাচাই · Platform verified', '/candidate/verification'], TRADE_SKILL: ['পেশা ও দক্ষতা · Trade & skill', '/candidate/onboarding'],
  HIRE_READY: ['মূল্যায়নে প্রস্তুত · Hire-ready assessment', '/candidate/assessments'], AVAILABLE: ['কাজের জন্য প্রস্তুত · Available', '/candidate/profile'],
  NOT_RESERVED: ['অন্য কাজে সংরক্ষিত নন · Not reserved', '/candidate/dashboard'], NO_ACTIVE_PLACEMENT: ['চলমান নিয়োগ নেই · No active placement', '/candidate/dashboard'],
}
export function ReadinessCard() {
  const tr = useTradeText()

  const state = useLoad(tradeApi.readiness), r = state.data
  return <section className="surface mb-6"><div className="mb-4 flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">{tr("কাজের প্রস্তুতি · Readiness")}</h2><button className="button-secondary" disabled={state.loading} onClick={state.reload}>{tr("হালনাগাদ · Refresh")}</button></div><LoadState {...state} />{!state.loading && !state.error && r && <><p className={`mb-5 rounded-xl p-4 font-semibold ${r.eligible ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-900'}`}>{r.eligible ? tr("✓ যোগ্যতার শর্ত পূরণ হয়েছে · Readiness requirements met") : tr("আরও কিছু ধাপ বাকি · A few steps to go")}</p><ul className="grid gap-3 sm:grid-cols-2">{r.checks.map(c => <li key={c.code} className="flex items-start gap-3 rounded-xl bg-slate-50 p-3"><span aria-label={c.passed ? tr("Complete") : tr("Incomplete")} className={c.passed ? 'text-emerald-700' : 'text-slate-400'}>{c.passed ? '✓' : '○'}</span><Link className="text-sm leading-relaxed hover:underline" to={labels[c.code]?.[1] ?? '/candidate/dashboard'}>{tr(labels[c.code]?.[0] ?? c.code)}</Link></li>)}</ul><p className="mt-4 text-sm leading-relaxed text-slate-500">{tr("প্রস্তুতির অবস্থা বর্তমান তথ্য অনুযায়ী। এটি অপেক্ষমাণ তালিকায় ভর্তি বা চাকরির নিশ্চয়তা নয়। Readiness is based on current records; it does not confirm queue admission or a job.")}</p></>}</section>
}
