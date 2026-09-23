import { assessmentLabel } from './api'
import { Avatar } from '../cv/ProfilePhoto'
import { useLoad, words } from './useLoad'
import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { marketplace, downloadCv, type Applicant, type ApplicationStatus, type EvaluationWeights, type EvaluationAttempt } from './api'
import { Workspace, LoadState, Feedback, Empty } from './shared'

function evaluationMessage(attempt: EvaluationAttempt) {
  switch (attempt.failureCode) {
    case 'AI_PROVIDER_NOT_CONFIGURED': return 'AI evaluation provider is not configured yet.'
    case 'INSUFFICIENT_CV_DATA': return 'Insufficient CV data. A structured CV is needed; uploaded PDFs have no extracted text.'
    case 'INSUFFICIENT_PORTFOLIO_DATA': return 'Insufficient portfolio data. No projects or portfolio links provided.'
    case 'INVALID_AI_RESPONSE': return 'Evaluation failed: the provider returned an invalid score.'
    default: return 'Evaluation failed. Please try again later.'
  }
}
function EvaluationScores({ applicant: a, onUpdated }: { applicant: Applicant; onUpdated: () => void }) {
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function evaluate(component: 'cv' | 'portfolio' | 'both') {
    if (busy) return
    setBusy(true); setError('')
    try {
      await api.post<Applicant>(`/jobs/${a.jobId}/applications/${a.id}/evaluate${component === 'both' ? '' : `-${component}`}`)
      onUpdated()
    } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  const scores = [['CV', a.cvScore, a.cvEvaluation], ['Portfolio', a.portfolioScore, a.portfolioEvaluation], ['Experience', a.experienceScore, null], ['Assessment', a.assessmentScore, null]] as const
  return <section className="my-4 rounded-xl border border-slate-200 bg-slate-50 p-4" aria-label="Application evaluation">
    <div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-bold">Evaluation</h3><span className="text-xs capitalize text-slate-600">{words(a.evaluationStatus)}</span></div>
    <dl className="mt-3 grid grid-cols-2 gap-4">{scores.map(([label, score, attempt]) => <div key={label}><dt className="text-xs text-slate-600">{label}</dt><dd className="mt-1 text-sm font-semibold tabular-nums">{score !== null ? score.toFixed(2) : attempt?.failureCode === 'AI_PROVIDER_NOT_CONFIGURED' ? 'AI not configured' : attempt?.status === 'FAILED' ? 'Evaluation failed' : attempt?.status === 'UNAVAILABLE' ? 'Insufficient data' : 'Not evaluated'}</dd></div>)}</dl>
    <div className="mt-3 space-y-2" aria-live="polite">{([['CV', a.cvEvaluation, a.cvScore], ['Portfolio', a.portfolioEvaluation, a.portfolioScore]] as const).map(([label, attempt, score]) => (attempt.status === 'FAILED' || attempt.status === 'UNAVAILABLE') && <p key={label} className="text-xs text-amber-800">{label}: {evaluationMessage(attempt)}{score !== null && ' Previous successful score retained.'}</p>)}</div>
    {a.status !== 'WITHDRAWN' && <div className="mt-4 flex flex-wrap gap-2" aria-busy={busy}>{(['cv', 'portfolio', 'both'] as const).map(component => <button key={component} className="button-secondary" disabled={busy} onClick={() => void evaluate(component)}>{busy ? 'Evaluating…' : component === 'both' ? 'Evaluate both with AI' : component === 'cv' ? 'Evaluate CV' : 'Evaluate Portfolio'}</button>)}</div>}
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><span className="text-sm text-slate-600">Interview: {assessmentLabel(a.assessmentStatus)}</span><Link className="font-semibold text-indigo-700 underline" to={`/employer/jobs/${a.jobId}/applications/${a.id}/assessment`}>View assessment</Link></div>
    <Feedback error={error} />
    <div className="mt-4 flex items-center justify-between gap-3 border-t border-slate-200 pt-3"><span className="text-sm font-semibold">Final weighted score</span><strong className="text-xl tabular-nums text-indigo-700">{a.finalScore.toFixed(4)}</strong></div>
    <p className="mt-3 text-xs leading-relaxed text-slate-500">Experience compares the candidate’s months with the job requirement. It can exceed 1 for additional experience; no experience required gives 1. Unevaluated components contribute zero to the final score.</p>
  </section>
}

const weightLabels = [['cvWeight', 'CV'], ['portfolioWeight', 'Portfolio'], ['experienceWeight', 'Experience'], ['assessmentWeight', 'Assessment']] as const
function WeightEditor({ jobId, initial, onApplying, onApplied }: { jobId: string; initial: EvaluationWeights; onApplying: () => void; onApplied: () => void }) {
  const [weights, setWeights] = useState(initial), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function apply() {
    setBusy(true); setError(''); onApplying()
    try {
      await api.put<EvaluationWeights>(`/jobs/${jobId}/evaluation-weights`, weights)
      onApplied()
    } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  return <form className="surface mb-6" onSubmit={e => { e.preventDefault(); void apply() }}>
    <h2 className="text-lg font-bold">Set your ranking priorities</h2>
    <p className="mt-2 text-sm text-slate-600">Each weight is independent, from 0 to 1. They do not need to total 1. Apply to save these priorities for this job and refresh the ranking.</p>
    <fieldset disabled={busy} className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">{weightLabels.map(([key, label]) => <div key={key}>
      <label htmlFor={key} className="flex justify-between gap-3 text-sm font-semibold">{label}<output htmlFor={key} className="tabular-nums text-indigo-700">{weights[key].toFixed(2)}</output></label>
      <input id={key} type="range" min="0" max="1" step="0.05" value={weights[key]} onChange={e => setWeights({ ...weights, [key]: Number(e.target.value) })} className="mt-3 h-8 w-full cursor-pointer accent-indigo-600" />
    </div>)}</fieldset>
    <Feedback error={error} /><button className="button-primary mt-4" disabled={busy}>{busy ? 'Saving weights…' : 'Apply weights'}</button>
  </form>
}

