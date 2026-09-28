import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { Feedback, LoadState } from '../marketplace/shared'

const loadPolicy = () => api.get<{ days: number }>('/placements/replacement-guarantee').then(r => r.data)
export function HirePlacementModal({ jobId, candidateId, name, close, hired }: { jobId: number; candidateId: number; name: string; close: () => void; hired: () => void }) {
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
      {guaranteed && <p className="text-xs text-slate-500">The Admin policy in effect when you confirm sets the deadline. Matching depends on eligible workers in the existing skill queue. The fulfillment target is a separate 24 hours after requesting replacement.</p>}
      <Feedback error={error} />
      <div className="flex flex-wrap gap-3"><button type="button" className="button-secondary" disabled={busy} onClick={close}>Cancel</button><button className="button-primary" disabled={busy || (guaranteed && (!policy.data || policy.loading))}>{busy ? 'Hiring…' : 'Confirm hire'}</button></div>
    </form>
  </Modal>
}
