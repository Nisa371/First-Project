import { useCallback } from 'react'
import { Link, useParams } from 'react-router'
import { Workspace, LoadState, Empty } from '../marketplace/shared'
import { useLoad } from '../marketplace/useLoad'
import { tradeApi, statusLabels, statusClass, type VerificationReview } from './api'
const date = (value: string) => new Date(value).toLocaleString()
export { VerificationPage } from '../verification/VerificationPage'
export function VerificationQueuePage() {
  const state = useLoad(tradeApi.pending)
  return <Workspace title="Verification work queue" subtitle="Historical identity references. Administrators manage verification decisions and new documents."><LoadState {...state} />{!state.loading && !state.error && state.data && <section className="surface"><div className="mb-5 flex items-center justify-between gap-3"><h2 className="text-xl font-bold">Awaiting review · {state.data.length}</h2><button className="button-secondary" onClick={state.reload}>Refresh</button></div>{!state.data.length ? <Empty title="All caught up">New verification submissions will appear here.</Empty> : <ul className="divide-y divide-slate-100">{state.data.map(v => <li key={v.id} className="flex flex-wrap items-center justify-between gap-4 py-5"><div><p className="font-bold">{v.candidateName}</p><p className="mt-2 text-sm text-slate-500">{v.candidateType} · {v.trade || 'General verification'} · {date(v.submittedAt)}</p><span className={`mt-2 inline-block rounded-full px-3 py-1 text-sm ${statusClass(v.status)}`}>{statusLabels[v.status]}</span></div><Link className="button-secondary" to={`/evaluator/verifications/${v.id}`}>Review →</Link></li>)}</ul>}</section>}</Workspace>
}
export function VerificationReviewPage() {
  const { id } = useParams(), loader = useCallback(() => tradeApi.detail(Number(id)), [id]), state = useLoad(loader)
  return <Workspace title="Review verification" subtitle="Identity references and review notes are restricted to evaluators. Employers receive only a safe status."><LoadState {...state} />{!state.loading && !state.error && state.data && <Review initial={state.data} key={state.data.id} />}</Workspace>
}
function Review({ initial: v }: { initial: VerificationReview }) {
  return <section className="surface space-y-5"><span className="badge">Historical platform verification</span><h2 className="text-2xl font-bold">{v.candidateName}</h2><p className="text-slate-600">{v.candidateType} · {v.trade || 'General'} · {v.location || 'Location not provided'}</p><div className="rounded-xl bg-slate-50 p-5"><h3 className="text-sm font-semibold">Legacy identity reference</h3><p className="mt-2 whitespace-pre-wrap break-words">{v.identityReference}</p></div><p className={`rounded-xl p-4 font-semibold ${statusClass(v.status)}`}>{statusLabels[v.status]}</p><p className="whitespace-pre-wrap break-words text-sm">{v.notes}</p><p className="text-sm text-slate-500">Verification decisions are now managed by administrators. Existing records are preserved here for reference.</p><Link className="inline-block font-semibold text-indigo-700" to="/evaluator/verifications">← Back to verification queue</Link></section>
}
