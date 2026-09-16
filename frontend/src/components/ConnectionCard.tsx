import { useEffect, useState } from 'react'
import { getHealth } from '../services/health'

type ConnectionState = 'checking' | 'connected' | 'unavailable'

export function ConnectionCard() {
  const [state, setState] = useState<ConnectionState>('checking')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getHealth(controller.signal).then(
      () => { if (!controller.signal.aborted) setState('connected') },
      () => { if (!controller.signal.aborted) setState('unavailable') },
    )
    return () => controller.abort()
  }, [attempt])

  const content = {
    checking: { label: 'Checking connection', description: 'Contacting the marketplace service…', color: 'bg-sky-50 text-sky-800', dot: 'bg-sky-600 motion-safe:animate-pulse' },
    connected: { label: 'Connected', description: 'The frontend and backend are communicating successfully.', color: 'bg-emerald-50 text-emerald-800', dot: 'bg-emerald-600' },
    unavailable: { label: 'Unavailable', description: 'We couldn’t reach the service. Check that the backend is running, then try again.', color: 'bg-rose-50 text-rose-800', dot: 'bg-rose-600' },
  }[state]

  return (
    <section id="connection" aria-labelledby="connection-title" className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div><p className="eyebrow">LIVE SERVICE CHECK</p><h2 id="connection-title" className="mt-2 text-2xl font-semibold tracking-tight">A connected foundation.</h2></div>
        <span className={`inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-semibold ${content.color}`}><span aria-hidden="true" className={`size-2 rounded-full ${content.dot}`} />{content.label}</span>
      </div>
      <p role="status" aria-live="polite" className="mt-5 min-h-12 max-w-xl text-slate-600">{content.description}</p>
      <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 pt-5">
        <span className="text-sm text-slate-500">Development environment · API availability</span>
        <button type="button" onClick={() => { setState('checking'); setAttempt(value => value + 1) }} disabled={state === 'checking'} className="button-primary">{state === 'checking' ? 'Checking…' : 'Check again'}<span aria-hidden="true">↗</span></button>
      </div>
    </section>
  )
}
