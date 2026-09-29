import { ThemeToggle } from '../features/theme/ThemeToggle'
import { TradeTools } from '../features/trade/TradeTools'
import { tradeText, useIsTrade } from '../features/trade/useTradeText'
import { Suspense, useEffect } from 'react'
import { flushSync } from 'react-dom'
import { useAuth } from '../features/auth/useAuth'
import { AuthenticatedShell } from './AuthenticatedShell'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'

export function AppLayout() {
  const { user, logout, loading } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const publicPage = pathname === '/' || pathname === '/login' || pathname === '/register'
  const workspaceUser = publicPage ? null : user
  const tradeUser = useIsTrade()
  const isTrade = !!workspaceUser && tradeUser
  const tr = (text: string) => tradeText(text, isTrade)

  function signOut() {
    // Commit the public layout and run workspace cleanup before router navigation.
    flushSync(() => logout())
    void navigate('/login', { replace: true })
  }
  useEffect(() => { document.documentElement.lang = isTrade ? 'bn' : 'en'; document.title = isTrade ? 'আপনার কাজের পাতা · ভেরিফায়েড ক্যারিয়ার' : `${pathname === '/' ? 'Skills, trust & opportunity' : pathname.split('/').at(-1)?.replaceAll('-', ' ')} · Verified Career`; window.scrollTo(0, 0); document.getElementById('main-content')?.focus({ preventScroll: true }) }, [pathname, isTrade])
  const content = <><main id="main-content" tabIndex={-1} className={workspaceUser ? isTrade ? "flex-1 pb-44 pt-8 sm:pt-10" : "flex-1 py-8 sm:py-10" : "flex-1 py-12 sm:py-16"}><Suspense fallback={<p role="status" className="surface">{tr("Loading your workspace…")}</p>}><Outlet /></Suspense></main>
    <footer className="workspace-footer flex flex-wrap justify-between gap-3 border-t border-slate-200 py-6 text-sm text-slate-500"><span>{tr("Verified Skill & Career Managed Marketplace")}</span><span>{tr("Built for Bangladesh")}</span></footer></>
  return (
    <div key={workspaceUser ? `workspace:${workspaceUser.id}` : 'public'} data-trade-workspace={isTrade} lang={isTrade ? "bn" : "en"} onInvalidCapture={e => { if (isTrade && (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement)) e.target.setCustomValidity(e.target.validity.valueMissing ? "এই ঘরটি পূরণ করুন।" : "সঠিক তথ্য দিন। নির্ধারিত ধরন ও সীমা মেনে লিখুন।") }} onInputCapture={e => { if (isTrade && (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement)) e.target.setCustomValidity('') }} className={workspaceUser ? "app-shell min-h-screen" : "app-shell mx-auto flex min-h-screen max-w-6xl flex-col px-5 sm:px-8"}>
      <a href="#main-content" className="skip-link sr-only focus:not-sr-only focus:py-3">{tr("Skip to content")}</a>
      {workspaceUser ? <AuthenticatedShell user={workspaceUser} logout={signOut}>{content}{isTrade && <TradeTools />}</AuthenticatedShell> : <><header className="app-header flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 py-6">
        <Link to="/" aria-label={tr("Verified Career home")} className="brand flex items-center gap-3 rounded-lg">
          <span aria-hidden="true" className="brand-mark grid size-10 place-items-center rounded-xl bg-slate-900 text-xl font-bold text-white">v<span className="sr-only">Verified</span></span>
          <span className="brand-name text-lg font-bold tracking-tight">Verified<span className="brand-accent"> Career</span></span>
        </Link>
        <nav aria-label={tr("Main navigation")} className="flex flex-wrap items-center gap-3 text-sm font-semibold">
          <ThemeToggle />
          {!loading && <><Link className="header-link py-3" to="/login">Sign in</Link><Link className="button-primary" to="/register">Join now</Link></>}
        </nav>
      </header>
      {content}</>}
    </div>
  )
}
