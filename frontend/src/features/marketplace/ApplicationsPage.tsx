import { useTradeText } from '../trade/useTradeText'
import { useState } from 'react'
import { Link } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useLoad, words } from './useLoad'
import { Workspace, LoadState, Empty, Feedback } from './shared'
import { type Application } from './api'
const applications = () => api.get<Application[]>('/candidates/me/applications').then(r => r.data)
export function MyApplicationsPage() {
  const tr = useTradeText()

  const state = useLoad(applications), [busy, setBusy] = useState<number | null>(null), [error, setError] = useState('')
  async function withdraw(id: number) { setBusy(id); setError(''); try { const { data } = await api.patch<Application>(`/applications/${id}/withdraw`); state.setData(state.data!.map(a => a.id === id ? data : a)) } catch (e) { setError(apiFailure(e).message) } finally { setBusy(null) } }
  return <Workspace title={tr("My applications")} subtitle={tr("Track your applications and employer decisions.")}><LoadState {...state} /><Feedback error={error} />{state.data && <div className="space-y-4">{state.data.map(a => <article key={a.id} className="surface flex flex-wrap items-center justify-between gap-5"><div className="min-w-0"><h2 className="break-words text-xl font-bold">{a.job.title}</h2><p className="mt-2 text-slate-600">{a.job.companyName} · {a.job.location}</p><p className="mt-2 text-sm text-slate-500">{tr("Applied")}{' '}{new Date(a.appliedAt).toLocaleString()}</p><span className="badge mt-3">{tr(words(a.status))}</span></div><div className="flex flex-wrap gap-3"><Link className="button-secondary" to={`/candidate/jobs/${a.job.id}`}>{tr("View details")}</Link>{(a.assessmentStatus !== 'NOT_STARTED' || (a.status !== 'WITHDRAWN' && a.status !== 'REJECTED' && a.job.status === 'ACTIVE')) && <Link className="button-primary" to={`/candidate/applications/${a.id}/assessment`}>{a.assessmentStatus === 'COMPLETED' ? tr("Assessment completed") : a.assessmentStatus === 'FAILED' ? tr("View submitted assessment") : a.assessmentStatus === 'IN_PROGRESS' ? tr("Continue assessment") : tr("Start assessment")}</Link>}{a.status !== 'WITHDRAWN' && <button className="button-secondary" disabled={busy !== null} onClick={() => { if (window.confirm(tr("Withdraw this application? It will remain in your history and cannot be submitted again."))) void withdraw(a.id) }}>{busy === a.id ? tr("Withdrawing…") : tr("Withdraw")}</button>}</div></article>)}{!state.data.length && <Empty title={tr("You haven’t applied yet")}><Link className="font-semibold text-indigo-700 underline" to="/candidate/jobs">{tr("View current openings")}</Link> {tr("to find a role and apply.")}</Empty>}</div>}</Workspace>
}
