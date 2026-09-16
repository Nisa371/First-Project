import { Suspense, useEffect } from 'react'
import { useAuth } from '../features/auth/useAuth'
import { dashboardPath } from '../features/auth/types'
import { Link, Outlet, useLocation } from 'react-router'

export function AppLayout() {
  const { user, logout, loading } = useAuth()
  const { pathname } = useLocation()
  useEffect(() => { document.title = `${pathname === '/' ? 'Skills, trust & opportunity' : pathname.split('/').at(-1)?.replaceAll('-', ' ')} · Verified Career`; window.scrollTo(0, 0); document.getElementById('main-content')?.focus({ preventScroll: true }) }, [pathname])
  return (
    <div className="mx-auto flex min-h-screen max-w-6xl flex-col px-5 sm:px-8">
      <a href="#main-content" className="sr-only focus:not-sr-only focus:py-3">Skip to content</a>
      <header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 py-6">
        <Link to="/" aria-label="Verified Career home" className="flex items-center gap-3 rounded-lg">
          <span aria-hidden="true" className="grid size-10 place-items-center rounded-xl bg-slate-900 text-xl font-bold text-white">v<span className="sr-only">Verified</span></span>
          <span className="text-lg font-bold tracking-tight">Verified<span className="font-normal text-slate-500"> Career</span></span>
        </Link>
        <nav aria-label="Main navigation" className="flex items-center gap-4 text-sm font-semibold">
          {user ? <><Link className="py-3 text-indigo-700" to={dashboardPath(user.role)}>Dashboard</Link><button onClick={logout} className="min-h-11 rounded-xl border border-slate-200 bg-white px-4">Sign out</button></> : !loading && <><Link className="py-3" to="/login">Sign in</Link><Link className="button-primary" to="/register">Join now</Link></>}
        </nav>
      </header>
      <main id="main-content" tabIndex={-1} className="flex-1 py-12 sm:py-16"><Suspense fallback={<p role="status" className="surface">Loading your workspace…</p>}><Outlet /></Suspense></main>
      <footer className="flex flex-wrap justify-between gap-3 border-t border-slate-200 py-6 text-sm text-slate-500"><span>Verified Skill & Career Managed Marketplace</span><span>Built for Bangladesh</span></footer>
    </div>
  )
}
