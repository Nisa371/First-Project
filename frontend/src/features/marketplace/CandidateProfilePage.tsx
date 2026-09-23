import { useTradeText } from '../trade/useTradeText'
import { Link } from 'react-router'
import { PhotoUpload } from '../cv/ProfilePhoto'
import { useLoad } from './useLoad'
import { useState, type FormEvent } from 'react'
import { api, apiFailure } from '../../services/api'
import { marketplace, downloadCv, type Profile } from './api'
import { Workspace, LoadState, Field, Feedback } from './shared'
const load = () => Promise.all([marketplace.profile(), marketplace.skills()])
export function CandidateProfilePage() {
  const tr = useTradeText()

  const state = useLoad(load)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  const [skillId, setSkillId] = useState(''), [level, setLevel] = useState('Familiar')
  const [file, setFile] = useState<File | null>(null)
  const p = state.data?.[0], catalog = state.data?.[1] ?? []
  function change(key: keyof Profile, value: string | number) { if (state.data) state.setData([{ ...state.data[0], [key]: value }, state.data[1]]) }
  async function action(work: () => Promise<Profile>, message: string, preserveDraft = true) {
    setBusy(true); setError(''); setSuccess(''); setFields({})
    try { const updated = await work(); if (state.data) state.setData([preserveDraft ? { ...state.data[0], skills: updated.skills, cvOriginalName: updated.cvOriginalName } : updated, state.data[1]]); setSuccess(message) }
    catch (e) { const failure = apiFailure(e); setError(failure.message); setFields(failure.fieldErrors ?? {}) } finally { setBusy(false) }
  }
  function save(e: FormEvent) {
    e.preventDefault(); if (!p) return
    const { fullName, phone, location, bio, educationSummary, experienceSummary, totalExperienceMonths, availability, primaryTradeCategory, portfolioUrl } = p
    void action(() => api.put<Profile>('/candidates/me', { fullName, phone, location, bio, educationSummary, experienceSummary, totalExperienceMonths, availability, primaryTradeCategory, portfolioUrl }).then(r => r.data), tr("Your profile has been saved."), false)
  }
  return <Workspace title={tr("Make your skills stand out")} subtitle={tr("Tell employers what you do best. Your career track and platform review results are managed separately.")}>
    <LoadState {...state} />{!state.loading && !state.error && p && <><Feedback error={error} success={success} />
    <div className="grid items-start gap-6 lg:grid-cols-[1.5fr_1fr]"><form className="surface space-y-5" onSubmit={save}><h2 className="text-xl font-bold">{p.candidateType === 'TRADE' ? tr("আপনার পরিচিতি · Your profile") : 'Profile details'}</h2>
      <fieldset disabled={busy} className="space-y-5">
      <Field name="fullName" label={p.candidateType === 'TRADE' ? tr("পুরো নাম · Full name") : 'Full name'} value={p.fullName} onChange={v => change('fullName', v)} required maxLength={160} error={fields.fullName} />
      <div className="grid gap-5 sm:grid-cols-2"><Field name="phone" label={tr("ফোন · Phone")} value={p.phone} onChange={v => change('phone', v)} type="tel" maxLength={32} error={fields.phone} /><Field name="location" label={tr("এলাকা · Location")} value={p.location} onChange={v => change('location', v)} error={fields.location} /></div>
      <Field name="bio" label={tr("About you")} value={p.bio} onChange={v => change('bio', v)} multiline maxLength={2000} error={fields.bio} />
      <Field name="experienceSummary" label={tr("অভিজ্ঞতা · Experience")} value={p.experienceSummary} onChange={v => change('experienceSummary', v)} multiline maxLength={2000} error={fields.experienceSummary} />
      <label className="block"><span className="field-label">{tr("Total experience (months)")}</span><input name="totalExperienceMonths" type="number" min={0} step={1} required className="form-input" value={p.totalExperienceMonths} onChange={e => change('totalExperienceMonths', Number(e.target.value))} />{fields.totalExperienceMonths && <span className="text-sm text-rose-700">{fields.totalExperienceMonths && tr("Enter a valid number of months.")}</span>}<span className="mt-1 block text-xs text-slate-500">{tr("Enter completed months of experience. Keep your description above.")}</span></label>
      {p.candidateType === 'TECH' ? <><Field name="educationSummary" label={tr("Education")} value={p.educationSummary} onChange={v => change('educationSummary', v)} multiline maxLength={2000} error={fields.educationSummary} /><Field name="portfolioUrl" label="Portfolio URL" value={p.portfolioUrl} onChange={v => change('portfolioUrl', v)} type="url" maxLength={2048} error={fields.portfolioUrl} /></> : <Field name="primaryTradeCategory" label={tr("পেশা · Primary trade")} value={p.primaryTradeCategory} onChange={v => change('primaryTradeCategory', v)} maxLength={120} error={fields.primaryTradeCategory} />}
      <div><label className="field-label" htmlFor="availability">{tr("কাজের জন্য প্রস্তুত? · Availability")}</label><select id="availability" className="form-input" value={p.availability} onChange={e => change('availability', e.target.value)}><option value="AVAILABLE">{tr("Available for work · কাজ করতে প্রস্তুত")}</option><option value="UNAVAILABLE">{tr("Currently unavailable · এখন প্রস্তুত নই")}</option></select></div>
      <button className="button-primary" disabled={busy}>{busy ? tr("Saving…") : tr("Save profile · সংরক্ষণ")}</button></fieldset></form>
      <div className="space-y-6"><PhotoUpload profile={p} onSaved={updated => { if (state.data) state.setData([{ ...state.data[0], profilePhotoUrl: updated.profilePhotoUrl }, state.data[1]]) }} /><section className="surface"><h2 className="text-xl font-bold">{tr("Built CV")}</h2>{!p.hasBuiltCv && <p className="mt-3 text-sm text-amber-800">{tr("Your built CV is empty. Add a summary or your first entry in the builder.")}</p>}<p className="my-3 text-sm text-slate-600">{tr("Create and maintain your structured CV, then preview or print it. Your uploaded PDF stays separate.")}</p><Link className="button-primary" to="/candidate/cv">{tr("Open CV Builder")}</Link></section><section className="surface"><h2 className="text-xl font-bold">{tr("Skills · দক্ষতা")}</h2><p className="mt-2 text-sm text-slate-500">{tr("Add the skills employers can search for. Proficiency is self-reported.")}</p>
        <ul className="my-5 space-y-3">{p.skills.map(s => <li key={s.id} className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 p-3"><div><p className="font-semibold">{s.name}</p><p className="text-sm text-slate-500">{tr(s.proficiencyLevel)}</p></div><button disabled={busy} className="button-secondary" aria-label={`${tr('Remove')} ${s.name}`} onClick={() => void action(() => api.delete<Profile>(`/candidates/me/skills/${s.id}`).then(r => r.data), tr("Skill removed."))}>{tr("Remove")}</button></li>)}</ul>
        {!p.skills.length && <p className="my-5 text-sm text-slate-500">{tr("No skills yet. Add your first skill below.")}</p>}
        <form className="space-y-3" onSubmit={e => { e.preventDefault(); void action(() => api.post<Profile>('/candidates/me/skills', { skillId: Number(skillId), proficiencyLevel: level }).then(r => r.data), tr("Skill added.")); setSkillId('') }}>
          <label className="field-label" htmlFor="skill">{tr("Choose skill")}</label><select id="skill" className="form-input" required value={skillId} disabled={busy} onChange={e => setSkillId(e.target.value)}><option value="">{tr("Select a skill")}</option>{catalog.filter(s => !p.skills.some(own => own.id === s.id)).map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select>
          <label className="field-label" htmlFor="level">{tr("Experience level")}</label><select id="level" className="form-input" value={level} disabled={busy} onChange={e => setLevel(e.target.value)}>{['Familiar', 'Practising', 'Experienced'].map(v => <option key={v} value={v}>{tr(v)}</option>)}</select><button disabled={busy || !skillId} className="button-primary w-full">{tr("Add skill")}</button></form></section>
      {p.candidateType === 'TECH' && <section className="surface"><h2 className="text-xl font-bold">Your CV</h2><p className="mt-2 text-sm text-slate-500">PDF only, up to 5 MB. Employers reviewing your job applications can download your CV. Avoid including identity documents.</p>
        {p.cvOriginalName && <div className="my-4 rounded-xl bg-indigo-50 p-4"><p className="break-all font-medium">{p.cvOriginalName}</p><button className="mt-2 min-h-11 text-sm font-semibold text-indigo-700" disabled={busy} onClick={() => { setError(''); void downloadCv('/candidates/me/cv').catch(e => setError(apiFailure(e).message)) }}>Download current CV</button></div>}
        <form className="mt-5 space-y-4" onSubmit={e => { e.preventDefault(); if (!file) return; if (file.size > 5 * 1024 * 1024 || !file.name.toLowerCase().endsWith('.pdf')) { setError('Choose a PDF up to 5 MB.'); return } const form = new FormData(); form.append('file', file); void action(() => api.post<Profile>('/candidates/me/cv', form).then(r => r.data), 'CV uploaded securely.') }}><label className="field-label" htmlFor="cv">{p.cvOriginalName ? 'Replace CV' : 'Upload CV'}</label><input id="cv" className="w-full min-w-0 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-slate-100 file:p-3" type="file" accept=".pdf,application/pdf" disabled={busy} onChange={e => setFile(e.target.files?.[0] ?? null)} /><button className="button-primary w-full" disabled={busy || !file}>Upload PDF</button></form></section>}</div></div></>}
  </Workspace>
}
