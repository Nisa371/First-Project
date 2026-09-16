import { Modal } from '../../components/Modal'
import { useState } from 'react'
import { Link } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useLoad, words } from '../marketplace/useLoad'
import { Empty, Feedback, LoadState, Stat, Workspace } from '../marketplace/shared'
interface Account { id: number; email: string; role: string; status: string }
interface Stats { users: number; activeUsers: number; jobs: number; pendingVerifications: number; activePlacements: number; replacements: number }
interface Job { id: number; title: string; company: string; location: string; status: string }
interface Verification { id: number; candidate: string; status: string; submittedAt: string }
interface Skill { id: number; name: string; category: string; active: boolean }
const load = async () => {
  const [stats, users, jobs, verifications, skills] = await Promise.all([api.get<Stats>('/admin/stats'), api.get<Account[]>('/admin/users'), api.get<Job[]>('/admin/jobs'), api.get<Verification[]>('/admin/verifications'), api.get<Skill[]>('/admin/skills')])
  return { stats: stats.data, users: users.data, jobs: jobs.data, verifications: verifications.data, skills: skills.data }
}
export function AdminPage() {
  const state = useLoad(load); const [query, setQuery] = useState(''); const [tab, setTab] = useState('Accounts')
  const [pending, setPending] = useState<{ user: Account; status: string } | null>(null)
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [success, setSuccess] = useState('')
  async function save() {
    if (!pending) return; setBusy(true); setError(''); setSuccess('')
    try { await api.patch(`/admin/users/${pending.user.id}/status`, { status: pending.status }); setSuccess(`Account updated to ${words(pending.status)}.`); setPending(null); state.reload() } catch (err) { setError(apiFailure(err).message) } finally { setBusy(false) }
  }
  const users = state.data?.users.filter(u => `${u.email} ${u.role} ${u.status}`.toLowerCase().includes(query.toLowerCase())) ?? []
  return <Workspace title="The marketplace, at a glance." subtitle="Account health, verification workload and managed hiring in one place."><Feedback error={error} success={success} /><LoadState {...state} />{state.data && <div className="space-y-8">
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"><Stat label="Active accounts" value={`${state.data.stats.activeUsers} / ${state.data.stats.users}`} /><Stat label="Jobs" value={state.data.stats.jobs} /><Stat label="Awaiting verification" value={state.data.stats.pendingVerifications} /><Stat label="Active placements" value={state.data.stats.activePlacements} /><Stat label="Replacement requests" value={state.data.stats.replacements} /><div className="surface bg-slate-900 text-white"><p className="text-emerald-300">Managed workforce</p><Link to="/admin/replacements" className="mt-3 inline-block font-bold underline underline-offset-4">View replacement activity →</Link><Link to="/admin/queue" className="mt-4 block underline underline-offset-4">Open FIFO waiting room →</Link></div></div>
    <div className="flex flex-wrap gap-2" aria-label="Admin lists">{['Accounts', 'Verifications', 'Jobs', 'Skills'].map(t => <button key={t} aria-pressed={tab === t} className={tab === t ? 'button-primary' : 'button-secondary'} onClick={() => setTab(t)}>{t}</button>)}</div>
    {tab === 'Accounts' && <section className="surface"><h2 className="text-xl font-bold">Account management</h2><label className="field-label mt-5" htmlFor="account-search">Filter by email, role or status</label><input id="account-search" className="form-input" type="search" value={query} onChange={e => setQuery(e.target.value)} />
      <div className="mt-5 divide-y divide-slate-100">{users.map(u => <article key={u.id} className="flex flex-wrap items-center justify-between gap-4 py-5"><div className="min-w-0"><h3 className="font-semibold break-all">{u.email}</h3><p className="mt-2 text-sm text-slate-600">{words(u.role)} · <span className="badge">{words(u.status)}</span></p></div>{u.role === 'ADMIN' ? <span className="text-sm text-slate-500">Protected admin</span> : <div><label className="sr-only" htmlFor={`status-${u.id}`}>Status for {u.email}</label><select id={`status-${u.id}`} className="form-input" value={u.status} disabled={busy} onChange={e => setPending({ user: u, status: e.target.value })}>{['ACTIVE', 'SUSPENDED', 'FLAGGED', 'BLOCKED'].map(s => <option key={s} value={s}>{words(s)}</option>)}</select></div>}</article>)}</div>{!users.length && <Empty title="No matching accounts">Try another name, role or status.</Empty>}
      {pending && <Modal title="Confirm account change" close={() => { if (!busy) setPending(null) }}><Feedback error={error} /><p className="mt-2 break-words">Set {pending.user.email} to {words(pending.status)}? Inactive accounts lose access immediately. You can restore access here.</p><div className="mt-4 flex gap-3"><button disabled={busy} onClick={save} className="button-primary">{busy ? 'Saving…' : 'Confirm status change'}</button><button disabled={busy} onClick={() => setPending(null)} className="button-secondary">Cancel</button></div></Modal>}
    </section>}
    {tab === 'Verifications' && <section className="surface"><h2 className="mb-4 text-xl font-bold">Verification workload</h2><p className="mb-4 text-sm text-slate-600">Platform/manual review. Evidence stays in the evaluator’s restricted review screen.</p>{state.data.verifications.map(v => <div className="flex flex-wrap justify-between gap-3 border-t border-slate-100 py-4" key={v.id}><div><p className="font-semibold">{v.candidate}</p><p className="text-sm text-slate-500">{new Date(v.submittedAt).toLocaleDateString()}</p></div><span className="badge">{words(v.status)}</span></div>)}{!state.data.verifications.length && <Empty title="No verification cases">Submitted cases will appear here.</Empty>}</section>}
    {tab === 'Jobs' && <section className="grid gap-4 sm:grid-cols-2">{state.data.jobs.map(j => <article key={j.id} className="surface"><span className="badge">{words(j.status)}</span><h2 className="mt-3 text-lg font-bold">{j.title}</h2><p className="mt-2 text-slate-600">{j.company} · {j.location}</p></article>)}{!state.data.jobs.length && <Empty title="No jobs yet">Employer job posts will appear here.</Empty>}</section>}
    {tab === 'Skills' && <section className="surface"><h2 className="mb-4 text-xl font-bold">Skill reference catalog</h2><div className="flex flex-wrap gap-3">{state.data.skills.map(s => <span key={s.id} className="badge">{s.name} · {s.category} · {s.active ? 'active' : 'inactive'}</span>)}</div>{!state.data.skills.length && <Empty title="No skills yet">The reference catalog is empty.</Empty>}</section>}
  </div>}</Workspace>
}
