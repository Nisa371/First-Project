import { useState } from 'react'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { Feedback, LoadState } from '../marketplace/shared'

const load = () => api.get<{ days: number }>('/admin/replacement-guarantee').then(r => r.data)
export function ReplacementGuaranteeSettings() {
  const state = useLoad(load)
  const [days, setDays] = useState<string | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  async function save() {
    setBusy(true); setError(''); setSuccess('')
    try { const response = await api.put<{ days: number }>('/admin/replacement-guarantee', { days: Number(days ?? state.data?.days) }); state.setData(response.data); setDays(null); setSuccess('Replacement guarantee duration saved. Existing placement deadlines are unchanged.') }
    catch (e) { setError(apiFailure(e).message) }
    finally { setBusy(false) }
  }
  return <section className="surface space-y-4"><h2 className="text-xl font-bold">Replacement Guarantee</h2><p className="text-sm text-slate-600">Employers may request a replacement within this period after a guaranteed placement begins.</p><LoadState {...state} /><Feedback error={error} success={success} />{state.data && <form className="space-y-4" onSubmit={e => { e.preventDefault(); void save() }}><label className="block max-w-sm"><span className="field-label">Replacement request window (days)</span><input className="form-input" type="number" min={1} max={3650} step={1} required disabled={busy} value={days ?? state.data.days} onChange={e => { setDays(e.target.value); setSuccess('') }} /></label><p className="text-sm text-slate-500">Applies to new guaranteed placements only. Existing deadlines stay fixed. The separate replacement fulfillment target remains 24 hours.</p><button className="button-primary" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button></form>}</section>
}
