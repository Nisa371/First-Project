import { CompanyTypeSelector, type CompanyType } from '../company-types/CompanyTypeSelector'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useAuth } from './useAuth'
import { dashboardPath, type AuthResponse, type CandidateType } from './types'

function SignInText({ en, bn, heading = false }: { en: string; bn: string; heading?: boolean }) {
  return <span className="inline-block"><span className="block">{en}</span><span lang="bn" className={`block font-normal normal-case tracking-normal opacity-80 ${heading ? 'mt-1 text-base leading-snug' : 'text-xs leading-snug'}`}>{bn}</span></span>
}

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const registering = mode === 'register'
  const auth = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [params, setParams] = useSearchParams()
  const accountType = params.get('account') === 'employer' ? 'EMPLOYER' : 'CANDIDATE'
  const track: CandidateType = params.get('track') === 'TRADE' ? 'TRADE' : 'TECH'
  function selectMode(key: 'account' | 'track', value: string) {
    setParams(previous => { const next = new URLSearchParams(previous); next.set(key, value); return next }, { replace: true, state: location.state })
    setFields({}); setError('')
  }
  const [showPassword, setShowPassword] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [companyType, setCompanyType] = useState<CompanyType>()
  const [customCompanyType, setCustomCompanyType] = useState('')
  const trade = registering && accountType === 'CANDIDATE' && track === 'TRADE'
  const formRef = useRef<HTMLFormElement>(null)
  useEffect(() => {
    formRef.current?.querySelectorAll('input').forEach(input => input.setCustomValidity(''))
  }, [trade])
  const tr = (en: string, bn: string) => trade ? bn : en
  const formText = (en: string, bn: string) => registering ? tr(en, bn) : <SignInText en={en} bn={bn} />
  const errorText = (message: string, field?: string) => {
    if (!trade) return message
    const messages: Record<string, string> = {
      'Please enter a name.': 'আপনার নাম লিখুন।',
      'Use a password of at most 72 UTF-8 bytes.': 'পাসওয়ার্ডের আকার সর্বোচ্চ ৭২ বাইট হতে পারে। ছোট পাসওয়ার্ড দিন।',
      'Passwords do not match.': 'পাসওয়ার্ড দুটি মিলছে না।',
    }
    if (messages[message]) return messages[message]
    if (field === 'identifier') return 'ইমেইল অথবা ফোন নম্বরটি যাচাই করুন। আগে ব্যবহার করা হয়ে থাকলে প্রবেশ করুন।'
    if (field === 'password') return 'পাসওয়ার্ডটি যাচাই করুন। নিবন্ধনের জন্য কমপক্ষে ৮ অক্ষর এবং সর্বোচ্চ ৭২ বাইট ব্যবহার করুন।'
    if (field) return 'এই ঘরের তথ্য যাচাই করে আবার চেষ্টা করুন।'
    return 'কাজটি সম্পূর্ণ হয়নি। আপনার তথ্য ও ইন্টারনেট সংযোগ যাচাই করে আবার চেষ্টা করুন।'
  }
  if (auth.loading) return <p role="status" className="surface text-center">{formText('Checking your session…', 'আপনার প্রবেশের অবস্থা যাচাই করা হচ্ছে…')}</p>
  if (auth.user) return <Navigate to={dashboardPath(auth.user.role)} replace />
  if (auth.error) return <div className="surface mx-auto max-w-lg"><h1 className="text-2xl font-bold">{formText('Let’s reconnect', 'আবার সংযোগ করুন')}</h1><p role="alert" className="my-5">{errorText(auth.error)}</p><button className="button-primary" onClick={auth.retry}>{formText('Try again', 'আবার চেষ্টা করুন')}</button><button className="ml-4 min-h-11 underline" onClick={auth.logout}>{formText('Return to sign in', 'প্রবেশের পাতায় ফিরুন')}</button></div>

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const data = new FormData(event.currentTarget)
    const identifier = String(data.get('identifier') ?? '').trim()
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
      const body = registering ? { identifier, password, accountType,
        ...(accountType === 'CANDIDATE' ? { candidateType: track, fullName: name } : { companyName: name, companyTypeId: companyType?.id, customCompanyType: companyType?.other ? customCompanyType.trim() : null }) } : { identifier, password }
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
  const fieldError = (name: string) => fields[name] ? <span id={`${name}-error`} className="mt-1 block text-sm text-rose-700">{errorText(fields[name], name)}</span> : null
  return <div className="auth-panel grid overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm lg:grid-cols-[0.85fr_1.15fr]">
    <aside className="relative bg-slate-900 p-6 text-white sm:p-10 lg:p-12">
      <span className="inline-flex rounded-full border border-teal-300/30 bg-teal-300/10 px-3 py-1 text-xs font-semibold text-teal-200">{tr('Verified Career', 'ভেরিফায়েড ক্যারিয়ার')}</span>
      <h2 className="mt-4 max-w-sm text-2xl font-semibold leading-tight lg:mt-7 lg:text-4xl">{tr('A place for skills.', 'দক্ষতার পরিচয়।')}<br />{tr('A path to work.', 'কাজের পথ।')}</h2>
      <p className="mt-5 hidden max-w-sm leading-relaxed text-slate-300 lg:block">{tr('A career community for Bangladesh’s professionals, skilled workers and employers.', 'বাংলাদেশের পেশাজীবী, দক্ষ কর্মী ও নিয়োগকর্তাদের কাজের ঠিকানা।')}</p>
      <div className="mt-9 hidden gap-4 lg:grid">
        <div className="rounded-2xl border border-white/15 p-4"><p className="text-sm font-bold text-teal-200">{tr('Tech and Corporate', 'প্রযুক্তি ও কর্পোরেট')}</p><p className="mt-1 text-sm text-slate-300">{tr('Students, developers and engineers.', 'শিক্ষার্থী, সফটওয়্যার নির্মাতা ও প্রকৌশলীদের জন্য।')}</p></div>
        <div className="rounded-2xl border border-white/15 p-4"><p className="text-sm font-bold text-teal-200">{tr('TRADE · Skilled workforce', 'দক্ষ কর্মীদের জন্য')}</p><p className="mt-1 text-sm text-slate-300">{tr('Your skills, your identity.', 'আপনার দক্ষতা, আপনার পরিচয়।')}</p></div>
      </div>
      <p className="mt-8 hidden text-xs leading-relaxed text-slate-400 lg:block">{tr('Platform/manual verification.', 'প্ল্যাটফর্মের মাধ্যমে নথি ও তথ্য যাচাই করা হয়।')}<br />{tr('Creating an account does not confer verified status.', 'শুধু অ্যাকাউন্ট খুললেই পরিচয় যাচাই সম্পন্ন হয় না।')}</p>
    </aside>
    <section className="p-6 sm:p-10 lg:p-12" aria-labelledby="auth-title">
      <p className="eyebrow">{registering ? tr('GET STARTED', 'শুরু করুন') : <SignInText en="WELCOME BACK" bn="স্বাগতম" />}</p>
      <h1 id="auth-title" className="mt-3 text-3xl font-bold tracking-tight">{registering ? tr('Create your account', 'আপনার অ্যাকাউন্ট তৈরি করুন') : <SignInText en="Sign in to your account" bn="আপনার অ্যাকাউন্টে প্রবেশ করুন" heading />}</h1>
      <p className="mt-3 text-sm text-slate-600">{registering ? tr('Choose how you’ll use Verified Career.', 'কীভাবে ভেরিফায়েড ক্যারিয়ার ব্যবহার করবেন, বেছে নিন।') : <SignInText en="Pick up where you left off." bn="যেখান থেকে শেষ করেছিলেন, সেখান থেকেই আবার শুরু করুন।" />}</p>
      {auth.expired && <p role="status" className="mt-5 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">{formText('Your session ended. Please sign in again.', 'আপনার প্রবেশের মেয়াদ শেষ হয়েছে। আবার প্রবেশ করুন।')}</p>}
      <form ref={formRef} onSubmit={submit} className="mt-7 space-y-5">
        <fieldset disabled={busy} className="space-y-5 disabled:opacity-70">
          {registering && <fieldset><legend className="field-label">{tr('I’m here as a', 'আমি নিবন্ধন করছি')}</legend><div className="grid grid-cols-2 gap-3">{(['CANDIDATE','EMPLOYER'] as const).map(type => <label key={type} className={`choice-card ${accountType === type ? 'choice-selected' : ''}`}><input type="radio" name="accountType" value={type} checked={accountType === type} onChange={() => { selectMode('account', type.toLowerCase()) }} className="accent-indigo-600" /><span>{type === 'CANDIDATE' ? tr('Candidate', 'চাকরিপ্রার্থী') : tr('Employer', 'নিয়োগকর্তা')}</span></label>)}</div></fieldset>}
            {registering && accountType === 'CANDIDATE' && <fieldset><legend className="field-label">{tr('Choose your career track', 'আপনার কাজের ধরন বেছে নিন')}</legend><div className="grid grid-cols-2 gap-3">{(['TECH','TRADE'] as const).map(type => <label key={type} className={`choice-card items-start ${track === type ? 'choice-selected' : ''}`}><input type="radio" name="track" checked={track === type} onChange={() => selectMode('track', type)} className="mt-1 accent-indigo-600" /><span>{type === 'TECH' ? tr('Tech and Corporate', 'প্রযুক্তি ও কর্পোরেট') : tr('Trade', 'দক্ষ কর্মী')}<span className="mt-1 block text-xs font-normal text-slate-600">{type === 'TECH' ? tr('Professional careers', 'পেশাজীবীদের জন্য') : tr('Skilled workforce', 'দক্ষতাভিত্তিক কাজ')}</span></span></label>)}</div></fieldset>}
          {registering && <>


            <label className="block"><span id="name-label" className="field-label">{accountType === 'EMPLOYER' ? 'Company name' : tr('Full name', 'পুরো নাম')}</span><input key={nameField} name="name" aria-labelledby="name-label" autoComplete={accountType === 'EMPLOYER' ? 'organization' : 'name'} required maxLength={accountType === 'EMPLOYER' ? 200 : 160} className="form-input" aria-invalid={!!fields[nameField]} aria-describedby={fields[nameField] ? `${nameField}-error` : undefined} />{fieldError(nameField)}</label>
            {accountType === 'EMPLOYER' && <CompanyTypeSelector value={companyType?.id ?? null} custom={customCompanyType} selected={companyType} error={fields.companyTypeId || fields.customCompanyType} onChange={(type, custom) => { setCompanyType(type); setCustomCompanyType(custom) }} />}
          </>}
          <label className="block"><span id="identifier-label" className="field-label">{formText('Email or phone', 'ইমেইল অথবা ফোন নম্বর')}</span><input name="identifier" aria-labelledby="identifier-label" type="text" autoComplete="username" autoCapitalize="none" spellCheck={false} required maxLength={254} className="form-input" placeholder={tr('you@example.com or 01XXXXXXXXX', 'আপনার ইমেইল অথবা ফোন নম্বর')} aria-invalid={!!fields.identifier} aria-describedby={fields.identifier ? 'identifier-error' : undefined} />{fieldError('identifier')}</label>
          <div><label htmlFor="password" className="field-label">{formText('Password', 'পাসওয়ার্ড')}</label><div className="relative"><input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete={registering ? 'new-password' : 'current-password'} required minLength={registering ? 8 : undefined} maxLength={72} className="form-input pr-20" aria-invalid={!!fields.password} aria-describedby={fields.password ? 'password-error' : registering ? 'password-hint' : undefined} /><button type="button" onClick={() => setShowPassword(!showPassword)} aria-label={registering ? (showPassword ? tr('Hide password', 'পাসওয়ার্ড লুকান') : tr('Show password', 'পাসওয়ার্ড দেখুন')) : (showPassword ? 'Hide password, পাসওয়ার্ড লুকান' : 'Show password, পাসওয়ার্ড দেখুন')} aria-pressed={showPassword} className="absolute inset-y-0 right-0 min-w-16 rounded-r-xl px-3 text-sm font-semibold text-indigo-700">{showPassword ? formText('Hide', 'লুকান') : formText('Show', 'দেখুন')}</button></div>{fieldError('password')}{registering && <p id="password-hint" className="mt-2 text-xs text-slate-500">{tr('Use at least 8 characters.', 'কমপক্ষে ৮ অক্ষরের পাসওয়ার্ড দিন।')}</p>}</div>
          {registering && <label className="block"><span id="confirm-label" className="field-label">{tr('Confirm password', 'পাসওয়ার্ড নিশ্চিত করুন')}</span><input name="confirmPassword" aria-labelledby="confirm-label" type={showPassword ? 'text' : 'password'} autoComplete="new-password" required maxLength={72} className="form-input" aria-invalid={!!fields.confirmPassword} aria-describedby={fields.confirmPassword ? 'confirmPassword-error' : undefined} />{fieldError('confirmPassword')}</label>}
          {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{errorText(error)}</div>}
          <button type="submit" disabled={busy} className="button-primary w-full" aria-busy={busy}>{busy ? formText('Please wait…', 'অপেক্ষা করুন…') : registering ? tr('Create account', 'অ্যাকাউন্ট তৈরি করুন') : formText('Sign in', 'প্রবেশ করুন')}<span aria-hidden="true">→</span></button>
        </fieldset>
      </form>
      <p className={registering ? 'mt-6 text-center text-sm text-slate-600' : 'mt-6 flex flex-wrap items-center justify-center gap-x-3 gap-y-2 text-center text-sm text-slate-600'}>{registering ? tr('Already have an account?', 'আগেই অ্যাকাউন্ট খুলেছেন?') : <SignInText en="New to Verified Career?" bn="নতুন ব্যবহারকারী?" />} <Link className="font-semibold text-indigo-700 underline-offset-4 hover:underline" to={registering ? '/login' : '/register'}>{registering ? tr('Sign in', 'প্রবেশ করুন') : <SignInText en="Create an account" bn="অ্যাকাউন্ট তৈরি করুন" />}</Link></p>
    </section>
  </div>
}
