import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useLocation } from 'react-router'
import type { CurrentUser } from '../features/auth/types'
import { useTradeText } from '../features/trade/useTradeText'
import { ThemeToggle } from '../features/theme/ThemeToggle'
import { useUnreadNotifications } from '../features/placement/useUnreadNotifications'
import { AppSidebar } from './AppSidebar'
import { navigationActive, navigationFor } from './navigation'
import { NavigationIcon } from './NavigationIcon'

const preferenceKey = 'verified-career:sidebar-collapsed'
export function AuthenticatedShell({ user, logout, children }: { user: CurrentUser; logout: () => void; children: ReactNode }) {
  const tr = useTradeText()
  const unread = useUnreadNotifications()
  const location = useLocation()
  const items = navigationFor(user.role)
  const [collapsed, setCollapsed] = useState(() => { try { return localStorage.getItem(preferenceKey) === 'true' } catch { return false } })
  const [mobileOpen, setMobileOpen] = useState(false)
  const drawer = useRef<HTMLDialogElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  const current = items.find(item => navigationActive(item, location.pathname, location.search))
  function close() { setMobileOpen(false); trigger.current?.focus() }
  useEffect(() => {
    if (!mobileOpen) return
    const dialog = drawer.current
    dialog?.showModal()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    const desktop = window.matchMedia('(min-width: 1024px)')
    const resize = () => { if (desktop.matches) setMobileOpen(false) }
    desktop.addEventListener('change', resize)
    return () => { dialog?.close(); document.body.style.overflow = previousOverflow; desktop.removeEventListener('change', resize) }
  }, [mobileOpen])
  useEffect(() => { setMobileOpen(false) }, [location.key])
  function toggle() {
    const next = !collapsed
    setCollapsed(next)
    try { localStorage.setItem(preferenceKey, String(next)) } catch { /* Keep navigation usable when storage is unavailable. */ }
  }
  return <div className={`authenticated-shell ${collapsed ? 'sidebar-is-collapsed' : ''}`}>
    <aside className="desktop-sidebar"><AppSidebar unread={unread} items={items} collapsed={collapsed} onToggle={toggle} /></aside>
    <div className="authenticated-content">
      <header className="workspace-topbar">
        <div className="flex min-w-0 items-center gap-3"><button ref={trigger} type="button" className="sidebar-control mobile-menu-button" aria-label={tr('Open menu')} aria-expanded={mobileOpen} aria-controls="workspace-drawer" onClick={() => setMobileOpen(true)}><NavigationIcon name="menu" /></button><span className="truncate text-sm font-semibold">{tr(current?.label ?? 'Workspace')}</span></div>
        <div className="flex items-center gap-2 sm:gap-3"><span className="hidden max-w-48 truncate text-sm text-slate-600 sm:block" title={user.displayName}>{user.displayName}</span><ThemeToggle /><button onClick={logout} className="button-secondary">{tr('Sign out')}</button></div>
      </header>
      <div className="workspace-content">{children}</div>
    </div>
    <dialog ref={drawer} id="workspace-drawer" className="sidebar-drawer" aria-label={tr('Workspace menu')} onCancel={event => { event.preventDefault(); close() }} onClick={event => { if (event.target === event.currentTarget) close() }}>
      {mobileOpen && <AppSidebar unread={unread} items={items} collapsed={false} onToggle={toggle} onNavigate={close} mobile />}
    </dialog>
  </div>
}
