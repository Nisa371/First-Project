import { useLoad, words } from './useLoad'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { marketplace, type Job } from './api'
import { Workspace, LoadState, Field, Feedback, Empty } from './shared'
const load = () => Promise.all([marketplace.jobs(), marketplace.skills()])
const blank = { title: '', description: '', location: '', candidateType: 'TECH' as 'TECH' | 'TRADE', requiredSkillId: '' }
export function JobsPage() {
  const state = useLoad(load)
  const [draft, setDraft] = useState(blank), [editing, setEditing] = useState<number | null>(null), [showForm, setShowForm] = useState(false)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState(''), [closing, setClosing] = useState<number | null>(null)
  const [fields, setFields] = useState<Record<string, string>>({})
  async function save(e: FormEvent) {
    e.preventDefault(); setBusy(true); setError(''); setSuccess(''); setFields({})
    const body = { ...draft, requiredSkillId: draft.requiredSkillId ? Number(draft.requiredSkillId) : null }
    try {
      const result = editing ? await api.put<Job>(`/jobs/${editing}`, body) : await api.post<Job>('/jobs', body)
      if (state.data) state.setData([[result.data, ...state.data[0].filter(j => j.id !== result.data.id)], state.data[1]])
      setShowForm(false); setDraft(blank); setEditing(null); setSuccess(editing ? 'Job updated.' : 'Your job is active. Start finding talent.')
    } catch (e) { const failure = apiFailure(e); setError(failure.message); setFields(failure.fieldErrors ?? {}) } finally { setBusy(false) }
  }
  async function close(id: number) {
    setBusy(true); setError(''); setSuccess('')
    try { const result = await api.post<Job>(`/jobs/${id}/close`); if (state.data) state.setData([state.data[0].map(j => j.id === id ? result.data : j), state.data[1]]); setClosing(null); setSuccess('Job closed. Your shortlist is still available.') }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  return <Workspace title="Build your next great team" subtitle="Manage your openings, then find available candidates with the skills you need."><LoadState {...state} />{!state.loading && !state.error && state.data && <><Feedback error={error} success={success} />
    {!showForm && <button className="button-primary mb-6" onClick={() => { setEditing(null); setDraft(blank); setShowForm(true); setFields({}); setError('') }}>+ Create a job</button>}
    {showForm && <form className="surface mb-8" onSubmit={save}><h2 className="mb-6 text-xl font-bold">{editing ? 'Edit job' : 'Create an opening'}</h2><fieldset disabled={busy} className="space-y-5"><Field name="title" label="Job title" value={draft.title} required maxLength={200} error={fields.title} onChange={v => setDraft({ ...draft, title: v })} /><Field name="description" label="Responsibilities and requirements" value={draft.description} required maxLength={5000} multiline error={fields.description} onChange={v => setDraft({ ...draft, description: v })} /><div className="grid gap-5 sm:grid-cols-2"><Field name="location" label="Work location" value={draft.location} required error={fields.location} onChange={v => setDraft({ ...draft, location: v })} /><div><label className="field-label" htmlFor="track">Candidate track</label><select id="track" className="form-input" value={draft.candidateType} onChange={e => setDraft({ ...draft, candidateType: e.target.value as 'TECH' | 'TRADE' })}><option>TECH</option><option>TRADE</option></select></div></div><div><label className="field-label" htmlFor="requiredSkill">Required skill</label><select id="requiredSkill" className="form-input" value={draft.requiredSkillId} onChange={e => setDraft({ ...draft, requiredSkillId: e.target.value })}><option value="">Any skill</option>{state.data[1].map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div><div className="flex flex-wrap gap-3"><button disabled={busy} className="button-primary">{busy ? 'Saving…' : editing ? 'Save changes' : 'Publish job'}</button><button type="button" className="button-secondary" onClick={() => { setShowForm(false); setError('') }}>Cancel</button></div></fieldset></form>}
    {!state.data[0].length && <Empty title="Your first hire starts here">Create an opening with a clear role, location and required skill.</Empty>}
    <div className="grid gap-5 md:grid-cols-2">{state.data[0].map(j => <article className="surface flex flex-col" key={j.id}><div className="flex flex-wrap items-center justify-between gap-3"><span className="eyebrow">{j.candidateType}</span><span className={`badge ${j.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-800' : ''}`}>{words(j.status)}</span></div><h2 className="mt-4 break-words text-xl font-bold">{j.title}</h2><p className="mt-2 text-sm text-slate-500">{j.location} · {j.requiredSkillName ?? 'Any skill'}</p><p className="mt-4 line-clamp-3 whitespace-pre-line break-words text-sm leading-relaxed text-slate-600">{j.description}</p><p className="my-5 text-sm font-semibold">{j.shortlistCount} shortlisted</p><div className="mt-auto flex flex-wrap gap-2"><Link to={`/employer/candidates?job=${j.id}`} className="button-primary">View talent & shortlist</Link>{j.status === 'ACTIVE' && <><button disabled={busy} className="button-secondary" onClick={() => { setEditing(j.id); setDraft({ title: j.title, description: j.description, location: j.location, candidateType: j.candidateType, requiredSkillId: j.requiredSkillId?.toString() ?? '' }); setShowForm(true); setFields({}); setError(''); window.scrollTo({ top: 0, behavior: 'smooth' }) }}>Edit</button><button disabled={busy} className="button-secondary" onClick={() => setClosing(j.id)}>Close job</button></>}</div>{closing === j.id && <div className="mt-4 rounded-xl bg-amber-50 p-4"><p className="text-sm">Close this opening? It will stop accepting new shortlist entries and cannot be reopened.</p><div className="mt-3 flex gap-3"><button className="button-primary" disabled={busy} onClick={() => void close(j.id)}>Confirm close</button><button className="button-secondary" disabled={busy} onClick={() => setClosing(null)}>Keep open</button></div></div>}</article>)}</div></>}
  </Workspace>
}