function ApplicantRanking({ jobId }: { jobId: string }) {
  const loader = useCallback(async () => {
    const [applicants, weights] = await Promise.all([
      api.get<Applicant[]>(`/jobs/${jobId}/applications`),
      api.get<EvaluationWeights>(`/jobs/${jobId}/evaluation-weights`),
    ])
    return { applicants: applicants.data, weights: weights.data }
  }, [jobId])
  const state = useLoad(loader), [showWithdrawn, setShowWithdrawn] = useState(false), [success, setSuccess] = useState('')
  const active = state.data?.applicants.filter(a => a.status !== 'WITHDRAWN') ?? []
  const withdrawn = state.data?.applicants.filter(a => a.status === 'WITHDRAWN') ?? []
  function card(a: Applicant, rank?: number) {
    return <article key={a.id} className="surface">
      <div className="flex items-start gap-3"><div className="shrink-0 [&>*]:size-14"><Avatar name={a.candidate.fullName} url={a.candidate.profilePhotoUrl} /></div><div className="min-w-0 flex-1"><h2 className="break-words text-xl font-bold">{a.candidate.fullName}</h2><p className="mt-2 text-sm text-slate-600">{a.candidate.candidateType} · {a.candidate.location || 'Location not provided'}</p></div>{rank !== undefined && <span className="badge" aria-label={`Merit rank ${rank}`}>#{rank}</span>}</div>
      <p className="mt-3 text-sm">{a.candidate.totalExperienceMonths} months of experience</p><span className="badge my-3">{words(a.status)}</span>
      <p className="text-xs text-slate-500">Applied {new Date(a.appliedAt).toLocaleString()}</p><EvaluationScores applicant={a} onUpdated={state.reload} />
      <div className="flex flex-wrap gap-3"><Link className="button-primary" to={`/employer/jobs/${jobId}/applications/${a.id}`}>Review applicant</Link><Link className="button-secondary" to={`/employer/jobs/${jobId}/applications/${a.id}/cv/${a.candidate.id}`}>View built CV</Link></div>
    </article>
  }
  return <><LoadState {...state} />{state.error && <button className="button-secondary mb-4" onClick={state.reload}>Retry ranking</button>}{state.data && <>
    <WeightEditor jobId={jobId} initial={state.data.weights} onApplying={() => setSuccess('')} onApplied={() => { setSuccess('Weights saved. Ranking refreshed.'); state.reload() }} />
    <Feedback success={success} /><div className="mb-5 flex flex-wrap items-center justify-between gap-3"><p className="text-sm text-slate-600">{active.length} applicants · Highest merit first · Ties use newest application first</p>{withdrawn.length > 0 && <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={showWithdrawn} onChange={e => setShowWithdrawn(e.target.checked)} />Show withdrawn ({withdrawn.length})</label>}</div>
    {active.length ? <div className="grid gap-5 md:grid-cols-2">{active.map((a, i) => card(a, i + 1))}</div> : <Empty title="No active applications">Candidates will appear here after they apply to this job. Withdrawn applications remain in history.</Empty>}
    {showWithdrawn && withdrawn.length > 0 && <section className="mt-8"><h2 className="mb-4 text-xl font-bold">Withdrawn applications</h2><div className="grid gap-5 md:grid-cols-2">{withdrawn.map(a => card(a))}</div></section>}
  </>}</>
}

export function CandidateSearchPage() {
  const jobs = useLoad(marketplace.jobs), { jobId: routeJob } = useParams(), [params] = useSearchParams(), navigate = useNavigate()
  const jobId = routeJob ?? params.get('job') ?? ''
  return <Workspace title="Job applicants" subtitle="Review and rank people who applied to one of your openings."><LoadState {...jobs} />{jobs.data && <section className="surface mb-6"><label className="field-label" htmlFor="applicant-job">Choose your job</label><select id="applicant-job" className="form-input" value={jobId} onChange={e => navigate(e.target.value ? `/employer/jobs/${e.target.value}/applications` : '/employer/candidates')}><option value="">Select an opening</option>{jobs.data.map(j => <option key={j.id} value={j.id}>{j.title} · {words(j.status)}</option>)}</select>{!jobs.data.length && <Link className="mt-4 inline-block font-semibold text-indigo-700" to="/employer/jobs">Create an opening →</Link>}</section>}{jobId ? <ApplicantRanking key={jobId} jobId={jobId} /> : <Empty title="Select a job to review applicants">Each list contains only applications to the selected opening.</Empty>}</Workspace>
}
export function ApplicantDetailPage() {
  const { jobId, id } = useParams(), loader = useCallback(() => api.get<Applicant>(`/jobs/${jobId}/applications/${id}`).then(r => r.data), [jobId, id]), state = useLoad(loader)
  const [status, setStatus] = useState<ApplicationStatus>('UNDER_REVIEW'), [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  async function update() { setBusy(true); setError(''); setSuccess(''); try { state.setData((await api.patch<Applicant>(`/jobs/${jobId}/applications/${id}/status`, { status })).data); setSuccess('Application status updated.') } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) } }
  async function hire() { if (!state.data) return; setBusy(true); setError(''); try { await api.post('/placements', { jobId: Number(jobId), candidateId: state.data.candidate.id }); setSuccess('Placement activated. View Placements for details.') } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) } }
  const a = state.data, c = a?.candidate
  return <Workspace title={c?.fullName ?? 'Applicant details'} subtitle="Candidate information shared through this job application."><Link className="mb-5 inline-block font-semibold text-indigo-700" to={`/employer/jobs/${jobId}/applications`}>← Back to this job’s applicants</Link><LoadState {...state} /><Feedback error={error} success={success} />{a && c && <div className="grid items-start gap-5 lg:grid-cols-[1.5fr_1fr]"><article className="surface space-y-5"><Avatar name={c.fullName} url={c.profilePhotoUrl} /><Link className="button-secondary" to={`/employer/jobs/${jobId}/applications/${id}/cv/${c.id}`}>View built CV</Link><div className="flex flex-wrap gap-2"><span className="badge">{c.candidateType}</span><span className="badge">{words(c.availability)}</span><span className="badge">Verification: {words(c.verificationStatus)}</span></div><p>{c.location || 'Location not provided'} · {c.totalExperienceMonths} months of experience</p><p className="whitespace-pre-wrap break-words text-slate-600">{c.bio}</p><div><h2 className="font-bold">Experience</h2><p className="mt-2 whitespace-pre-wrap break-words text-slate-600">{c.experienceSummary || 'No experience description provided.'}</p></div><div className="flex flex-wrap gap-2">{c.skills.map(s => <span className="badge" key={s.id}>{s.name}</span>)}</div><div className="flex flex-wrap gap-3">{c.portfolioUrl && <a className="button-secondary" href={c.portfolioUrl} target="_blank" rel="noreferrer">Portfolio ↗</a>}{c.hasCv && <button className="button-secondary" onClick={() => { setError(''); void downloadCv(`/candidates/${c.id}/cv`).catch(e => setError(apiFailure(e).message)) }}>Download CV</button>}</div></article><section className="surface space-y-4"><EvaluationScores applicant={a} onUpdated={state.reload} /><h2 className="font-bold">Application status</h2><p role="status" className="badge">{words(a.status)}</p><p className="text-sm text-slate-500">Applied {new Date(a.appliedAt).toLocaleString()}</p>{a.status !== 'WITHDRAWN' && <form onSubmit={e => { e.preventDefault(); void update() }}><fieldset disabled={busy} className="space-y-3"><label className="field-label" htmlFor="application-status">Set status</label><select id="application-status" className="form-input" value={status} onChange={e => setStatus(e.target.value as ApplicationStatus)}>{(['APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'REJECTED'] as const).map(s => <option key={s} value={s}>{words(s)}</option>)}</select><button className="button-primary" disabled={busy}>{busy ? 'Saving…' : 'Update status'}</button></fieldset></form>}{a.status === 'SHORTLISTED' && <button className="button-secondary" disabled={busy} onClick={() => { if (window.confirm(`Activate a placement for ${c.fullName}?`)) void hire() }}>Hire applicant</button>}</section></div>}</Workspace>
}
