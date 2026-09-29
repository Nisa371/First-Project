import { candidateTrackLabel } from '../auth/types'
import { ApplicationBadge, ApplicationTimeline, ApplicationAssessmentLink, applicationClosed, applicationMessages } from './applicationLifecycle'
import { useTradeText } from '../trade/useTradeText'
import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useLoad } from './useLoad'
import { Workspace, LoadState, Empty, Feedback } from './shared'
import { type Application } from './api'
const applications = () => api.get<Application[]>('/candidates/me/applications').then(r => r.data)
export function MyApplicationsPage() {
  const tr = useTradeText()

  const state = useLoad(applications), [busy, setBusy] = useState<number | null>(null), [error, setError] = useState('')
  async function withdraw(id: number) { setBusy(id); setError(''); try { const { data } = await api.patch<Application>(`/applications/${id}/withdraw`); state.setData(state.data!.map(a => a.id === id ? data : a)) } catch (e) { setError(apiFailure(e).message) } finally { setBusy(null) } }
  return <Workspace title={tr("My applications")} subtitle={tr("Track your applications and employer decisions.")}><LoadState {...state} /><Feedback error={error} />{state.data && <div className="space-y-4">{state.data.map(a => <article key={a.id} className="surface flex flex-wrap items-center justify-between gap-5"><div className="min-w-0"><h2 className="break-words text-xl font-bold">{a.job.title}</h2><p className="mt-2 text-slate-600">{a.job.companyName} · {a.job.location}</p><p className="mt-2 text-sm text-slate-500">{tr("Applied")}{' '}{new Date(a.appliedAt).toLocaleString()}</p><div className="mt-3"><ApplicationBadge status={a.status} /></div><p className="mt-2 max-w-xl text-sm text-slate-600">{tr(applicationMessages[a.status])}</p></div><div className="flex flex-wrap gap-3"><Link className="button-secondary" to={`/candidate/applications/${a.id}`}>{tr("View details")}</Link><ApplicationAssessmentLink application={a} />{!applicationClosed(a.status) && <button className="button-secondary button-danger" disabled={busy !== null} onClick={() => { if (window.confirm(tr("Withdraw this application? It will remain in your history and cannot be submitted again."))) void withdraw(a.id) }}>{busy === a.id ? tr("Withdrawing…") : tr("Withdraw")}</button>}</div></article>)}{!state.data.length && <Empty title={tr("You haven’t applied yet")}><Link className="font-semibold text-indigo-700 underline" to="/candidate/jobs">{tr("View current openings")}</Link> {tr("to find a role and apply.")}</Empty>}</div>}</Workspace>
}

export function ApplicationDetailPage() {
  const tr = useTradeText(), { id } = useParams()
  const loader = useCallback(() => api.get<Application>(`/candidates/me/applications/${id}`).then(r => r.data), [id])
  const state = useLoad(loader), a = state.data
  return <Workspace title={a?.job.title ?? tr('Application details')} subtitle={tr('Your application, current status and next steps.')}>
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to="/candidate/applications">{tr('← My applications')}</Link>
    <LoadState {...state} />
    {state.error && <button className="button-secondary" onClick={state.reload}>{tr('Try again')}</button>}
    {a && <div className="grid items-start gap-5 lg:grid-cols-2"><article className="surface space-y-4">
      <h2 className="text-xl font-bold">{a.job.companyName}</h2><p className="text-slate-600">{a.job.location} · {tr(candidateTrackLabel(a.job.candidateType))}</p>
      <p className="text-sm text-slate-500">{tr('Application')} #{a.id} · {tr('Applied')} {new Date(a.appliedAt).toLocaleString()}</p>
      <p className="whitespace-pre-wrap break-words text-slate-600">{a.job.description}</p>
      <Link className="button-secondary" to={`/candidate/jobs/${a.job.id}`}>{tr('View job details')}</Link>
      <button className="button-secondary" onClick={state.reload}>{tr('Refresh status')}</button>
    </article><ApplicationTimeline application={a} /></div>}
  </Workspace>
}
