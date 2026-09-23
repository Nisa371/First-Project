import { CompanyTypeSelector, type CompanyType } from '../company-types/CompanyTypeSelector'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useAuth } from './useAuth'
import { dashboardPath, type AuthResponse, type CandidateType } from './types'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const registering = mode === 'register'
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [accountType, setAccountType] = useState<'CANDIDATE' | 'EMPLOYER'>(() => new URLSearchParams(location.search).get('account') === 'employer' ? 'EMPLOYER' : 'CANDIDATE')
  const [track, setTrack] = useState<CandidateType>(() => new URLSearchParams(location.search).get('track') === 'TRADE' ? 'TRADE' : 'TECH')
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [companyType, setCompanyType] = useState<CompanyType>()
  const [customCompanyType, setCustomCompanyType] = useState('')
  const trade = registering && accountType === 'CANDIDATE' && track === 'TRADE'
  if (auth.loading) return <p role="status" className="surface text-center">Checking your session…</p>
  if (auth.user) return <Navigate to={dashboardPath(auth.user.role)} replace />
  if (auth.error) return <div className="surface mx-auto max-w-lg"><h1 className="text-2xl font-bold">Let’s reconnect</h1><p role="alert" className="my-5">{auth.error}</p><button className="button-primary" onClick={auth.retry}>Try again</button><button className="ml-4 min-h-11 underline" onClick={auth.logout}>Return to sign in</button></div>

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const email = String(data.get('email') ?? '').trim()
    const password = String(data.get('password') ?? '')
    const name = String(data.get('name') ?? '').trim()
    const validation: Record<string, string> = {}
    if (registering && !name) validation[accountType === 'CANDIDATE' ? 'fullName' : 'companyName'] = 'Please enter a name.'
    if (new TextEncoder().encode(password).length > 72) validation.password = 'Use a password of at most 72 UTF-8 bytes.'
    if (registering && password !== data.get('confirmPassword')) validation.confirmPassword = 'Passwords do not match.'
    if (registering && accountType === 'EMPLOYER' && !companyType) validation.companyTypeId = 'Select a company type.'
    setFields(validation); setError('')
    if (Object.keys(validation).length) return
    setBusy(true)
    try {
      const body = registering ? { email, password, accountType,
        ...(accountType === 'CANDIDATE' ? { candidateType: track, fullName: name } : { companyName: name, companyTypeId: companyType?.id, customCompanyType: companyType?.other ? customCompanyType.trim() : null }) } : { email, password }
      const { data: response } = await api.post<AuthResponse>(`/auth/${mode}`, body)
      auth.accept(response)
      const home = dashboardPath(response.user.role)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from?.startsWith(`/${response.user.role.toLowerCase()}/`) ? from : home, { replace: true })
    } catch (cause) {
      const failure = apiFailure(cause)
      setError(failure.message); setFields(failure.fieldErrors ?? {})
    } finally { setBusy(false) }
  }
  const nameField = accountType === 'CANDIDATE' ? 'fullName' : 'companyName'
  const fieldError = (name: string) => fields[name] ? <span id={`${name}-error`} className="mt-1 block text-sm text-rose-700">{fields[name]}</span> : null
  return <div className="grid overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm lg:grid-cols-[0.85fr_1.15fr]">
    <aside className="relative bg-slate-900 p-6 text-white sm:p-10 lg:p-12">
      <span className="inline-flex rounded-full border border-teal-300/30 bg-teal-300/10 px-3 py-1 text-xs font-semibold text-teal-200">A better path to opportunity</span>
      <h2 className="mt-4 max-w-sm text-2xl font-semibold leading-tight lg:mt-7 lg:text-4xl">Your skills.<br />Your next chapter.</h2>
      <p className="mt-5 hidden max-w-sm leading-relaxed text-slate-300 lg:block">A career community for Bangladesh’s professionals, skilled workers and employers.</p>
      <div className="mt-9 hidden gap-4 lg:grid">
        <div className="rounded-2xl border border-white/15 p-4"><p className="text-sm font-bold text-teal-200">TECH · Professional talent</p><p className="mt-1 text-sm text-slate-300">Students, developers and engineers.</p></div>
        <div className="rounded-2xl border border-white/15 p-4"><p className="text-sm font-bold text-teal-200">TRADE · Skilled workforce</p><p className="mt-1 text-sm text-slate-300" lang="bn">আপনার দক্ষতা, আপনার পরিচয়।</p></div>
      </div>
      <p className="mt-8 hidden text-xs leading-relaxed text-slate-400 lg:block">University showcase · Platform/manual verification.<br />Creating an account does not confer verified status.</p>
    </aside>
    <section className="p-6 sm:p-10 lg:p-12" aria-labelledby="auth-title">
      <p className="eyebrow">{registering ? 'GET STARTED' : 'WELCOME BACK'}</p>
      <h1 id="auth-title" className="mt-3 text-3xl font-bold tracking-tight">{registering ? 'Create your account' : 'Sign in to your account'}</h1>
      <p className="mt-3 text-sm text-slate-600">{registering ? 'Choose how you’ll use Verified Career.' : 'Pick up where you left off.'}</p>
      {auth.expired && <p role="status" className="mt-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">Your session ended. Please sign in again.</p>}
      <form onSubmit={submit} className="mt-7 space-y-5">
        <fieldset disabled={busy} className="space-y-5 disabled:opacity-70">
          {registering && <>
            <fieldset><legend className="field-label">I’m here as a</legend><div className="grid grid-cols-2 gap-3">{(['CANDIDATE','EMPLOYER'] as const).map(type => <label key={type} className={`choice-card ${accountType === type ? 'choice-selected' : ''}`}><input type="radio" name="accountType" value={type} checked={accountType === type} onChange={() => { setAccountType(type); setFields({}); setError('') }} className="accent-indigo-600" /><span>{type === 'CANDIDATE' ? 'Candidate' : 'Employer'}</span></label>)}</div></fieldset>
            {accountType === 'CANDIDATE' && <fieldset><legend className="field-label">Choose your career track</legend><div className="grid grid-cols-2 gap-3">{(['TECH','TRADE'] as const).map(type => <label key={type} className={`choice-card items-start ${track === type ? 'choice-selected' : ''}`}><input type="radio" name="track" checked={track === type} onChange={() => setTrack(type)} className="mt-1 accent-indigo-600" /><span>{type}<span className="mt-1 block text-xs font-normal text-slate-600">{type === 'TECH' ? 'Professional careers' : 'দক্ষ কর্মী'}</span></span></label>)}</div></fieldset>}
            <label className="block"><span id="name-label" className="field-label">{accountType === 'EMPLOYER' ? 'Company name' : trade ? 'Full name / পুরো নাম' : 'Full name'}</span><input key={nameField} name="name" aria-labelledby="name-label" autoComplete={accountType === 'EMPLOYER' ? 'organization' : 'name'} required maxLength={accountType === 'EMPLOYER' ? 200 : 160} className="form-input" aria-invalid={!!fields[nameField]} aria-describedby={fields[nameField] ? `${nameField}-error` : undefined} />{fieldError(nameField)}</label>
            {accountType === 'EMPLOYER' && <CompanyTypeSelector value={companyType?.id ?? null} custom={customCompanyType} selected={companyType} error={fields.companyTypeId || fields.customCompanyType} onChange={(type, custom) => { setCompanyType(type); setCustomCompanyType(custom) }} />}
          </>}
          <label className="block"><span id="email-label" className="field-label">{trade ? 'Email / ইমেইল' : 'Email address'}</span><input name="email" aria-labelledby="email-label" type="email" autoComplete="email" required maxLength={254} className="form-input" placeholder="you@example.com" aria-invalid={!!fields.email} aria-describedby={fields.email ? 'email-error' : undefined} />{fieldError('email')}</label>
          <div><label htmlFor="password" className="field-label">{trade ? 'Password / পাসওয়ার্ড' : 'Password'}</label><div className="relative"><input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete={registering ? 'new-password' : 'current-password'} required minLength={registering ? 8 : undefined} maxLength={72} className="form-input pr-20" aria-invalid={!!fields.password} aria-describedby={fields.password ? 'password-error' : registering ? 'password-hint' : undefined} /><button type="button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Hide password' : 'Show password'} aria-pressed={showPassword} className="absolute inset-y-0 right-0 min-w-16 rounded-r-xl px-3 text-sm font-semibold text-indigo-700">{showPassword ? 'Hide' : 'Show'}</button></div>{fieldError('password')}{registering && <p id="password-hint" className="mt-2 text-xs text-slate-500">Use at least 8 characters.</p>}</div>
          {registering && <label className="block"><span id="confirm-label" className="field-label">Confirm password</span><input name="confirmPassword" aria-labelledby="confirm-label" type={showPassword ? 'text' : 'password'} autoComplete="new-password" required maxLength={72} className="form-input" aria-invalid={!!fields.confirmPassword} aria-describedby={fields.confirmPassword ? 'confirmPassword-error' : undefined} />{fieldError('confirmPassword')}</label>}
          {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{error}</div>}
          <button type="submit" disabled={busy} className="button-primary w-full" aria-busy={busy}>{busy ? 'Please wait…' : registering ? trade ? 'Create account / অ্যাকাউন্ট খুলুন' : 'Create account' : 'Sign in'}<span aria-hidden="true">→</span></button>
        </fieldset>
      </form>
      <p className="mt-6 text-center text-sm text-slate-600">{registering ? 'Already have an account?' : 'New to Verified Career?'} <Link className="font-semibold text-indigo-700 underline-offset-4 hover:underline" to={registering ? '/login' : '/register'}>{registering ? 'Sign in' : 'Create an account'}</Link></p>
    </section>
  </div>
}
