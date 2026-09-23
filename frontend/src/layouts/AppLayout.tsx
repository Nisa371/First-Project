import { ThemeToggle } from '../features/theme/ThemeToggle'
import { TradeTools } from '../features/trade/TradeTools'
import { useIsTrade, useTradeText } from '../features/trade/useTradeText'
import { Suspense, useEffect } from 'react'
import { useAuth } from '../features/auth/useAuth'
import { dashboardPath } from '../features/auth/types'
import { Link, Outlet, useLocation } from 'react-router'

export function AppLayout() {
  const tr = useTradeText()
  const isTrade = useIsTrade()

  const { user, logout, loading } = useAuth()
  const { pathname } = useLocation()
  useEffect(() => { document.documentElement.lang = isTrade ? 'bn' : 'en'; document.title = isTrade ? 'আপনার কাজের পাতা · ভেরিফায়েড ক্যারিয়ার' : `${pathname === '/' ? 'Skills, trust & opportunity' : pathname.split('/').at(-1)?.replaceAll('-', ' ')} · Verified Career`; window.scrollTo(0, 0); document.getElementById('main-content')?.focus({ preventScroll: true }) }, [pathname, isTrade])
  return (
    <div data-trade-workspace={isTrade} lang={isTrade ? "bn" : "en"} onInvalidCapture={e => { if (isTrade && (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement)) e.target.setCustomValidity(e.target.validity.valueMissing ? "এই ঘরটি পূরণ করুন।" : "সঠিক তথ্য দিন। নির্ধারিত ধরন ও সীমা মেনে লিখুন।") }} onInputCapture={e => { if (isTrade && (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement || e.target instanceof HTMLSelectElement)) e.target.setCustomValidity('') }} className="mx-auto flex min-h-screen max-w-6xl flex-col px-5 sm:px-8">
      <a href="#main-content" className="sr-only focus:not-sr-only focus:py-3">{tr("Skip to content")}</a>
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 py-6">
        <Link to="/" aria-label={tr("Verified Career home")} className="flex items-center gap-3 rounded-lg">
          <span aria-hidden="true" className="grid size-10 place-items-center rounded-xl bg-slate-900 text-xl font-bold text-white">v<span className="sr-only">Verified</span></span>
          <span className="text-lg font-bold tracking-tight">Verified<span className="font-normal text-slate-500"> Career</span></span>
        </Link>
        <nav aria-label={tr("Main navigation")} className="flex flex-wrap items-center gap-3 text-sm font-semibold">
          <ThemeToggle />
          {user ? <><Link className="py-3 text-indigo-700" to={dashboardPath(user.role)}>{tr("Dashboard")}</Link><button onClick={logout} className="min-h-11 rounded-xl border border-slate-200 bg-white px-4">{tr("Sign out")}</button></> : !loading && <><Link className="py-3" to="/login">Sign in</Link><Link className="button-primary" to="/register">Join now</Link></>}
        </nav>
      </header>
      <main id="main-content" tabIndex={-1} className={isTrade ? "flex-1 pb-44 pt-12 sm:pt-16" : "flex-1 py-12 sm:py-16"}><Suspense fallback={<p role="status" className="surface">{tr("Loading your workspace…")}</p>}><Outlet /></Suspense></main>
      <footer className="flex flex-wrap justify-between gap-3 border-t border-slate-200 py-6 text-sm text-slate-500"><span>{tr("Verified Skill & Career Managed Marketplace")}</span><span>{tr("Built for Bangladesh")}</span></footer>
      {isTrade && <TradeTools key={user?.id} />}
    </div>
  )
}
