import { useCallback, useState, type FormEvent } from 'react'
import { api } from '../../services/api'
import { marketplace, bySkillName } from '../marketplace/api'
import { useLoad, words } from '../marketplace/useLoad'
import { LoadState } from '../marketplace/shared'

export function ManagedFilters({ admin, replacement = false, apply }: { admin: boolean; replacement?: boolean; apply: (params: URLSearchParams) => void }) {
  const [values, setValues] = useState<Record<string, string>>({})
  const load = useCallback(() => Promise.all([marketplace.skills(), admin ? api.get<{ id: number; name: string }[]>('/admin/company-types').then(r => r.data) : Promise.resolve([])]), [admin])
  const options = useLoad(load)
  const set = (key: string, value: string) => setValues(old => ({ ...old, [key]: value, ...(key === 'track' ? { skillId: '' } : {}) }))
  function submit(event: FormEvent) {
    event.preventDefault()
    apply(new URLSearchParams(Object.entries(values).filter(([, value]) => value.trim()).map(([key, value]) => [key, value.trim()])))
  }
  const text = (key: string, label: string, type = 'search') => <label key={key}><span className="field-label">{label}</span><input className="form-input" type={type} value={values[key] ?? ''} onChange={e => set(key, e.target.value)} maxLength={key === 'phone' ? 50 : 200} min={key === 'dateTo' ? values.dateFrom : undefined} /></label>
  const select = (key: string, label: string, choices: { value: string; label: string }[]) => <label key={key}><span className="field-label">{label}</span><select className="form-input" value={values[key] ?? ''} onChange={e => set(key, e.target.value)}><option value="">All</option>{choices.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}</select></label>
  const statuses = replacement ? ['ACCEPTED', 'CANCELLED', 'CANDIDATE_SELECTED', 'COMPLETED', 'FAILED', 'HIRING', 'MATCHING', 'REQUESTED', 'WAITING_FOR_CANDIDATE'] : ['ACTIVE', 'COMPLETED', 'PENDING', 'REPLACED', 'TERMINATED']
  return <details className="surface mb-6"><summary className="cursor-pointer font-semibold">Filter {replacement ? 'replacements' : 'placements'}</summary><form onSubmit={submit} className="mt-5 space-y-4"><div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
    {text('candidateName', replacement ? 'Candidate being replaced' : 'Candidate name')}{text('phone', 'Candidate mobile / phone')}{text('jobTitle', replacement ? 'Original job title' : 'Job title')}
    {select('track', 'Candidate track', [{ value: 'TECH', label: 'Tech and Corporate' }, { value: 'TRADE', label: 'Trade' }])}
    {select('skillId', 'Skill', (options.data?.[0] ?? []).filter(s => !values.track || s.category === values.track).sort(bySkillName).map(s => ({ value: String(s.id), label: s.name })))}
    {select('status', replacement ? 'Replacement status' : 'Placement status', statuses.map(value => ({ value, label: words(value) })))}
    {text('dateFrom', replacement ? 'Request date from (UTC)' : 'Start date from', 'date')}{text('dateTo', replacement ? 'Request date to (UTC)' : 'Start date to', 'date')}
    {replacement && select('slaStatus', 'Trade SLA status', ['BREACHED', 'ON_TIME', 'PENDING'].map(value => ({ value, label: words(value) })))}
    {admin && <>{text('companyName', 'Company name')}{select('companyTypeId', 'Company type', (options.data?.[1] ?? []).slice().sort(bySkillName).map(t => ({ value: String(t.id), label: t.name })))}</>}
  </div><LoadState {...options} /><div className="flex flex-wrap gap-3"><button className="button-primary">Apply filters</button><button type="button" className="button-secondary" onClick={() => { setValues({}); apply(new URLSearchParams()) }}>Clear filters</button></div></form></details>
}
