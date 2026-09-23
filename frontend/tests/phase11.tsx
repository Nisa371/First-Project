import { act } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Routes, Route } from 'react-router'
import { ThemeProvider } from '../src/features/theme/ThemeProvider'
import { themeKey, initialTheme } from '../src/features/theme/useTheme'
import { AuthContext } from '../src/features/auth/useAuth'
import type { CurrentUser } from '../src/features/auth/types'
import { AppLayout } from '../src/layouts/AppLayout'
import { AdminPage } from '../src/features/admin/AdminPage'
import { Workspace } from '../src/features/marketplace/shared'
import { Modal } from '../src/components/Modal'
import { api } from '../src/services/api'
import '../src/app/styles.css'
Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
const container = document.querySelector('#fixture')!, results = document.querySelector('#results')!, root = createRoot(container)
const logs: string[] = [], previous = localStorage.getItem(themeKey)
let authMutations = 0
function check(condition: unknown, message: string) { if (!condition) throw new Error(message); logs.push(`PASS ${message}`); results.textContent = logs.join('\n') }
const requests: string[] = []
const fixtures: Record<string, { id: number; title: string; subtitle: string; status: string; fields: Record<string, string | number | boolean | null> }[]> = {}
let lastWrite: { path?: string; body: Record<string, unknown> } = { body: {} }
api.defaults.adapter = async config => {
  requests.push(`${config.method} ${config.url}`)
  if (config.method !== 'get') lastWrite = { path: config.url, body: typeof config.data === 'string' ? JSON.parse(config.data) : {} }
  let data: unknown = {}
  if (config.url === '/admin/stats') data = { users: 23, activeUsers: 21, jobs: 4, activeJobs: 3, pendingVerifications: 2, activePlacements: 1, replacements: 0, candidates: 16, employers: 6, applications: 12, pendingBookings: 1, demoTransactions: 5 }
  else if (config.url === '/admin/records/users') data = { content: [{ id: 7, title: 'demo@example.test', subtitle: 'CANDIDATE', status: 'ACTIVE', fields: { role: 'CANDIDATE', createdAt: '2026-09-01' } }], totalElements: 23, page: 0, totalPages: 2 }
  else if (config.url?.startsWith('/admin/records/')) { const content = fixtures[config.url.split('/').at(-1)!] ?? []; data = { content, totalElements: content.length, page: 0, totalPages: content.length ? 1 : 0 } }
  else if (config.url === '/company-types' || config.url === '/admin/company-types') data = []
  else if (config.url === '/admin/verification-submissions/page') data = { content: [], totalElements: 0, totalPages: 0 }
  else if (config.url?.includes('/trade/assistant/')) data = { messages: [] }
  return { data, status: 200, statusText: 'OK', headers: {}, config }
}
const user: CurrentUser = { id: 11, email: 'admin@example.test', role: 'ADMIN', candidateType: null, displayName: 'Admin' }
async function render(current: CurrentUser | null, modal = false) {
  await act(async () => root.render(<ThemeProvider><AuthContext.Provider value={{ user: current, loading: false, error: null, expired: false, retry() {}, accept() { authMutations++ }, logout() { authMutations++ } }}><MemoryRouter><Routes><Route element={<AppLayout />}><Route path="/" element={current?.role === 'ADMIN' ? <AdminPage /> : <Workspace title="Theme check" subtitle="Readable in both themes"><div className="surface"><label className="field-label" htmlFor="theme-input">Sample input</label><input id="theme-input" className="form-input" /><p className="text-rose-700 bg-rose-50 p-3">Validation message</p><span className="badge">Pending</span>{modal && <Modal title="Shared dialog" close={() => {}}><p>Readable dialog content</p></Modal>}</div></Workspace>} /></Route></Routes></MemoryRouter></AuthContext.Provider></ThemeProvider>))
}
function button(name: string) { const found = [...container.querySelectorAll('button')].find(b => b.textContent === name || (['Details', 'Edit', 'Remove'].includes(name) && b.textContent?.startsWith(name)) || b.getAttribute('aria-label') === name); if (!found) throw new Error(`Missing button ${name}`); return found }
try {
  localStorage.setItem(themeKey, 'light'); await render(user)
  check(document.documentElement.dataset.theme === 'light', 'Stored light preference initializes shared layout')
  check(requests.includes('get /admin/records/users') && !requests.includes('get /admin/users'), 'Admin uses server-paginated records')
  await act(async () => button('Dark theme').click())
  check(document.documentElement.dataset.theme === 'dark' && localStorage.getItem(themeKey) === 'dark', 'Toggle applies and persists dark theme')
  check(authMutations === 0, 'Theme toggle does not mutate authentication')
  check(getComputedStyle(document.body).backgroundColor === 'rgb(11, 18, 32)', 'Dark page background applied')
  await act(async () => root.render(null)); await render(user)
  check(initialTheme() === 'dark' && document.documentElement.dataset.theme === 'dark', 'Stored theme survives root remount/reload initialization')
  await act(async () => button('Details').click())
  check(!!container.querySelector('dialog[open]') && getComputedStyle(container.querySelector('dialog')!).backgroundColor === 'rgb(23, 34, 53)', 'Shared admin dialog uses dark surface')
  await act(async () => button('Close').click())
  for (const name of ['Candidates', 'Employers', 'Jobs', 'Applications', 'Bookings', 'Demo payments', 'Application assessments', 'Skill attempts', 'Skills']) {
    await act(async () => button(name).click()); check(container.textContent?.includes('No matching records'), `${name} has a working empty state`)
  }
  for (const [name, empty] of [['Company types', 'No matching company types'], ['Verification requirements', 'No matching requirements'], ['Verifications', 'No matching submissions']]) {
    await act(async () => button(name).click()); check(container.textContent?.includes(empty), `${name} integrates the existing paginated admin module`)
  }
  fixtures.candidates = [{ id: 12, title: 'Test candidate', subtitle: 'candidate@example.test', status: 'ACTIVE', fields: { fullName: 'Test candidate', userId: 99, candidateType: 'TECH', availability: 'AVAILABLE', totalExperienceMonths: 12, updatedAt: '2026-09-01' } }]
  await act(async () => button('Candidates').click()); await act(async () => button('Edit').click())
  await act(async () => button('Save').click())
  check(lastWrite.path === '/admin/candidates/12' && lastWrite.body.fullName === 'Test candidate' && !('userId' in lastWrite.body) && !('candidateType' in lastWrite.body) && !('updatedAt' in lastWrite.body), 'Profile form sends only whitelisted editable fields')
  fixtures.jobs = [{ id: 4, title: 'Test vacancy', subtitle: 'Demo employer', status: 'ACTIVE', fields: { title: 'Test vacancy' } }]
  await act(async () => button('Jobs').click())
  const beforeClose = requests.filter(r => r === 'post /admin/jobs/4/close').length
  await act(async () => button('Close').click())
  check(!!container.querySelector('dialog[open]') && requests.filter(r => r === 'post /admin/jobs/4/close').length === beforeClose, 'Job close waits for explicit confirmation')
  await act(async () => button('Confirm').click())
  check(requests.includes('post /admin/jobs/4/close'), 'Confirmed job closure calls the dedicated domain action')
  fixtures['company-types'] = [{ id: 6, title: 'Demo type', subtitle: '', status: 'ACTIVE', fields: { name: 'Demo type', active: true, other: false } }]
  await act(async () => button('Company types').click()); await act(async () => button('Remove').click())
  check(!!container.querySelector('dialog[open]') && !requests.includes('delete /admin/company-types/6'), 'Company type removal requires confirmation')
  await act(async () => button('Cancel').click())
  check(!requests.includes('delete /admin/company-types/6'), 'Cancelling destructive action leaves the record unchanged')
  await render({ ...user, role: 'CANDIDATE', candidateType: 'TRADE' })
  check(container.textContent?.includes('আমার প্রোফাইল') && !!container.querySelector('[data-trade-mic]'), 'Bangla navigation and floating microphone remain available')
  await act(async () => button('হালকা থিম').click())
  check(document.documentElement.dataset.theme === 'light', 'Bangla toggle returns to light theme')
  await render({ ...user, role: 'EMPLOYER' }); check(container.textContent?.includes('Company profile'), 'Employer layout renders with theme provider')
  await render({ ...user, role: 'CANDIDATE', candidateType: 'TECH' }); check(container.textContent?.includes('My profile'), 'TECH layout renders with theme provider')
  await render(null); check(container.textContent?.includes('Sign in') && document.documentElement.dataset.theme === 'light', 'Signed-out layout retains browser theme')
  check(authMutations === 0, 'All theme changes leave authentication untouched')
  await render(user); await act(async () => button('Dark theme').click())
  results.textContent = `${logs.join('\n')}\nALL ${logs.length} CHECKS PASSED`
} catch (e) { results.textContent = `${logs.join('\n')}\nFAIL ${String(e)}`; console.error(e) }
finally { if (previous === null) localStorage.removeItem(themeKey); else localStorage.setItem(themeKey, previous) }
