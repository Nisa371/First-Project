import { ThemeProvider } from '../src/features/theme/ThemeProvider'
// Development-only browser checks. Open /tests/phase10.html through the Vite dev server.
// Uses React's own act helper and browser DOM; no additional test framework or provider.
import { act, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Routes, Route } from 'react-router'
import { AuthContext } from '../src/features/auth/useAuth'
import type { CurrentUser } from '../src/features/auth/types'
import { AppLayout } from '../src/layouts/AppLayout'
import { Workspace } from '../src/features/marketplace/shared'
import { CandidateJobsPage } from '../src/features/marketplace/ExploreJobsPage'
import { MyApplicationsPage } from '../src/features/marketplace/ApplicationsPage'
import { DemoPaymentPage } from '../src/features/payment/DemoPaymentPage'
import { api } from '../src/services/api'
import { tradeText } from '../src/features/trade/useTradeText'
import { type Recognition, insertTranscript, supportedField } from '../src/features/trade/voice'
import '../src/app/styles.css'

Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
const results = document.querySelector('#results')!, container = document.querySelector('#fixture')!
const root = createRoot(container)
const logs: string[] = []
function check(condition: unknown, message: string) { if (!condition) throw new Error(message); logs.push(`PASS ${message}`); results.textContent = logs.join('\n') }
function field(id: string) { return document.getElementById(id) as HTMLInputElement }
function mic() { return document.querySelector('[data-trade-mic]') as HTMLButtonElement }
const current: CurrentUser = { id: 1, email: 'test@example.test', role: 'CANDIDATE', candidateType: 'TRADE', displayName: 'পরীক্ষা' }
export function Fields() {
  const [text, setText] = useState('আমি রাজশাহীতে'), [area, setArea] = useState('')
  return <Workspace title="পরীক্ষা" subtitle="বাংলা ভয়েস পরীক্ষা"><label htmlFor="text">লেখা</label><input id="text" value={text} onChange={e => setText(e.target.value)} /><label htmlFor="area">বিস্তারিত</label><textarea id="area" value={area} onChange={e => setArea(e.target.value)} /><input id="password" type="password" /><input id="number" type="number" /><input id="readonly" readOnly /><fieldset disabled><input id="fieldset-disabled" /></fieldset><output id="state">{text}|{area}</output></Workspace>
}
let latest: FakeRecognition | null = null, starts = 0, stops = 0, aborts = 0
class FakeRecognition implements Recognition {
  lang = ''; continuous = false; interimResults = false
  onresult: Recognition['onresult'] = null; onerror: Recognition['onerror'] = null; onend: Recognition['onend'] = null
  // The fake exposes its active instance so tests can emit browser speech events.
  // oxlint-disable-next-line typescript/no-this-alias
  constructor() { latest = this }
  start() { starts++ }
  stop() { stops++; this.onend?.() }
  abort() { aborts++ }
  result(text: string, final = true) { this.onresult?.({ resultIndex: 0, results: [Object.assign([{ transcript: text }], { isFinal: final })] }) }
}
function recognition() { if (!latest) throw new Error('Recognition did not start'); return latest }
const win = window as Window & { SpeechRecognition?: new () => Recognition; webkitSpeechRecognition?: new () => Recognition }
const original = win.SpeechRecognition, originalWebkit = win.webkitSpeechRecognition
win.SpeechRecognition = FakeRecognition; win.webkitSpeechRecognition = undefined
const job = { id: 4, title: 'English employer job', description: 'Keep this employer description unchanged.', companyName: 'Employer', companyTypeName: 'Household', location: 'Dhaka', candidateType: 'TRADE', expectedExperienceMonths: 12, status: 'ACTIVE', hasApplied: false, createdAt: '2026-09-01', requiredSkillName: null }
api.defaults.adapter = async config => {
  let data: unknown
  if (config.url?.includes('/trade/assistant/')) data = { messages: [], failureCode: config.method === 'post' ? 'AI_PROVIDER_NOT_CONFIGURED' : null }
  else if (config.url === '/company-types') data = []
  else if (config.url?.includes('/jobs')) data = { content: [job], totalElements: 1, totalPages: 1, page: 0 }
  else if (config.url?.includes('/applications')) data = [{ id: 3, job, status: 'APPLIED', assessmentStatus: 'NOT_STARTED', appliedAt: '2026-09-01' }]
  else if (config.url?.includes('/payments')) data = { id: 1, purpose: 'SESSION_BOOKING', description: 'Session', amount: 100, currency: 'BDT', reference: 'DEMO', status: 'PENDING' }
  else throw new Error(`Unexpected test route ${config.url}`)
  return { data, status: 200, statusText: 'OK', headers: {}, config }
}
async function render(user: CurrentUser = current, page = 'fields') {
  await act(async () => root.render(<ThemeProvider><AuthContext.Provider value={{ user, loading: false, error: null, expired: false, retry() {}, accept() {}, logout() {} }}><MemoryRouter key={`${user.id}-${user.role}-${user.candidateType}-${page}`} initialEntries={['/candidate/test']}><Routes><Route element={<AppLayout />}><Route path="/candidate/test" element={page === 'jobs' ? <CandidateJobsPage /> : page === 'applications' ? <MyApplicationsPage /> : page === 'payment' ? <DemoPaymentPage /> : <Fields />} /></Route></Routes></MemoryRouter></AuthContext.Provider></ThemeProvider>))
}
async function keyboardToggle() { await act(async () => { mic().focus(); mic().click() }) }
try {
  await render()
  check(starts === 0, 'Microphone never starts automatically')
  check(container.textContent?.includes('আমার প্রোফাইল') && !container.textContent?.includes('My profile'), 'Trade navigation uses Bangla')
  await act(async () => { field('text').focus(); field('text').setSelectionRange(field('text').value.length, field('text').value.length) })
  await keyboardToggle()
  check(recognition().lang === 'bn-BD', 'Recognition uses bn-BD')
  await act(async () => recognition().result('চাকরি খুঁজছি', false))
  check(field('text').value === 'আমি রাজশাহীতে', 'Interim speech does not alter input')
  await act(async () => recognition().result('চাকরি খুঁজছি'))
  check(field('text').value === 'আমি রাজশাহীতে চাকরি খুঁজছি' && field('state').textContent?.includes('চাকরি খুঁজছি'), 'Final speech preserves text and updates controlled React state')
  await keyboardToggle()
  check(stops === 1, 'Keyboard toggle stops recognition')
  await act(async () => { field('area').focus() })
  await keyboardToggle(); await act(async () => recognition().result('আমার অভিজ্ঞতা'))
  check(field('area').value === 'আমার অভিজ্ঞতা' && field('state').textContent?.endsWith('আমার অভিজ্ঞতা'), 'Textarea receives final transcript in React state')
  await keyboardToggle()
  await act(async () => { field('text').focus(); field('text').setSelectionRange(4, 13); insertTranscript(field('text'), 'ঢাকায়') })
  check(field('text').value.includes('ঢাকায়') && field('text').value.endsWith('চাকরি খুঁজছি'), 'Cursor selection insertion retains surrounding text')
  check(!supportedField(field('password')) && !supportedField(field('number')) && !supportedField(field('readonly')) && !supportedField(field('fieldset-disabled')), 'Sensitive, numeric and read-only inputs excluded')
  const before = starts
  await act(async () => field('password').focus()); await keyboardToggle()
  check(starts === before, 'Focusing a password clears the prior voice target')
  await act(async () => field('area').focus()); await keyboardToggle()
  await act(async () => recognition().onerror?.({ error: 'not-allowed' }))
  check(container.textContent?.includes('অনুমতি পাওয়া যায়নি') && mic().getAttribute('aria-pressed') === 'false', 'Permission denial is Bangla and stops recognition')
  await act(async () => field('area').focus())
  mic().setPointerCapture = () => {}
  await act(async () => mic().dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, button: 0, pointerId: 1 })))
  check(mic().getAttribute('aria-pressed') === 'true', 'Pointer hold starts recognition')
  await act(async () => mic().dispatchEvent(new PointerEvent('pointerup', { bubbles: true, button: 0, pointerId: 1 })))
  check(mic().getAttribute('aria-pressed') === 'false', 'Pointer release stops recognition')
  await act(async () => (container.querySelector('[aria-label="বাংলা সহকারী খুলুন"]') as HTMLButtonElement).click())
  await keyboardToggle(); await act(async () => recognition().result('কীভাবে আবেদন করব?')); await keyboardToggle()
  check(field('trade-question').value === 'কীভাবে আবেদন করব?', 'Assistant reuses global dictation')
  await act(async () => (field('trade-question').closest('form')!).dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })))
  check(container.textContent?.includes('AI সহকারী এখনো কনফিগার করা হয়নি') && field('trade-question').value === 'কীভাবে আবেদন করব?', 'Unconfigured assistant keeps draft and shows controlled Bangla state')
  const oldAborts = aborts
  await act(async () => field('area').focus()); await keyboardToggle()
  await render({ ...current, candidateType: 'TECH' })
  check(aborts > oldAborts, 'Unmount cancels microphone')
  check(!mic() && container.textContent?.includes('My profile'), 'TECH keeps English navigation and no Trade tools')
  await render({ ...current, role: 'EMPLOYER', candidateType: null })
  check(!mic() && container.textContent?.includes('Company profile'), 'Employer retains English UI and no Trade tools')
  await render({ ...current, role: 'ADMIN', candidateType: null })
  check(!mic() && container.textContent?.includes('Waiting room'), 'Admin retains English UI and no Trade tools')
  win.SpeechRecognition = undefined
  await render()
  check(mic().disabled && container.textContent?.includes('এই ব্রাউজারে ভয়েস নেই'), 'Unsupported browser degrades to typing')
  await act(async () => insertTranscript(field('text'), 'লিখছি'))
  check(field('state').textContent?.includes('লিখছি'), 'Typing-state updates remain available without recognition')
  win.SpeechRecognition = FakeRecognition
  await render(current, 'jobs')
  check(container.textContent?.includes('আবেদন করুন') && container.textContent?.includes(job.description), 'Job UI is Bangla and employer content is untouched')
  check((container.querySelector('option[value="TECH"]') as HTMLOptionElement)?.value === 'TECH', 'Filter API enum remains TECH')
  await render(current, 'applications')
  check(container.textContent?.includes('আবেদন করা হয়েছে'), 'Application status has a Bangla display label')
  await render(current, 'payment')
  check(container.textContent?.includes('এটি শুধুমাত্র ডেমো পেমেন্ট। কোনো আসল টাকা কাটা হবে না।'), 'Payment clearly states no real money in Bangla')
  check(tradeText('Apply', false) === 'Apply' && tradeText('applied', true) === 'আবেদন করা হয়েছে', 'Translation is conditional and enum display mapping is separate')
  results.textContent = `${logs.join('\n')}\n\nALL ${logs.length} CHECKS PASSED`
} catch (error) { results.textContent = `${logs.join('\n')}\nFAIL ${String(error)}`; console.error(error) }
finally { win.SpeechRecognition = original; win.webkitSpeechRecognition = originalWebkit }
