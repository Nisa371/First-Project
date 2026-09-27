import { ApplicationBadge, applicationLabels, employerNextStatuses } from './applicationLifecycle'
import { assessmentLabel } from './api'
import { Avatar } from '../cv/ProfilePhoto'
import { useLoad, words } from './useLoad'
import { useCallback, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { marketplace, downloadCv, type Applicant, type ApplicantPage, type ApplicationStatus, type EvaluationWeights, type EvaluationAttempt } from './api'
import { Workspace, LoadState, Feedback, Empty } from './shared'

function evaluationMessage(attempt: EvaluationAttempt) {
  switch (attempt.failureCode) {
    case 'AI_PROVIDER_NOT_CONFIGURED': return 'AI evaluation provider is not configured yet.'
    case 'CV_TEXT_UNREADABLE': return 'CV text could not be extracted. Please upload a text-based PDF or complete the CV Builder.'
    case 'INSUFFICIENT_CV_DATA': return 'No usable CV data. Upload a text-based PDF or complete the CV Builder.'
    case 'INSUFFICIENT_PORTFOLIO_DATA': return 'Insufficient portfolio data. No projects or portfolio links provided.'
    case 'INVALID_AI_RESPONSE': return 'Evaluation failed: the provider returned an invalid score.'
    default: return 'Evaluation unavailable.'
  }
}
function EvaluationScores({ applicant: a }: { applicant: Applicant }) {
  const scores = [['CV', a.cvScore, a.cvEvaluation], ['Portfolio', a.portfolioScore, a.portfolioEvaluation], ['Experience', a.experienceScore, null], ['Assessment', a.assessmentScore, null]] as const
  return <section className="my-4 rounded-xl border border-slate-200 bg-slate-50 p-4" aria-label="Application evaluation">
    <div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-bold">Evaluation</h3><span className="text-xs capitalize text-slate-600">{words(a.evaluationStatus)}</span></div>
    <dl className="mt-3 grid grid-cols-2 gap-4">{scores.map(([label, score, attempt]) => <div key={label}><dt className="text-xs text-slate-600">{label}</dt><dd className="mt-1 text-sm font-semibold tabular-nums">{score !== null ? score.toFixed(2) : attempt ? (attempt.status === 'FAILED' || attempt.status === 'UNAVAILABLE' ? 'Evaluation unavailable' : 'Evaluation pending') : 'Not evaluated'}</dd></div>)}</dl>
    <div className="mt-3 space-y-2" aria-live="polite">{([['CV', a.cvEvaluation, a.cvScore], ['Portfolio', a.portfolioEvaluation, a.portfolioScore]] as const).map(([label, attempt, score]) => (attempt.status === 'FAILED' || attempt.status === 'UNAVAILABLE') && <p key={label} className="text-xs text-amber-800">{label}: {evaluationMessage(attempt)}{score !== null && ' Previous successful score retained.'}</p>)}</div>
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3"><span className="text-sm text-slate-600">Interview: {assessmentLabel(a.assessmentStatus)}</span><Link className="font-semibold text-indigo-700 underline" to={`/employer/jobs/${a.jobId}/applications/${a.id}/assessment`}>View assessment</Link></div>
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
  const defaults = { status: '', assessment: '', minScore: '', minExperience: '', search: '', sort: 'scoreDesc' }
  const [draft, setDraft] = useState(defaults), [filters, setFilters] = useState(defaults), [page, setPage] = useState(0)
  const [success, setSuccess] = useState('')
  const weightsLoader = useCallback(() => api.get<EvaluationWeights>(`/jobs/${jobId}/evaluation-weights`).then(r => r.data), [jobId])
  const weights = useLoad(weightsLoader)
  const loader = useCallback(() => {
    const params = new URLSearchParams({ page: String(page), size: '20', sort: filters.sort })
    for (const [key, value] of Object.entries(filters)) if (value !== '') params.set(key, value)
    return api.get<ApplicantPage>(`/jobs/${jobId}/applications`, { params }).then(r => r.data)
  }, [jobId, filters, page])
  const state = useLoad(loader)
  const filtered = !!(filters.status || filters.assessment || filters.minScore || filters.minExperience || filters.search)
  const changed = JSON.stringify(draft) !== JSON.stringify(filters)
  function clear() { setDraft(defaults); setFilters(defaults); setPage(0) }
  function card(a: Applicant, rank?: number) {
    return <article key={a.id} className="surface">
      <div className="flex items-start gap-3"><div className="shrink-0 [&>*]:size-14"><Avatar name={a.candidate.fullName} url={a.candidate.profilePhotoUrl} /></div><div className="min-w-0 flex-1"><h2 className="break-words text-xl font-bold">{a.candidate.fullName}</h2><p className="mt-2 text-sm text-slate-600">{a.candidate.candidateType} · {a.candidate.location || 'Location not provided'}</p></div>{rank !== undefined && <span className="badge" aria-label={`Position ${rank} in these results`}>#{rank}</span>}</div>
      <p className="mt-3 text-sm">{a.candidate.totalExperienceMonths} months of experience</p><div className="my-3"><ApplicationBadge status={a.status} /></div>
      <p className="text-xs text-slate-500">Applied {new Date(a.appliedAt).toLocaleString()}</p><EvaluationScores applicant={a} />
      <div className="flex flex-wrap gap-3"><Link className="button-primary" to={`/employer/jobs/${jobId}/applications/${a.id}`}>Review applicant</Link><Link className="button-secondary" to={`/employer/jobs/${jobId}/applications/${a.id}/cv/${a.candidate.id}`}>View built CV</Link></div>
    </article>
  }
  return <>
    <LoadState {...weights} />{weights.data && <WeightEditor jobId={jobId} initial={weights.data} onApplying={() => setSuccess('')} onApplied={() => { setSuccess('Weights saved. Ranking refreshed.'); setPage(0); state.reload() }} />}
    <Feedback success={success} />
    <form className="surface mb-5" onSubmit={e => { e.preventDefault(); setFilters({ ...draft, search: draft.search.trim() }); setPage(0) }} aria-label="Filter applicants">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <label><span className="field-label">Candidate name</span><input className="form-input" type="search" maxLength={200} placeholder="Search this job’s applicants" value={draft.search} onChange={e => setDraft({ ...draft, search: e.target.value })} /></label>
        <label><span className="field-label">Application status</span><select className="form-input" value={draft.status} onChange={e => setDraft({ ...draft, status: e.target.value })}><option value="">All statuses</option>{Object.entries(applicationLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <label><span className="field-label">Assessment state</span><select className="form-input" value={draft.assessment} onChange={e => setDraft({ ...draft, assessment: e.target.value })}><option value="">All assessments</option>{['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED'].map(value => <option key={value} value={value}>{assessmentLabel(value)}</option>)}</select></label>
        <label><span className="field-label">Minimum final score</span><input className="form-input" type="number" min="0" step="any" placeholder="e.g. 0.70" value={draft.minScore} onChange={e => setDraft({ ...draft, minScore: e.target.value })} /></label>
        <label><span className="field-label">Minimum experience (months)</span><input className="form-input" type="number" min="0" max="2147483647" step="1" placeholder="e.g. 12" value={draft.minExperience} onChange={e => setDraft({ ...draft, minExperience: e.target.value })} /></label>
        <label><span className="field-label">Sort applicants</span><select className="form-input" value={draft.sort} onChange={e => setDraft({ ...draft, sort: e.target.value })}><option value="scoreDesc">Final score: highest first</option><option value="scoreAsc">Final score: lowest first</option><option value="newest">Application date: newest</option><option value="oldest">Application date: oldest</option><option value="experienceDesc">Experience: highest first</option><option value="experienceAsc">Experience: lowest first</option></select></label>
      </div>
      <div className="mt-4 flex flex-wrap items-center gap-3"><button className="button-primary">Apply filters</button><button type="button" className="button-secondary" onClick={clear}>Clear filters</button><span className="text-sm text-slate-600 dark:text-slate-300" role="status">{changed ? 'Changes ready to apply' : filtered ? 'Filters active' : 'Showing all statuses'} · {filters.sort === 'scoreDesc' ? 'Highest merit first' : 'Custom sorting'}</span></div>
    </form>
    <LoadState {...state} />{state.data && <>
      <p className="mb-5 text-sm text-slate-600 dark:text-slate-300" role="status">{state.data.totalElements} matching of {state.data.totalApplicants} applicants · Ties use newest application first</p>
      {state.data.content.length ? <div className="grid gap-5 md:grid-cols-2">{state.data.content.map((a, i) => card(a, filters.sort === 'scoreDesc' ? state.data!.page * state.data!.size + i + 1 : undefined))}</div> : <Empty title={state.data.totalApplicants === 0 ? 'No applications received yet.' : 'No applicants match the selected filters.'}>{state.data.totalApplicants === 0 ? 'Candidates will appear here after they apply to this job.' : <button className="button-secondary" onClick={clear}>Clear filters</button>}</Empty>}
      {state.data.totalPages > 1 && <nav className="mt-6 flex flex-wrap items-center justify-between gap-3" aria-label="Applicant pages"><button className="button-secondary" disabled={state.data.page === 0} onClick={() => setPage(state.data!.page - 1)}>Previous</button><span className="text-sm">Page {state.data.page + 1} of {state.data.totalPages} · {state.data.size} per page</span><button className="button-secondary" disabled={state.data.page + 1 >= state.data.totalPages} onClick={() => setPage(state.data!.page + 1)}>Next</button></nav>}
    </>}
  </>
}

export function CandidateSearchPage() {
  const jobs = useLoad(marketplace.jobs), { jobId: routeJob } = useParams(), [params] = useSearchParams(), navigate = useNavigate()
  const jobId = routeJob ?? params.get('job') ?? ''
  return <Workspace title="Job applicants" subtitle="Review and rank people who applied to one of your openings."><LoadState {...jobs} />{jobs.data && <section className="surface mb-6"><label className="field-label" htmlFor="applicant-job">Choose your job</label><select id="applicant-job" className="form-input" value={jobId} onChange={e => navigate(e.target.value ? `/employer/jobs/${e.target.value}/applications` : '/employer/candidates')}><option value="">Select an opening</option>{jobs.data.map(j => <option key={j.id} value={j.id}>{j.title} · {words(j.status)}</option>)}</select>{!jobs.data.length && <Link className="mt-4 inline-block font-semibold text-indigo-700" to="/employer/jobs">Create an opening →</Link>}</section>}{jobId ? <ApplicantRanking key={jobId} jobId={jobId} /> : <Empty title="Select a job to review applicants">Each list contains only applications to the selected opening.</Empty>}</Workspace>
}
export function ApplicantDetailPage() {
  const { jobId, id } = useParams(), loader = useCallback(() => api.get<Applicant>(`/jobs/${jobId}/applications/${id}`).then(r => r.data), [jobId, id]), state = useLoad(loader)
  const [status, setStatus] = useState<ApplicationStatus | ''>(''), [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  async function update() { if (!state.data || !employerNextStatuses[state.data.status].includes(status as ApplicationStatus)) return; setBusy(true); setError(''); setSuccess(''); try { state.setData((await api.patch<Applicant>(`/jobs/${jobId}/applications/${id}/status`, { status })).data); setStatus(''); setSuccess('Application status updated.') } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) } }
  async function hire() { if (!state.data) return; setBusy(true); setError(''); try { await api.post('/placements', { jobId: Number(jobId), candidateId: state.data.candidate.id }); setSuccess('Placement activated. View Placements for details.') } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) } }
  const a = state.data, c = a?.candidate
  return <Workspace title={c?.fullName ?? 'Applicant details'} subtitle="Candidate information shared through this job application."><Link className="mb-5 inline-block font-semibold text-indigo-700" to={`/employer/jobs/${jobId}/applications`}>← Back to this job’s applicants</Link><LoadState {...state} /><Feedback error={error} success={success} />{a && c && <div className="grid items-start gap-5 lg:grid-cols-[1.5fr_1fr]"><article className="surface space-y-5"><Avatar name={c.fullName} url={c.profilePhotoUrl} /><Link className="button-secondary" to={`/employer/jobs/${jobId}/applications/${id}/cv/${c.id}`}>View built CV</Link><div className="flex flex-wrap gap-2"><span className="badge">{c.candidateType}</span><span className="badge">{words(c.availability)}</span><span className="badge">Verification: {words(c.verificationStatus)}</span></div><p>{c.location || 'Location not provided'} · {c.totalExperienceMonths} months of experience</p><p className="whitespace-pre-wrap break-words text-slate-600">{c.bio}</p><div><h2 className="font-bold">Experience</h2><p className="mt-2 whitespace-pre-wrap break-words text-slate-600">{c.experienceSummary || 'No experience description provided.'}</p></div><div className="flex flex-wrap gap-2">{c.skills.map(s => <span className="badge" key={s.id}>{s.name}</span>)}</div><div className="flex flex-wrap gap-3">{c.portfolioUrl && <a className="button-secondary" href={c.portfolioUrl} target="_blank" rel="noreferrer">Portfolio ↗</a>}{c.hasCv && <button className="button-secondary" onClick={() => { setError(''); void downloadCv(`/candidates/${c.id}/cv`).catch(e => setError(apiFailure(e).message)) }}>Download CV</button>}</div></article><section className="surface space-y-4"><EvaluationScores applicant={a} /><h2 className="font-bold">Application status</h2><div role="status"><ApplicationBadge status={a.status} /></div><p className="text-sm text-slate-500">Applied {new Date(a.appliedAt).toLocaleString()}</p>{employerNextStatuses[a.status].length > 0 ? <form onSubmit={e => { e.preventDefault(); void update() }}><fieldset disabled={busy} className="space-y-3"><label className="field-label" htmlFor="application-status">Next status</label><select id="application-status" className="form-input" value={employerNextStatuses[a.status].includes(status as ApplicationStatus) ? status : ''} onChange={e => setStatus(e.target.value as ApplicationStatus)}><option value="">Choose next status</option>{employerNextStatuses[a.status].map(s => <option key={s} value={s}>{applicationLabels[s]}</option>)}</select><button className="button-primary" disabled={busy || !employerNextStatuses[a.status].includes(status as ApplicationStatus)}>{busy ? 'Saving…' : 'Update status'}</button></fieldset></form> : <p className="text-sm text-slate-600">This application is closed. Its details and assessment remain available to review.</p>}{a.status === 'SHORTLISTED' && <button className="button-secondary" disabled={busy} onClick={() => { if (window.confirm(`Activate a placement for ${c.fullName}?`)) void hire() }}>Hire applicant</button>}</section></div>}</Workspace>
}
