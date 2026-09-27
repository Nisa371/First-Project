import { Link } from 'react-router'
import type { Application, ApplicationStatus } from './api'
import { useTradeText } from '../trade/useTradeText'

export const applicationLabels: Record<ApplicationStatus, string> = {
  APPLIED: 'Applied', UNDER_REVIEW: 'Under review', SHORTLISTED: 'Shortlisted', REJECTED: 'Rejected', WITHDRAWN: 'Withdrawn',
}
export const applicationMessages: Record<ApplicationStatus, string> = {
  APPLIED: 'Your application has been submitted and is waiting for review.',
  UNDER_REVIEW: 'The employer is reviewing your application. Watch for their next update.',
  SHORTLISTED: 'You have been shortlisted. Wait for the employer’s next update; a placement is not yet confirmed by this status.',
  REJECTED: 'This application was not selected. You can explore other openings.',
  WITHDRAWN: 'You withdrew this application. It stays in your records and cannot be submitted again.',
}
export const employerNextStatuses: Record<ApplicationStatus, ApplicationStatus[]> = {
  APPLIED: ['UNDER_REVIEW', 'SHORTLISTED', 'REJECTED'], UNDER_REVIEW: ['SHORTLISTED', 'REJECTED'],
  SHORTLISTED: ['REJECTED'], REJECTED: [], WITHDRAWN: [],
}
export const applicationClosed = (status: ApplicationStatus) => status === 'REJECTED' || status === 'WITHDRAWN'
const tones: Record<ApplicationStatus, string> = {
  APPLIED: 'bg-sky-50 text-sky-800', UNDER_REVIEW: 'bg-indigo-50 text-indigo-800',
  SHORTLISTED: 'bg-emerald-50 text-emerald-800', REJECTED: 'bg-rose-50 text-rose-800', WITHDRAWN: 'bg-slate-100 text-slate-600',
}
export function ApplicationBadge({ status }: { status: ApplicationStatus }) {
  const tr = useTradeText()
  return <span className={`badge ${tones[status]}`}>{tr(applicationLabels[status])}</span>
}
export function ApplicationAssessmentLink({ application: a }: { application: Application }) {
  const tr = useTradeText()
  const available = !applicationClosed(a.status) && a.job.status === 'ACTIVE'
  if (a.assessmentStatus === 'NOT_STARTED' && !available) return null
  const label = a.assessmentStatus === 'COMPLETED' ? 'View completed assessment' : a.assessmentStatus === 'FAILED' ? 'View submitted assessment'
    : !available ? 'View assessment' : a.assessmentStatus === 'IN_PROGRESS' ? 'Continue assessment' : 'Start assessment'
  return <Link className="button-secondary" to={`/candidate/applications/${a.id}/assessment`}>{tr(label)}</Link>
}
export function ApplicationTimeline({ application: a }: { application: Application }) {
  const tr = useTradeText()
  const closed = applicationClosed(a.status)
  // Only submission and the current status are known; intermediate transitions are not stored.
  const stages: { status: ApplicationStatus; kind: 'completed' | 'current' | 'future' }[] = [
    { status: 'APPLIED', kind: a.status === 'APPLIED' ? 'current' : 'completed' },
    ...(a.status !== 'APPLIED' ? [{ status: a.status, kind: 'current' as const }] : []),
    ...(!closed ? employerNextStatuses[a.status].filter(s => s !== 'REJECTED').map(status => ({ status, kind: 'future' as const })) : []),
  ]
  const assessment = a.assessmentStatus === 'COMPLETED' ? 'Assessment completed. Your responses have been submitted for this application.'
    : a.assessmentStatus === 'FAILED' ? 'Assessment submitted, but evaluation could not finish. The employer can review its status.'
    : a.assessmentStatus === 'IN_PROGRESS' ? (closed || a.job.status !== 'ACTIVE' ? 'Your saved assessment remains available to view. Further answers are currently unavailable.' : 'Your assessment is in progress. Continue your saved conversation.')
    : closed || a.job.status !== 'ACTIVE' ? 'Assessment not started. It is currently unavailable for this application.' : 'An assessment is available for this application. Start it from here.'
  return <section className="surface space-y-5" aria-label={tr('Application progress')}>
    <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">{tr('Application progress')}</h2><ApplicationBadge status={a.status} /></div>
    <p className="text-sm text-slate-600">{tr(applicationMessages[a.status])}</p>
    <ol className="space-y-3">{stages.map(stage => <li key={stage.status} aria-current={stage.kind === 'current' ? 'step' : undefined}
      className={`flex gap-3 rounded-xl border p-4 ${stage.kind === 'current' ? `${tones[stage.status]} border-current` : 'border-slate-200'} ${stage.kind === 'future' ? 'border-dashed' : ''}`}>
      <span aria-hidden="true" className="font-bold">{stage.kind === 'completed' ? '✓' : stage.kind === 'current' ? '●' : '○'}</span>
      <div><p className="font-semibold">{tr(applicationLabels[stage.status])}</p><p className="mt-1 text-xs text-slate-600">{stage.status === 'APPLIED' ? `${tr('Submitted')} · ${new Date(a.appliedAt).toLocaleString()}` : stage.kind === 'current' ? tr('Current status') : tr('Possible next step · not guaranteed')}</p></div>
    </li>)}</ol>
    <p className="text-xs text-slate-500">{tr('This shows submission, current status and possible next steps, not a complete status history. Review may be skipped before shortlisting.')}{!closed && ` ${tr('The employer may also reject the application.')}`}</p>
    <p className="text-xs text-slate-500">{tr('Application record last updated')}: {new Date(a.updatedAt).toLocaleString()}. {tr('This may include evaluation updates, not just status changes.')}</p>
    <div className="space-y-3 border-t border-slate-200 pt-4"><h3 className="font-bold">{tr('Application assessment')}</h3><p className="text-sm text-slate-600">{tr(assessment)}</p><ApplicationAssessmentLink application={a} /><p className="text-xs text-slate-500">{tr('Assessment progress is separate from the employer’s decision. Completing it does not guarantee shortlisting.')}</p></div>
    {closed && <Link className="button-secondary" to="/candidate/jobs">{tr('Explore jobs')}</Link>}
  </section>
}
