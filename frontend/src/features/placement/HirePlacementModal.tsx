import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState } from '../marketplace/shared'
import type { Candidate } from '../marketplace/api'

const loadPolicy = () => api.get<{ days: number }>('/placements/replacement-guarantee').then(r => r.data)
export function HirePlacementModal({ jobId, candidateId, name, tradeReadiness, close, hired }: { jobId: number; candidateId: number; name: string; tradeReadiness?: Pick<Candidate, 'verificationStatus' | 'availability'>; close: () => void; hired: () => void }) {
  const policy = useLoad(loadPolicy)
  const [guaranteed, setGuaranteed] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function hire() {
    setBusy(true); setError('')
    try { await api.post('/placements', { jobId, candidateId, guaranteed }); hired() }
    catch (e) { setError(apiFailure(e).message) }
    finally { setBusy(false) }
  }
  return <Modal title={`Hire ${name}`} close={() => { if (!busy) close() }}>
    <form onSubmit={e => { e.preventDefault(); void hire() }} className="space-y-5">
      <p className="text-sm text-slate-600">Choose coverage for this placement. Confirming hire starts an active placement.</p>
      <fieldset disabled={busy} className="space-y-3">
        <legend className="field-label">Placement type</legend>
        <label className="flex gap-3 rounded-xl border p-4"><input type="radio" name="placement-type" checked={!guaranteed} onChange={() => setGuaranteed(false)} /><span><strong>Standard placement</strong><span className="mt-1 block text-sm text-slate-600">No replacement guarantee.</span></span></label>
        <label className="flex gap-3 rounded-xl border p-4"><input type="radio" name="placement-type" checked={guaranteed} onChange={() => setGuaranteed(true)} /><span><strong>Replacement-guaranteed placement</strong><span className="mt-1 block text-sm text-slate-600">{policy.data ? `Replacement requests allowed within ${policy.data.days} days of the placement start date.` : 'Loading the current replacement policy…'}</span></span></label>
      </fieldset>
      <LoadState {...policy} />
      {guaranteed && <p className="text-xs text-slate-500">The Admin policy in effect when you confirm sets the deadline. Tech & Corporate replacements receive a free job repost. Trade replacements use the worker queue with a separate 24-hour operational target.</p>}
      {tradeReadiness && <section aria-label="Trade readiness summary" className="rounded-xl border border-slate-200 p-4 text-sm">
        <h3 className="font-semibold">Trade readiness</h3>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-slate-600">
          <li>Verification: {tradeReadiness.verificationStatus === 'VERIFIED' ? 'Complete' : 'Incomplete'}</li>
          <li>Availability: {tradeReadiness.availability === 'AVAILABLE' ? 'Available' : 'Unavailable'}</li>
        </ul>
        <p className="mt-2 text-xs text-slate-500">Based on the loaded applicant details. Verification, required skill, availability and placement eligibility are checked when you confirm.</p>
      </section>}
      {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm leading-relaxed text-rose-800">
        <p className="whitespace-pre-line break-words">{error}</p>
      </div>}
      <div className="flex flex-wrap gap-3"><button type="button" className="button-secondary" disabled={busy} onClick={close}>Cancel</button><button className="button-primary" disabled={busy || (guaranteed && (!policy.data || policy.loading))}>{busy ? 'Hiring…' : 'Confirm hire'}</button></div>
    </form>
  </Modal>
}
