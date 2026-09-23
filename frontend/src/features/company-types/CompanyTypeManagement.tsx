import { Modal } from '../../components/Modal'
import { useCallback, useState, type FormEvent } from 'react'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { Empty, Feedback, LoadState } from '../marketplace/shared'
import type { CompanyType } from './CompanyTypeSelector'

export function CompanyTypeManagement() {
  const [page, setPage] = useState(0)
  const [pending, setPending] = useState<{ type: CompanyType; remove: boolean } | null>(null)
  const [editing, setEditing] = useState<CompanyType | null>(null)
  const [name, setName] = useState(''), [query, setQuery] = useState('')
  const load = useCallback(() => api.get<{ content: { id: number; fields: Omit<CompanyType, 'id'> }[]; totalPages: number }>('/admin/records/company-types', { params: { page, size: 12, search: query } }).then(r => ({ content: r.data.content.map(row => ({ id: row.id, ...row.fields })), totalPages: r.data.totalPages })), [page, query])
  const state = useLoad(load)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  async function mutate(action: () => Promise<unknown>, message: string) {
    setBusy(true); setError(''); setSuccess('')
    try { await action(); setSuccess(message); setPending(null); setEditing(null); setName(''); state.reload() }
    catch (cause) { setError(apiFailure(cause).message) } finally { setBusy(false) }
  }
  function save(e: FormEvent) {
    e.preventDefault()
    void mutate(() => editing ? api.put(`/admin/company-types/${editing.id}`, { name, active: editing.active }) : api.post('/admin/company-types', { name, active: true }), 'Company type saved.')
  }
  const visible = state.data?.content ?? []
  return <section className="surface space-y-5">
    <div><h2 className="text-xl font-bold">Company types</h2><p className="mt-2 text-sm text-slate-600">Manage the choices employers see. Removing a type that is in use deactivates it; existing profiles keep their selection. Other is always available.</p></div>
    <Feedback error={error} success={success} />
    <form onSubmit={save} className="flex flex-wrap items-end gap-3"><label className="min-w-0 w-full sm:w-auto sm:flex-1"><span className="field-label">{editing ? 'Rename company type' : 'New company type'}</span><input required maxLength={120} className="form-input" value={name} disabled={busy} onChange={e => setName(e.target.value)} /></label><button className="button-primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save name' : 'Add company type'}</button>{editing && <button type="button" className="button-secondary" disabled={busy} onClick={() => { setEditing(null); setName('') }}>Cancel</button>}</form>
    <label className="block"><span className="field-label">Filter company types</span><input type="search" className="form-input" value={query} maxLength={200} onChange={e => { setQuery(e.target.value); setPage(0) }} /></label>
    <LoadState {...state} />
    <div className="divide-y divide-slate-100">{visible.map(t => <article key={t.id} className="flex flex-wrap items-center justify-between gap-3 py-4"><div><h3 className="font-semibold">{t.name}</h3><span className="badge mt-2">{t.active ? 'Active' : 'Inactive'}</span></div>{t.other ? <span className="text-sm text-slate-500">Required option</span> : <div className="flex flex-wrap gap-2"><button disabled={busy} className="button-secondary" onClick={() => { setEditing(t); setName(t.name) }}>Rename<span className="sr-only"> {t.name}</span></button><button disabled={busy} className="button-secondary" onClick={() => t.active ? setPending({ type: t, remove: false }) : void mutate(() => api.put(`/admin/company-types/${t.id}`, { name: t.name, active: true }), 'Availability updated.')}>{t.active ? 'Deactivate' : 'Activate'}<span className="sr-only"> {t.name}</span></button><button disabled={busy} className="button-secondary text-rose-700" onClick={() => setPending({ type: t, remove: true })}>Remove<span className="sr-only"> {t.name}</span></button></div>}</article>)}</div>
    {!state.loading && !state.error && !visible.length && <Empty title="No matching company types">Try a different search or add a new type.</Empty>}
    {state.data && <div className="flex items-center justify-between gap-3"><button className="button-secondary" disabled={!page} onClick={() => setPage(p => p - 1)}>Previous</button><span>Page {page + 1} of {Math.max(1, state.data.totalPages)}</span><button className="button-secondary" disabled={page + 1 >= state.data.totalPages} onClick={() => setPage(p => p + 1)}>Next</button></div>}
    {pending && <Modal title={pending.remove ? 'Remove company type' : 'Deactivate company type'} close={() => { if (!busy) setPending(null) }}><Feedback error={error} /><p>{pending.type.name} will no longer be selectable. Referenced records will be deactivated to preserve history.</p><div className="mt-5 flex gap-3"><button disabled={busy} className="button-primary" onClick={() => void mutate(() => pending.remove ? api.delete(`/admin/company-types/${pending.type.id}`) : api.put(`/admin/company-types/${pending.type.id}`, { name: pending.type.name, active: false }), 'Company type updated.')}>{busy ? 'Saving…' : 'Confirm'}</button><button disabled={busy} className="button-secondary" onClick={() => setPending(null)}>Cancel</button></div></Modal>}
  </section>
}
