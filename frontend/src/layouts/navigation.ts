import type { Role } from '../features/auth/types'

export interface NavigationItem { label: string; path: string; icon: NavigationIcon }
export type NavigationIcon = 'home' | 'user' | 'file' | 'jobs' | 'search' | 'clipboard' | 'shield' | 'calendar' | 'users' | 'refresh' | 'training' | 'bell' | 'settings' | 'payment' | 'skills' | 'menu' | 'close' | 'left' | 'right'
type Entry = [string, string, NavigationIcon]
const roleLinks: Record<Role, Entry[]> = {
  CANDIDATE: [['dashboard', 'Overview', 'home'], ['profile', 'My profile', 'user'], ['cv', 'CV Builder', 'file'], ['jobs', 'Explore Jobs', 'search'], ['applications', 'My applications', 'clipboard'], ['verification', 'Verification', 'shield'], ['bookings', 'Appointments', 'calendar']],
  EMPLOYER: [['dashboard', 'Overview', 'home'], ['profile', 'Company profile', 'user'], ['jobs', 'Jobs', 'jobs'], ['candidates', 'Applicants', 'users']],
  EVALUATOR: [['verifications', 'Verifications', 'shield'], ['bookings', 'Appointments', 'calendar']],
  ADMIN: [['dashboard', 'Overview', 'home'], ['queue', 'Waiting room', 'users']],
}
const adminTabs: [string, NavigationIcon][] = [['Users', 'users'], ['Candidates', 'user'], ['Employers', 'jobs'], ['Jobs', 'jobs'], ['Applications', 'clipboard'], ['Bookings', 'calendar'], ['Demo payments', 'payment'], ['Application assessments', 'file'], ['Skill attempts', 'clipboard'], ['Skills', 'skills'], ['Company types', 'jobs'], ['Verifications', 'shield'], ['Verification requirements', 'shield'], ['Marketplace settings', 'settings']]
export function navigationFor(role: Role): NavigationItem[] {
  const links = [...roleLinks[role]]
  if (role === 'ADMIN') links.push(...adminTabs.map(([label, icon]): Entry => [`dashboard?tab=${encodeURIComponent(label)}`, label, icon]))
  if (role !== 'EVALUATOR') links.push(['placements', 'Placements', 'users'])
  if (role === 'EMPLOYER' || role === 'ADMIN') links.push(['replacements', 'Replacements', 'refresh'])
  if (role === 'EMPLOYER') links.push(['verification', 'Verification', 'shield'])
  if (role !== 'EMPLOYER') links.push(['training', 'Training', 'training'])
  links.push(['notifications', 'Notifications', 'bell'])
  return links.map(([path, label, icon]) => ({ path: `/${role.toLowerCase()}/${path}`, label, icon }))
}
export function navigationActive(item: NavigationItem, pathname: string, search: string) {
  const [path, query] = item.path.split('?')
  if (path === '/admin/dashboard') {
    const tab = new URLSearchParams(search).get('tab') || 'Overview'
    return pathname === path && tab === (query ? new URLSearchParams(query).get('tab') : 'Overview')
  }
  if (/^\/employer\/jobs\/[^/]+\/applications(?:\/|$)/.test(pathname)) return path === '/employer/candidates'
  return pathname === path || pathname.startsWith(`${path}/`)
}
