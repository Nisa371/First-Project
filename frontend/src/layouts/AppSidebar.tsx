import { Link, useLocation } from 'react-router'
import { useTradeText } from '../features/trade/useTradeText'
import { navigationActive, type NavigationItem } from './navigation'
import { NavigationIcon } from './NavigationIcon'

export function AppSidebar({ items, unread, collapsed, onToggle, onNavigate, mobile = false }: { items: NavigationItem[]; unread: number | null | undefined; collapsed: boolean; onToggle: () => void; onNavigate?: () => void; mobile?: boolean }) {
  const tr = useTradeText()
  const { pathname, search } = useLocation()
  return <div className={`sidebar-panel ${collapsed ? 'is-collapsed' : ''}`}>
    <div className="sidebar-brand"><Link to="/" onClick={onNavigate} aria-label={tr('Verified Career home')} title={tr('Verified Career home')} className="brand flex min-w-0 items-center gap-3 rounded-lg"><span aria-hidden="true" className="brand-mark grid size-10 shrink-0 place-items-center rounded-xl text-xl font-bold text-white">v</span>{!collapsed && <span className="brand-name whitespace-nowrap">{tr('Verified')}<span className="brand-accent"> {tr('Career')}</span></span>}</Link>{mobile && <button type="button" className="sidebar-control" aria-label={tr('Close menu')} onClick={onNavigate}><NavigationIcon name="close" /></button>}</div>
    <nav aria-label={tr('Workspace')} className="sidebar-links">{items.map(item => {
      const active = navigationActive(item, pathname, search)
      const label = tr(item.label)
      const badge = item.icon === 'bell' && unread != null && unread > 0
      return <Link key={item.path} to={item.path} onClick={onNavigate} className="sidebar-link" aria-current={active ? 'page' : undefined} aria-label={badge ? `${label}, ${unread} ${tr('unread updates')}` : label} title={collapsed ? label : undefined}><span className="sidebar-icon"><NavigationIcon name={item.icon} />{collapsed && badge && <span aria-hidden="true" className="sidebar-badge">{unread > 99 ? '99+' : unread}</span>}</span>{!collapsed && <><span className="min-w-0 flex-1">{label}</span>{badge && <span aria-hidden="true" className="sidebar-badge">{unread > 99 ? '99+' : unread}</span>}</>}</Link>
    })}</nav>
    {!mobile && <button type="button" className="sidebar-link sidebar-collapse" onClick={onToggle} aria-label={tr(collapsed ? 'Expand sidebar' : 'Collapse sidebar')} title={tr(collapsed ? 'Expand sidebar' : 'Collapse sidebar')} aria-expanded={!collapsed}><NavigationIcon name={collapsed ? 'right' : 'left'} />{!collapsed && <span>{tr('Collapse sidebar')}</span>}</button>}
  </div>
}
