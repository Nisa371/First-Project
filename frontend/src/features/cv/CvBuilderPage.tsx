import { useTradeText } from '../trade/useTradeText'
import { useEffect, useState } from 'react'
import { Link, useBlocker } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { Workspace, LoadState, Field, Feedback } from '../marketplace/shared'
import { useLoad } from '../marketplace/useLoad'
import { loadCv, sections, type BuiltCv, type CvContent, type Section, type Entry } from './model'
import { Avatar } from './ProfilePhoto'

export function CvBuilderPage() {
  const tr = useTradeText()

  const state = useLoad(loadCv)
  return <Workspace title={tr("Build your next opportunity")} subtitle={tr("Create a clear, professional CV. Save your changes, then preview or print your saved CV.")}><LoadState {...state} />{state.data && <CvEditor initial={state.data} />}</Workspace>
}
function CvEditor({ initial }: { initial: BuiltCv }) {
  const tr = useTradeText()

  const [saved, setSaved] = useState(initial), [draft, setDraft] = useState<CvContent>(initial.content)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState(''), [errors, setErrors] = useState<Record<string, string>>({})
  const dirty = JSON.stringify(draft) !== JSON.stringify(saved.content)
  const blocker = useBlocker(dirty)
  useEffect(() => { if (!dirty) return; const prevent = (e: BeforeUnloadEvent) => { e.preventDefault(); e.returnValue = '' }; window.addEventListener('beforeunload', prevent); return () => window.removeEventListener('beforeunload', prevent) }, [dirty])
  function entries(key: Section, list: Entry[]) { setDraft(d => ({ ...d, [key]: list })); setSuccess('') }
  function field(key: Section, index: number, name: string, value: string | boolean | null) {
    entries(key, draft[key].map((entry, i) => i === index ? { ...entry, [name]: value, ...(name === 'current' && value ? { endDate: null } : {}) } : entry))
  }
  async function save() {
    setBusy(true); setError(''); setSuccess(''); setErrors({})
    try { const { data } = await api.put<BuiltCv>('/candidates/me/built-cv', draft); setSaved(data); setDraft(data.content); setSuccess(tr("CV saved. Preview shows this saved version.")) }
    catch (e) { const failure = apiFailure(e); setError(failure.message); setErrors(failure.fieldErrors ?? {}) } finally { setBusy(false) }
  }
  return <div className="space-y-6">
    {blocker.state === 'blocked' && <div role="alert" className="surface fixed inset-x-4 bottom-4 z-50 mx-auto max-w-xl border-amber-300 shadow-xl"><p className="font-semibold">{tr("You have unsaved CV changes.")}</p><div className="mt-3 flex gap-3"><button className="button-primary" onClick={() => blocker.reset()}>{tr("Keep editing")}</button><button className="button-secondary" onClick={() => blocker.proceed()}>{tr("Discard changes and leave")}</button></div></div>}
    <section className="surface flex flex-wrap items-center gap-5"><Avatar name={saved.header.fullName} url={saved.header.profilePhotoUrl} /><div className="min-w-0 flex-1"><h2 className="text-xl font-bold">{saved.header.fullName}</h2><p className="break-words text-sm text-slate-600">{[saved.header.email, saved.header.phone, saved.header.location].filter(Boolean).join(' · ')}</p><Link className="mt-3 inline-block font-semibold text-indigo-700" to="/candidate/profile">{tr("Edit profile, photo & skills →")}</Link></div></section>
    {!saved.header.profilePhotoUrl && <p className="rounded-xl bg-amber-50 p-4 text-amber-900">{tr("Profile photo required. Add a photo in My profile.")}</p>}
    {saved.empty && <p className="rounded-xl bg-indigo-50 p-4 text-indigo-900">{tr("Your built CV is empty. Add a summary or your first entry below, then save.")}</p>}
    <Feedback error={error} success={success} />
    <form onSubmit={e => { e.preventDefault(); void save() }}><fieldset disabled={busy} className="space-y-6">
      <section className="surface space-y-5"><h2 className="text-xl font-bold">{tr("Professional summary")}</h2><Field name="summary" label={tr("Introduce your strengths and career goals")} multiline maxLength={4000} value={draft.summary} onChange={v => setDraft(d => ({ ...d, summary: v }))} error={errors.summary} /><div className="grid gap-5 sm:grid-cols-2">{(['linkedinUrl', 'githubUrl'] as const).map(key => <Field key={key} name={key} label={key === 'linkedinUrl' ? tr("LinkedIn URL") : tr("GitHub URL")} type="url" maxLength={2048} value={draft[key]} onChange={v => setDraft(d => ({ ...d, [key]: v }))} error={errors[key]} />)}</div></section>
      <section className="surface"><h2 className="text-xl font-bold">{tr("Skills")}</h2><p className="my-3 text-sm text-slate-600">{tr("Your CV uses the same skills as your profile.")}</p><div className="flex flex-wrap gap-2">{saved.header.skills.map(skill => <span key={skill.id} className="badge">{skill.name} · {tr(skill.proficiencyLevel)}</span>)}{!saved.header.skills.length && <p className="text-sm text-slate-500">{tr("No skills added. Add skills from My profile.")}</p>}</div></section>
      {sections.map(section => <section className="surface space-y-5" key={section.key}><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">{tr(section.label)} <span className="text-sm font-normal text-slate-500">({draft[section.key].length})</span></h2><button type="button" className="button-secondary" disabled={draft[section.key].length >= 30} onClick={() => entries(section.key, [...draft[section.key], Object.fromEntries(section.fields.map(f => [f.key, f.type === 'checkbox' ? false : f.type === 'date' ? null : '']))])}>{tr("+ Add")}{' '}{tr(section.singular)}</button></div>
        {!draft[section.key].length && <p className="text-sm text-slate-500">{tr("No")}{' '}{tr(section.label)} {tr("yet. Include what is relevant to you.")}</p>}
        {draft[section.key].map((entry, index) => <div key={index} className="space-y-4 rounded-xl border border-slate-200 bg-slate-50/50 p-4 sm:p-5"><div className="flex flex-wrap items-center justify-between gap-2"><h3 className="font-bold">{tr(section.label)} · {index + 1}</h3><div className="flex flex-wrap gap-2">{[-1, 1].map(offset => <button key={offset} type="button" className="button-secondary" aria-label={`Move ${tr(section.singular)} ${index + 1} ${offset < 0 ? 'up' : 'down'}`} disabled={index + offset < 0 || index + offset >= draft[section.key].length} onClick={() => { const list = [...draft[section.key]]; [list[index], list[index + offset]] = [list[index + offset], list[index]]; entries(section.key, list) }}>{offset < 0 ? '↑' : '↓'}</button>)}<button type="button" className="button-secondary" onClick={() => { if (window.confirm(`Remove this ${tr(section.singular)}? Save CV to confirm the removal.`)) entries(section.key, draft[section.key].filter((_, i) => i !== index)) }}>{tr("Remove")}</button></div></div><div className="grid gap-4 sm:grid-cols-2">{section.fields.map(f => {
          const name = `${section.key}[${index}].${f.key}`, value = entry[f.key]
          if (f.type === 'checkbox') return <label className="flex min-h-11 items-center gap-3" key={f.key}><input type="checkbox" checked={value === true} onChange={e => field(section.key, index, f.key, e.target.checked)} />{tr(f.label)}</label>
          if (f.key === 'endDate' && entry.current) return null
          return <div key={f.key} className={f.type === 'textarea' ? 'sm:col-span-2' : ''}><Field name={name} label={tr(f.label)} value={typeof value === 'string' ? value : ''} type={f.type === 'textarea' ? 'text' : f.type} multiline={f.type === 'textarea'} required={f.required} maxLength={f.max ?? (f.type === 'url' ? 2048 : 160)} onChange={v => field(section.key, index, f.key, f.type === 'date' && !v ? null : v)} error={errors[name]} /></div>
        })}</div></div>)}
      </section>)}
      <div className="sticky bottom-3 z-10 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-lg"><p role="status" className="text-sm text-slate-600">{dirty ? tr("Unsaved changes") : saved.updatedAt ? tr("All changes saved") : tr("Ready to build your CV")}</p><div className="flex flex-wrap gap-3"><Link className="button-secondary" to="/candidate/cv/preview">{tr("Preview saved CV")}</Link><button className="button-primary" disabled={busy}>{busy ? tr("Saving…") : tr("Save CV")}</button></div></div>
    </fieldset></form>
  </div>
}
