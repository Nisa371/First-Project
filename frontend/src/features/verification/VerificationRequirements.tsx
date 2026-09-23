import { Modal } from '../../components/Modal'
import { useCallback, useState, type FormEvent } from 'react'
import { api, apiFailure } from '../../services/api'
import { useLoad, words } from '../marketplace/useLoad'
import { Field, Feedback, LoadState, Empty } from '../marketplace/shared'
import { type Requirement, type Target } from './api'
import type { CompanyType } from '../company-types/CompanyTypeSelector'
const loadTypes = () => api.get<CompanyType[]>('/company-types').then(r => r.data)
const targets: Target[] = ['CANDIDATE', 'EMPLOYER', 'COMPANY_EMPLOYER', 'HOUSEHOLD_EMPLOYER']
const empty = { name: '', description: '', targetType: 'CANDIDATE' as Target, companyTypeId: null as number | null, required: true, active: true }
export function VerificationRequirements() {
  const [page, setPage] = useState(0), [query, setQuery] = useState(''), [confirm, setConfirm] = useState(false)
  const loader = useCallback(() => api.get<{ content: { id: number; fields: Omit<Requirement, 'id'> }[]; totalPages: number }>('/admin/records/verification-requirements', { params: { page, size: 12, search: query } }).then(r => ({ requirements: r.data.content.map(row => ({ id: row.id, ...row.fields })), totalPages: r.data.totalPages })), [page, query])
  const state = useLoad(loader), typeState = useLoad(loadTypes)
  const [editing, setEditing] = useState<Requirement | null>(null), [form, setForm] = useState(empty)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  function reset() { setEditing(null); setForm(empty) }
  function submit(e: FormEvent) {
    e.preventDefault(); if (editing?.active && !form.active) setConfirm(true); else void save()
  }
  async function save() {
    setBusy(true); setError(''); setSuccess('')
    try { if (editing) await api.put(`/admin/verification-requirements/${editing.id}`, form); else await api.post('/admin/verification-requirements', form); reset(); setConfirm(false); state.reload(); setSuccess('Verification requirement saved.') }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  function edit(r: Requirement) { setEditing(r); setForm({ name: r.name, description: r.description ?? '', targetType: r.targetType, companyTypeId: r.companyTypeId, required: r.required, active: r.active }); setError(''); setSuccess('') }
  return <section className="surface space-y-5"><h2 className="text-xl font-bold">Verification requirements</h2><p className="text-sm text-slate-600">Baseline documents remain required. Additional requirements apply automatically to matching accounts; inactive requirements do not block verification.</p><Feedback error={error} success={success} /><LoadState {...state} />
    <form onSubmit={submit}><fieldset disabled={busy} className="grid gap-4 md:grid-cols-2"><Field name="requirement-name" label={editing ? 'Edit requirement name' : 'New requirement name'} value={form.name} required maxLength={160} onChange={name => setForm({ ...form, name })} /><Field name="requirement-description" label="Instructions for users" value={form.description} maxLength={2000} onChange={description => setForm({ ...form, description })} />
      <div><label className="field-label" htmlFor="requirement-target">Applies to</label><select id="requirement-target" className="form-input" disabled={editing?.baseline} value={form.targetType} onChange={e => setForm({ ...form, targetType: e.target.value as Target, companyTypeId: null })}>{targets.map(t => <option key={t} value={t}>{words(t)}</option>)}</select></div>
      {form.targetType !== 'CANDIDATE' && <div><label className="field-label" htmlFor="requirement-type">Company type (optional)</label><select id="requirement-type" className="form-input" disabled={editing?.baseline} value={form.companyTypeId ?? ''} onChange={e => setForm({ ...form, companyTypeId: e.target.value ? Number(e.target.value) : null })}><option value="">All matching company types</option>{editing?.companyTypeId && !typeState.data?.some(t => t.id === editing.companyTypeId) && <option value={editing.companyTypeId}>{editing.companyTypeName} (current)</option>}{typeState.data?.map(t => <option key={t.id} value={t.id}>{t.name}{t.active ? '' : ' (inactive)'}</option>)}</select></div>}
      <div className="flex flex-wrap gap-5"><label className="flex min-h-11 items-center gap-2"><input type="checkbox" disabled={editing?.baseline} checked={form.required} onChange={e => setForm({ ...form, required: e.target.checked })} />Required</label><label className="flex min-h-11 items-center gap-2"><input type="checkbox" disabled={editing?.baseline} checked={form.active} onChange={e => setForm({ ...form, active: e.target.checked })} />Active</label></div><div className="flex flex-wrap gap-3"><button className="button-primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save requirement' : 'Add requirement'}</button>{editing && <button className="button-secondary" type="button" onClick={reset}>Cancel</button>}</div>
    </fieldset></form>
    <label className="block"><span className="field-label">Search requirements by name or target</span><input type="search" className="form-input" maxLength={200} value={query} onChange={e => { setQuery(e.target.value); setPage(0) }} /></label>
    <div className="divide-y divide-slate-100">{state.data?.requirements.map(r => <article key={r.id} className="flex flex-wrap items-center justify-between gap-4 py-4"><div className="min-w-0"><h3 className="font-semibold break-words">{r.name}</h3><p className="mt-1 text-sm text-slate-600">{words(r.targetType)}{r.companyTypeName && ` · ${r.companyTypeName}`} · {r.required ? 'Required' : 'Optional'} · {r.active ? 'Active' : 'Inactive'}{r.baseline && ' · Baseline'}</p></div><button className="button-secondary" disabled={busy} onClick={() => edit(r)}>Edit<span className="sr-only"> {r.name} for {words(r.targetType)}</span></button></article>)}</div>
    {!state.loading && !state.error && !state.data?.requirements.length && <Empty title="No matching requirements">Try a different search or add a requirement.</Empty>}
    {typeState.error && <Feedback error={typeState.error} />}
    {state.data && <div className="flex items-center justify-between gap-3"><button className="button-secondary" disabled={!page} onClick={() => setPage(p => p - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, state.data.totalPages)}</span><button className="button-secondary" disabled={page + 1 >= state.data.totalPages} onClick={() => setPage(p => p + 1)}>Next</button></div>}
    {confirm && <Modal title="Deactivate verification requirement" close={() => { if (!busy) setConfirm(false) }}><Feedback error={error} /><p>This requirement will stop blocking verification for matching accounts. Existing submissions remain in history.</p><div className="mt-5 flex gap-3"><button className="button-primary" disabled={busy} onClick={() => void save()}>{busy ? 'Saving…' : 'Confirm'}</button><button className="button-secondary" disabled={busy} onClick={() => setConfirm(false)}>Go back</button></div></Modal>}
  </section>
}
