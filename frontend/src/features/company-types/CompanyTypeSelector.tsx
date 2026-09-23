import { useId, useState } from 'react'
import { api } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'

export interface CompanyType { id: number; name: string; active: boolean; other: boolean }
const loadTypes = () => api.get<CompanyType[]>('/company-types').then(r => r.data)
export function CompanyTypeSelector({ value, custom, selected, onChange, error }: {
  value: number | null; custom: string; selected?: CompanyType; error?: string
  onChange: (type: CompanyType | undefined, custom: string) => void
}) {
  const state = useLoad(loadTypes)
  const [query, setQuery] = useState('')
  const id = useId()
  const current = state.data?.find(t => t.id === value) ?? (selected?.id === value ? selected : undefined)
  const options = (state.data ?? []).filter(t => t.name.toLowerCase().includes(query.toLowerCase()))
  if (current && !options.some(t => t.id === current.id)) options.unshift(current)
  return <div className="space-y-3">
    <label className="block"><span className="field-label">Search company types</span><input type="search" className="form-input" value={query} onChange={e => setQuery(e.target.value)} placeholder="Search, e.g. pharmaceuticals" /></label>
    {state.loading && <p role="status" className="text-sm text-slate-600">Loading company types…</p>}
    {state.error && <div role="alert" className="text-sm text-rose-700">{state.error} <button type="button" className="underline" onClick={state.reload}>Retry</button></div>}
    <div><label htmlFor={`${id}-type`} className="field-label">Company type</label><select id={`${id}-type`} required className="form-input" value={value ?? ''} aria-invalid={!!error} aria-describedby={error ? `${id}-error` : undefined} onChange={e => { const type = options.find(t => t.id === Number(e.target.value)); onChange(type, type?.other ? custom : '') }}>
      <option value="">Select company type</option>
      {options.map(t => <option key={t.id} value={t.id}>{t.name}{!t.active ? ' (inactive — current selection)' : ''}</option>)}
    </select></div>
    {!state.loading && !state.error && !options.length && <p role="status" className="text-sm text-slate-600">No company types found. Try another search.</p>}
    {current?.other && <label className="block"><span className="field-label">Specify company type</span><input className="form-input" required maxLength={120} value={custom} onChange={e => onChange(current, e.target.value)} /></label>}
    {error && <p id={`${id}-error`} role="alert" className="text-sm text-rose-700">{error}</p>}
  </div>
}
