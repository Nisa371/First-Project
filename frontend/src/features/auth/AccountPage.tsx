import { Link } from 'react-router'
import { useAuth } from './useAuth'

export function AccountPage() {
  const { user } = useAuth()
  if (!user) return null
  const label = user.role === 'CANDIDATE' ? `${user.candidateType} candidate` : user.role.toLowerCase()
  return <div className="mx-auto max-w-3xl">
    <p className="eyebrow">YOUR ACCOUNT</p>
    <h1 className="mt-3 break-words text-3xl font-bold tracking-tight sm:text-4xl">Welcome, {user.displayName}</h1>
    <p className="mt-4 text-slate-600">You’re signed in to your {label} workspace.</p>
    {user.role === 'ADMIN' && <nav aria-label="Managed marketplace" className="mt-6 flex flex-wrap gap-3"><Link className="button-primary" to="/admin/queue">Waiting room</Link><Link className="button-secondary" to="/admin/replacements">Replacements</Link><Link className="button-secondary" to="/admin/placements">Placements</Link><Link className="button-secondary" to="/admin/notifications">Notifications</Link></nav>}
    <section className="surface mt-8" aria-labelledby="account-details">
      <div className="flex flex-wrap items-center justify-between gap-3"><h2 id="account-details" className="text-xl font-bold">Account details</h2><span className="rounded-full bg-emerald-50 px-3 py-1 text-sm font-semibold text-emerald-800">Account active</span></div>
      <dl className="mt-6 divide-y divide-slate-100">
        <div className="account-row"><dt>Email address</dt><dd className="break-all font-medium text-slate-900">{user.email}</dd></div>
        <div className="account-row"><dt>Account role</dt><dd className="font-medium capitalize text-slate-900">{user.role.toLowerCase()}</dd></div>
        {user.candidateType && <div className="account-row"><dt>Career track</dt><dd className="font-medium text-slate-900">{user.candidateType === 'TECH' ? 'TECH · Professional careers' : 'TRADE · দক্ষ কর্মী'}</dd></div>}
      </dl>
      <p className="mt-6 rounded-xl bg-slate-50 p-4 text-sm leading-relaxed text-slate-600">{user.role === 'CANDIDATE' ? 'Your career account is ready. Account activation is separate from skill assessment and platform verification.' : 'Your account is ready. Access is determined by your assigned platform role.'}</p>
    </section>
  </div>
}
