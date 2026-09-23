import { useTradeError, useTradeText } from '../trade/useTradeText'
import type { ReactNode } from 'react'
import { NavLink } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function Workspace({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  const tr = useTradeText()

  const { user } = useAuth()
  const root = `/${user?.role.toLowerCase()}`
  const links = user?.role === 'CANDIDATE' ? [['dashboard', tr("Overview")], ['profile', tr("My profile")], ['cv', tr("CV Builder")], ['jobs', tr("Explore Jobs")], ['applications', tr("My applications")], ...(user.candidateType === 'TRADE' ? [['onboarding', 'বাংলায় শুরু করুন']] : []), ['verification', tr("Verification")], ['assessments', tr("Assessments")], ['bookings', tr("Appointments")]] : user?.role === 'EVALUATOR' ? [['dashboard', 'Work queue'], ['verifications', 'Verifications'], ['bookings', tr("Appointments")]] : user?.role === 'ADMIN' ? [['dashboard', tr("Overview")], ['queue', 'Waiting room']] : [['dashboard', tr("Overview")], ['profile', 'Company profile'], ['jobs', 'Jobs'], ['candidates', 'Applicants']]
  if (user?.role !== 'EVALUATOR') links.push(['placements', tr("Placements")], ['replacements', tr("Replacements")])
  if (user?.candidateType === 'TRADE') links.push(['queue', 'কাজের তালিকা'])
  if (user?.role === 'EMPLOYER') links.push(['verification', tr("Verification")])
  if (user?.role !== 'EMPLOYER') links.push(['training', tr("Training")])
  links.push(['notifications', tr("🔔 Notifications")])
  return <div><div className="mb-8 flex flex-wrap items-center justify-between gap-4"><p className="eyebrow">{user?.role === 'CANDIDATE' ? user.candidateType === 'TRADE' ? 'দক্ষ কর্মী · আপনার কাজের যাত্রা' : `${user.candidateType} · YOUR CAREER` : `${user?.role} · YOUR WORKSPACE`}</p><span className="badge">{user?.displayName}</span></div>
    <details className="mb-6 sm:hidden"><summary className="min-h-11 cursor-pointer rounded-xl border border-slate-200 bg-white px-4 py-3 text-sm font-semibold">{tr("Workspace menu")}</summary><nav aria-label={tr("Mobile workspace")} className="mt-3 grid grid-cols-2 gap-2">{links.map(([path, label]) => <NavLink key={path} to={`${root}/${path}`} onClick={e => e.currentTarget.closest('details')?.removeAttribute('open')} className={({ isActive }) => `workspace-link ${isActive ? 'bg-slate-900 text-white' : 'bg-white text-slate-600'}`}>{label}</NavLink>)}</nav></details>
    <nav aria-label={tr("Workspace")} className="mb-8 hidden flex-wrap gap-2 sm:flex">{links.map(([path, label]) => <NavLink key={path} to={`${root}/${path}`} className={({ isActive }) => `workspace-link ${isActive ? 'bg-slate-900 text-white' : 'bg-white text-slate-600 hover:bg-slate-100'}`}>{label}</NavLink>)}</nav>
    <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{title}</h1><p className="mb-8 mt-3 max-w-2xl leading-relaxed text-slate-600">{subtitle}</p>{children}</div>
}
export function LoadState({ loading, error, reload }: { loading: boolean; error: string; reload: () => void }) {
  const tr = useTradeText()

  const errorText = useTradeError()
  if (loading) return <div role="status" className="surface animate-pulse"><div className="h-6 w-1/3 rounded bg-slate-200" /><div className="mt-5 h-28 rounded-xl bg-slate-100" /><p className="mt-4 text-sm text-slate-500">{tr("Loading your workspace…")}</p></div>
  if (error) return <div role="alert" className="surface"><p className="text-rose-700">{errorText(error)}</p><button className="button-primary mt-4" onClick={reload}>{tr("Try again")}</button></div>
  return null
}
export function Feedback({ error, success }: { error?: string; success?: string }) {
  const errorText = useTradeError(); const tr = useTradeText()
  return <>{error && <p role="alert" className="mb-5 rounded-xl bg-rose-50 p-4 text-rose-800">{errorText(error)}</p>}{success && <p role="status" className="mb-5 rounded-xl bg-emerald-50 p-4 text-emerald-800">{tr(success)}</p>}</>
}
export function Field({ name, label, value, onChange, required = false, maxLength = 255, multiline = false, type = 'text', error }: { name: string; label: string; value: string | null | undefined; onChange: (value: string) => void; required?: boolean; maxLength?: number; multiline?: boolean; type?: string; error?: string }) {
  const tr = useTradeText(); const errorText = useTradeError()
  const props = { id: name, name, value: value ?? '', onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(e.target.value), required, maxLength, className: 'form-input', 'aria-invalid': Boolean(error), 'aria-describedby': error ? `${name}-error` : undefined }
  return <div><label className="field-label" htmlFor={name}>{tr(label)}{' '}{required && ' *'}</label>{multiline ? <textarea {...props} rows={4} /> : <input {...props} type={type} />}{error && <p id={`${name}-error`} className="mt-2 text-sm text-rose-700">{errorText(error)}</p>}</div>
}
export function Empty({ title, children }: { title: string; children: ReactNode }) { return <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center"><h2 className="font-bold">{title}</h2><div className="mt-3 text-sm leading-relaxed text-slate-600">{children}</div></div> }
export function Stat({ label, value }: { label: string; value: string | number }) { const tr = useTradeText(); return <div className="surface"><p className="text-sm text-slate-500">{label}</p><p className="mt-3 text-3xl font-bold tracking-tight">{typeof value === 'string' ? tr(value) : value}</p></div> }
