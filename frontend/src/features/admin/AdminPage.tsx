import { VerificationRequirements } from '../verification/VerificationRequirements'
import { VerificationReviews } from '../verification/VerificationReviews'
import { CompanyTypeManagement } from '../company-types/CompanyTypeManagement'
import { AdminRecords } from './AdminRecords'
import { useState } from 'react'
import { Link } from 'react-router'
import { api } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState, Stat, Workspace } from '../marketplace/shared'
interface Stats { users: number; activeUsers: number; jobs: number; pendingVerifications: number; activePlacements: number; replacements: number; candidates: number; employers: number; activeJobs: number; applications: number; pendingBookings: number; demoTransactions: number }
const load = () => api.get<Stats>('/admin/stats').then(r => r.data)
const sections: Record<string, string> = { Users: 'users', Candidates: 'candidates', Employers: 'employers', Jobs: 'jobs', Applications: 'applications', Bookings: 'bookings', 'Demo payments': 'payments', 'Application assessments': 'assessments', 'Skill attempts': 'attempts', Skills: 'skills' }
export function AdminPage() {
  const state = useLoad(load), [tab, setTab] = useState('Users')
  return <Workspace title="The marketplace, at a glance." subtitle="Account health, verification workload and managed hiring in one place."><LoadState {...state} /><div className="space-y-8">
    {state.data && <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"><Stat label="Active accounts" value={`${state.data.activeUsers} / ${state.data.users}`} /><Stat label="Candidates / employers" value={`${state.data.candidates} / ${state.data.employers}`} /><Stat label="Active jobs / total" value={`${state.data.activeJobs} / ${state.data.jobs}`} /><Stat label="Applications" value={state.data.applications} /><Stat label="Pending verification submissions" value={state.data.pendingVerifications} /><Stat label="Bookings awaiting payment" value={state.data.pendingBookings} /><Stat label="Demo transactions" value={state.data.demoTransactions} /><Stat label="Active placements" value={state.data.activePlacements} /><div className="surface bg-slate-900 text-white"><p className="text-emerald-300">{state.data.replacements} replacement requests</p><Link to="/admin/replacements" className="mt-3 inline-block font-bold underline underline-offset-4">View replacement activity →</Link><Link to="/admin/queue" className="mt-4 block underline underline-offset-4">Open FIFO waiting room →</Link></div></div>}
    <nav className="flex flex-wrap gap-2" aria-label="Admin sections">{[...Object.keys(sections), 'Company types', 'Verifications', 'Verification requirements'].map(t => <button key={t} aria-pressed={tab === t} className={tab === t ? 'button-primary' : 'button-secondary'} onClick={() => setTab(t)}>{t}</button>)}</nav>
    {sections[tab] && <AdminRecords key={tab} section={sections[tab]} changed={state.reload} />}
    {tab === 'Verifications' && <VerificationReviews />}{tab === 'Verification requirements' && <VerificationRequirements />}{tab === 'Company types' && <CompanyTypeManagement />}
  </div></Workspace>
}
