import { useLoad } from './useLoad'
import { useState, type FormEvent } from 'react'
import { api, apiFailure } from '../../services/api'
import { marketplace, type Company } from './api'
import { Workspace, LoadState, Field, Feedback } from './shared'
export function EmployerProfilePage() {
  const state = useLoad(marketplace.company)
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('')
  const [fields, setFields] = useState<Record<string, string>>({})
  async function save(e: FormEvent) {
    e.preventDefault(); setBusy(true); setError(''); setSuccess(''); setFields({})
    try { state.setData((await api.put<Company>('/employers/me', state.data)).data); setSuccess('Company profile saved.') }
    catch (e) { const failure = apiFailure(e); setError(failure.message); setFields(failure.fieldErrors ?? {}) } finally { setBusy(false) }
  }
  return <Workspace title="Introduce your company" subtitle="A clear company profile helps your team build a consistent hiring presence."><LoadState {...state} />{!state.loading && !state.error && state.data && <form onSubmit={save} className="surface max-w-3xl"><Feedback error={error} success={success} /><fieldset disabled={busy} className="space-y-5">{([['companyName', 'Company name', 200], ['industry', 'Industry', 120], ['contactPhone', 'Contact phone', 32], ['address', 'Address', 500], ['description', 'About your company', 2000]] as const).map(([key, label, max]) => <Field key={key} name={key} label={label} maxLength={max} value={state.data![key]} required={key === 'companyName'} multiline={key === 'description'} type={key === 'contactPhone' ? 'tel' : 'text'} error={fields[key]} onChange={v => state.setData({ ...state.data!, [key]: v })} />)}<button className="button-primary" disabled={busy}>{busy ? 'Saving…' : 'Save company profile'}</button></fieldset></form>}</Workspace>
}
