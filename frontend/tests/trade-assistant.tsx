import { StrictMode, act } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter } from 'react-router'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { TradeTools } from '../src/features/trade/TradeTools'
import { api } from '../src/services/api'
Object.assign(globalThis, { IS_REACT_ACT_ENVIRONMENT: true })
const fixture = document.querySelector('#fixture')!, results = document.querySelector('#results')!
const root = createRoot(fixture), logs: string[] = []
const requests: { config: InternalAxiosRequestConfig; resolve: (value: AxiosResponse) => void; reject: (error: Error) => void }[] = []
api.defaults.adapter = config => new Promise((resolve, reject) => requests.push({ config, resolve, reject }))
const posts = () => requests.filter(r => r.config.method === 'post')
const gets = () => requests.filter(r => r.config.method === 'get')
const success = { failureCode: null, messages: [{ role: 'USER', content: 'প্রশ্ন' }, { role: 'ASSISTANT', content: 'সফল উত্তর' }] }
function check(value: unknown, message: string) { if (!value) throw new Error(message); logs.push(`PASS ${message}`) }
function button(label: string) { return fixture.querySelector<HTMLButtonElement>(`button[aria-label="${label}"]`)! }
async function complete(request: typeof requests[number], data: unknown) { await act(async () => request.resolve({ config: request.config, status: 200, statusText: 'OK', headers: {}, data })) }
async function fail(request: typeof requests[number]) { await act(async () => request.reject(new Error('Network failure'))) }
async function draft() {
  await act(async () => {
    const field = fixture.querySelector('textarea')!
    Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')!.set!.call(field, 'প্রশ্ন')
    field.dispatchEvent(new Event('input', { bubbles: true }))
  })
}
async function submitRepeatedly() {
  await act(async () => {
    const submit = fixture.querySelector<HTMLButtonElement>('button[type="submit"]')!
    submit.click(); submit.click()
    fixture.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }))
  })
}
try {
  await act(async () => root.render(<StrictMode><MemoryRouter><TradeTools /></MemoryRouter></StrictMode>))
  check(posts().length === 0, 'StrictMode mount never sends a message')
  await act(async () => button('বাংলা সহকারী খুলুন').click())
  await fail(gets().at(-1)!)
  check(posts().length === 0 && !!fixture.querySelector('[role="alert"]'), 'History failure is independent of send failures')
  await act(async () => fixture.querySelector<HTMLButtonElement>('[role="alert"] button')!.click())
  check(posts().length === 0 && gets().length === 2, 'History retry only fetches history, never Gemini')
  await complete(gets().at(-1)!, { failureCode: null, messages: [] })
  await draft(); await submitRepeatedly()
  check(posts().length === 1, 'One submission, repeated clicks and duplicate submit events cause exactly one API call')
  check(fixture.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled, 'Sending is disabled while pending')
  check(posts()[0].config.timeout === 60000, 'Send does not inherit the premature 8 second deadline')
  await act(async () => button('সহকারী বন্ধ করুন').click())
  const before = gets().length
  await act(async () => button('বাংলা সহকারী খুলুন').click())
  check(gets().length === before, 'Reopening during send does not start a competing conversation refresh')
  await complete(posts()[0], success)
  check(fixture.textContent?.split('সফল উত্তর').length === 2 && !fixture.querySelector('[role="alert"]'), 'Success shows exactly one assistant response and zero errors')
  await fail(posts()[0])
  check(!fixture.querySelector('[role="alert"]'), 'A late rejection cannot change settled success')
  await draft(); await submitRepeatedly(); await fail(posts()[1])
  check(fixture.textContent?.split('সফল উত্তর').length === 2 && !!fixture.querySelector('[role="alert"]'), 'Network failure is transient and never creates an assistant response')
  await act(async () => fixture.querySelector<HTMLButtonElement>('[role="alert"] button')!.click())
  check(posts().length === 3, 'Explicit retry sends once')
  await complete(posts()[2], { failureCode: 'AI_PROVIDER_RATE_LIMIT', messages: [] })
  check(fixture.textContent?.split('সফল উত্তর').length === 2, 'Provider failure preserves successful history without fake messages')
  await act(async () => fixture.querySelector<HTMLButtonElement>('[role="alert"] button')!.click())
  await complete(posts()[3], success)
  check(!fixture.querySelector('[role="alert"]'), 'Successful retry clears transient failure')
  await act(async () => button('সহকারী বন্ধ করুন').click())
  await act(async () => button('বাংলা সহকারী খুলুন').click())
  const stale = gets().at(-1)!
  await act(async () => button('সহকারী বন্ধ করুন').click())
  check(stale.config.signal?.aborted, 'Closing aborts conversation refresh')
  await act(async () => button('বাংলা সহকারী খুলুন').click())
  await complete(gets().at(-1)!, success)
  await fail(stale)
  check(!fixture.querySelector('[role="alert"]'), 'Stale history failure cannot append an error after success')
  await draft(); await submitRepeatedly()
  const last = posts().at(-1)!
  await act(async () => root.unmount())
  check(last.config.signal?.aborted, 'Unmount aborts the active send')
  await complete(last, success)
  check(!fixture.textContent, 'Unmounted request cannot update UI')
  results.textContent = `${logs.join('\n')}\nALL ${logs.length} CHECKS PASSED`
} catch (error) { results.textContent = `${logs.join('\n')}\nFAIL ${String(error)}`; console.error(error) }
