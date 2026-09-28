import { CandidateWorkflow, EmployerWorkflow } from './WorkflowGuide'
import { verification } from '../verification/api'
import { api } from '../../services/api'
import type { Applicant } from './api'
import { useTradeText } from '../trade/useTradeText'
import { ActivitySummary } from './ActivitySummary'
import { ReadinessCard } from '../trade/ReadinessCard'
import { CareerActivity } from '../assessment/CareerActivity'
import { useLoad, words } from './useLoad'
import { Link } from 'react-router'
import { marketplace } from './api'
import { Workspace, LoadState, Stat, Empty } from './shared'
export function CandidateDashboard() {
  const tr = useTradeText()

  const state = useLoad(marketplace.profile), p = state.data
  return <Workspace title={p ? `${tr('Welcome')}, ${p.fullName}` : tr("Your career, in focus")} subtitle={tr("Build a clear profile, share your skills and keep your availability up to date.")}><LoadState {...state} />{!state.loading && !state.error && p && <>
    <CandidateWorkflow profile={p} />
    <div className="mb-6 grid gap-4 sm:grid-cols-3"><Stat label={tr("Skills on your profile")} value={p.skills.length} /><Stat label={tr("Availability")} value={tr(words(p.availability))} /><Stat label={tr("Released assessments")} value={p.releasedResults.length} /></div>
    {p.candidateType === 'TRADE' && <ReadinessCard />}<CareerActivity /><ActivitySummary />
    <div className="grid gap-6 md:grid-cols-2"><section className="surface"><h2 className="text-xl font-bold">{tr("Your expertise")}</h2><p className="mt-2 text-sm text-slate-500">{p.location || tr("Add your location to help employers find you.")}</p><div className="my-5 flex flex-wrap gap-2">{p.skills.map(s => <span className="badge" key={s.id}>{s.name}</span>)}</div>{!p.skills.length && <p className="my-5 text-sm text-slate-500">{tr("Add your first skill to appear in skill searches.")}</p>}<Link className="font-semibold text-indigo-700" to="/candidate/profile">{tr("Edit skills and availability →")}</Link></section><section className="surface"><h2 className="text-xl font-bold">{tr("Platform review")}</h2><p className="mt-4"><span className="badge">{tr(words(p.verificationStatus))}</span></p><p className="mt-4 text-sm leading-relaxed text-slate-500">{tr("Verification is a manual platform review. An active account alone does not mean your skills or identity have been verified.")}</p>{p.releasedResults.length > 0 && <ul className="mt-4 space-y-2">{p.releasedResults.map((r, i) => <li key={i} className="text-sm font-semibold">{tr(words(r.recommendation))} {tr("· Score")} {r.score}</li>)}</ul>}</section></div>
  </>}</Workspace>
}
const employerLoad = async () => {
  const [company, jobs, checklist] = await Promise.all([marketplace.company(), marketplace.jobs(), verification.checklist()])
  const applications = await Promise.all(jobs.filter(job => job.applicationCount > 0).map(job => api.get<Applicant[]>(`/jobs/${job.id}/applications`).then(response => response.data)))
  return { company, jobs, checklist, applicants: applications.flat() }
}
export function EmployerDashboard() {
  const state = useLoad(employerLoad), company = state.data?.company, jobs = state.data?.jobs ?? []
  return <Workspace title={company ? `Welcome, ${company.companyName}` : 'Your hiring, in focus'} subtitle="A clear view of your openings and the candidates you are considering."><LoadState {...state} />{!state.loading && !state.error && company && <>
    {state.data && <EmployerWorkflow {...state.data} />}
    <div className="mb-6 grid gap-4 sm:grid-cols-3"><Stat label="Active jobs" value={jobs.filter(j => j.status === 'ACTIVE').length} /><Stat label="Shortlist entries" value={jobs.reduce((n, j) => n + j.shortlistCount, 0)} /><Stat label="Closed jobs" value={jobs.filter(j => j.status === 'CLOSED').length} /></div>
    <ActivitySummary /><div className="grid items-start gap-6 md:grid-cols-[1.5fr_1fr]"><section className="surface"><div className="mb-5 flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">Recent openings</h2><Link to="/employer/jobs" className="text-sm font-semibold text-indigo-700">View all →</Link></div>{!jobs.length ? <Empty title="Make your first opening">Your jobs and shortlist activity will appear here once you create a role.</Empty> : <ul className="divide-y divide-slate-100">{jobs.slice(0, 5).map(j => <li className="py-4" key={j.id}><Link className="break-words font-bold text-indigo-700" to={`/employer/jobs/${j.id}/applications`}>{j.title} →</Link><p className="mt-2 text-sm text-slate-500">{j.location} · {words(j.status)} · {j.shortlistCount} shortlisted</p><p className="mt-1 text-xs text-slate-500">Created {new Date(j.createdAt).toLocaleDateString()}</p></li>)}</ul>}</section><section className="surface"><h2 className="text-xl font-bold">Your company</h2><p className="mt-4 break-words font-semibold">{company.companyName}</p><p className="mt-2 text-sm text-slate-500">{(company.companyTypeOther ? company.customCompanyType : company.companyTypeName) || 'Add your company type'}</p><p className="mt-4 line-clamp-4 break-words text-sm leading-relaxed text-slate-600">{company.description || 'Introduce your company and give your hiring workspace a clear identity.'}</p><Link className="mt-5 inline-block font-semibold text-indigo-700" to="/employer/profile">Edit company profile →</Link></section></div>
  </>}</Workspace>
}
