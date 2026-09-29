import { ReplacementGuaranteeSettings } from './ReplacementGuaranteeSettings'
import { useUnreadNotifications } from '../placement/useUnreadNotifications'
import { VerificationRequirements } from '../verification/VerificationRequirements'
import { VerificationReviews } from '../verification/VerificationReviews'
import { CompanyTypeManagement } from '../company-types/CompanyTypeManagement'
import { AdminRecords } from './AdminRecords'
import { Link, useSearchParams } from 'react-router'
import { api } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState, Stat, Workspace } from '../marketplace/shared'
interface Stats { users: number; activeUsers: number; jobs: number; pendingVerifications: number; activePlacements: number; replacements: number; candidates: number; employers: number; activeJobs: number; applications: number; pendingBookings: number; demoTransactions: number; pendingCandidateVerifications: number; pendingEmployerVerifications: number; activeCandidates: number; activeEmployers: number }
const load = () => api.get<Stats>('/admin/stats').then(r => r.data)
const sections: Record<string, string> = { Users: 'users', Candidates: 'candidates', Employers: 'employers', Jobs: 'jobs', Applications: 'applications', Bookings: 'bookings', 'Demo payments': 'payments', 'Application assessments': 'assessments', 'Skill attempts': 'attempts', Skills: 'skills' }
export function AdminPage() {
  const state = useLoad(load)
  const unread = useUnreadNotifications()
  const [params] = useSearchParams()
  const tab = params.get('tab') || 'Overview'
  const reviewRole = ['CANDIDATE', 'EMPLOYER'].includes(params.get('role') ?? '') ? params.get('role')! : ''
  const candidateQueue = '/admin/dashboard?tab=Verifications&role=CANDIDATE'
  const employerQueue = '/admin/dashboard?tab=Verifications&role=EMPLOYER'
  const cardLink = 'block rounded-2xl transition hover:ring-2 hover:ring-indigo-400 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-indigo-500'
  return <Workspace title={tab === 'Overview' ? "The marketplace, at a glance." : tab} subtitle={tab === 'Overview' ? "Account health, verification workload and managed hiring in one place." : "Manage marketplace records and operations."}><div className="space-y-8">
    {tab === 'Overview' && <><LoadState {...state} />
    <div className="flex items-center justify-between gap-3"><h2 className="text-xl font-bold">Operational overview</h2><button className="button-secondary" disabled={state.loading} onClick={() => { state.reload(); window.dispatchEvent(new Event('notifications-refresh')) }}>Refresh overview</button></div>
    {state.data && <>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Link className={cardLink} to={candidateQueue}><Stat label="Pending candidate verifications" value={state.data.pendingCandidateVerifications} /></Link>
        <Link className={cardLink} to={employerQueue}><Stat label="Pending employer verifications" value={state.data.pendingEmployerVerifications} /></Link>
        <Link className={cardLink} to="/admin/dashboard?tab=Jobs"><Stat label="Active jobs" value={state.data.activeJobs} /></Link>
        <Link className={cardLink} to="/admin/dashboard?tab=Applications"><Stat label="Total applications" value={state.data.applications} /></Link>
        <Link className={cardLink} to="/admin/notifications"><Stat label="Unread Admin notifications" value={unread ?? 'Unavailable'} /></Link>
        <Stat label="Active candidates" value={state.data.activeCandidates} />
        <Stat label="Active employers" value={state.data.activeEmployers} />
      </div>
      <section className="surface space-y-3" aria-labelledby="admin-attention">
        <h2 id="admin-attention" className="text-xl font-bold">Needs attention</h2>
        <p className="text-sm text-slate-500">Verification counts cover current, applicable submissions awaiting review.</p>
        {state.data.pendingCandidateVerifications === 0 && state.data.pendingEmployerVerifications === 0 && unread === 0
          ? <p className="text-slate-600 dark:text-slate-300">All caught up. No pending verifications or unread notifications.</p>
          : <ul className="space-y-2 text-sm">
            {state.data.pendingCandidateVerifications > 0 && <li><Link className="font-semibold underline underline-offset-4" to={candidateQueue}>{state.data.pendingCandidateVerifications} candidate verification submissions waiting →</Link></li>}
            {state.data.pendingEmployerVerifications > 0 && <li><Link className="font-semibold underline underline-offset-4" to={employerQueue}>{state.data.pendingEmployerVerifications} employer verification submissions waiting →</Link></li>}
            {unread != null && unread > 0 && <li><Link className="font-semibold underline underline-offset-4" to="/admin/notifications">{unread} unread notifications →</Link></li>}
            {unread == null && <li>Notification count is unavailable. <Link className="underline underline-offset-4" to="/admin/notifications">Open notifications</Link> or refresh the overview.</li>}
          </ul>}
      </section>
      <div className="surface flex flex-wrap items-center gap-x-6 gap-y-3 text-sm">
        <span>{state.data.activePlacements} active placements</span>
        <Link className="font-semibold underline underline-offset-4" to="/admin/replacements">{state.data.replacements} replacement requests →</Link>
        <Link className="font-semibold underline underline-offset-4" to="/admin/queue">FIFO waiting room →</Link>
        <Link className="underline underline-offset-4" to="/admin/dashboard?tab=Bookings">{state.data.pendingBookings} bookings awaiting payment →</Link>
      </div>
    </>}
    </>}
    {tab === 'Marketplace settings' && <ReplacementGuaranteeSettings />}
    {sections[tab] && <AdminRecords key={tab} section={sections[tab]} changed={state.reload} />}
    {tab === 'Verifications' && <VerificationReviews key={reviewRole} initialRole={reviewRole} changed={state.reload} />}{tab === 'Verification requirements' && <VerificationRequirements />}{tab === 'Company types' && <CompanyTypeManagement />}
  </div></Workspace>
}
