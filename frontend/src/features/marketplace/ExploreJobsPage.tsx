import { useTradeText } from '../trade/useTradeText'
import { useCallback, useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import type { CompanyType } from '../company-types/CompanyTypeSelector'
import { experience, jobDiscovery, type PublicJob } from './api'
import { useLoad, words } from './useLoad'
import { Workspace, LoadState, Empty, Feedback } from './shared'

const loadTypes = () => api.get<CompanyType[]>('/company-types').then(r => r.data)
const posted = (job: PublicJob) => new Date(job.createdAt).toLocaleDateString()

function ApplyAction({ job, onApplied }: { job: PublicJob; onApplied: () => void }) {
  const tr = useTradeText()

  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function apply() {
    if (busy) return
    setBusy(true); setError('')
    try { await jobDiscovery.apply(job.id); onApplied() }
    catch (e) {
      const failure = apiFailure(e)
      setError(failure.message)
      // Another tab may already have applied; refresh the server's authoritative state.
      if (failure.error === 'ALREADY_APPLIED') onApplied()
    } finally { setBusy(false) }
  }
  return <div><Feedback error={error} />{job.hasApplied && job.applicationStatus
    ? <span role="status" className="badge bg-indigo-50 text-indigo-800">{tr("Application:")}{' '}{tr(words(job.applicationStatus))}</span>
    : <button className="button-primary" disabled={busy || job.status !== 'ACTIVE'} onClick={() => void apply()} aria-label={`${tr('Apply')} · ${job.title}`}>{busy ? tr("Applying…") : job.status === 'ACTIVE' ? tr("Apply") : tr("Job closed")}</button>}</div>
}

function Filters({ initial, onSearch }: { initial: URLSearchParams; onSearch: (params: URLSearchParams) => void }) {
  const tr = useTradeText()

  const types = useLoad(loadTypes)
  const [search, setSearch] = useState(initial.get('search') ?? '')
  const [location, setLocation] = useState(initial.get('location') ?? '')
  const [sector, setSector] = useState(initial.get('companyTypeId') ?? '')
  const [track, setTrack] = useState(initial.get('candidateTrack') ?? '')
  const [min, setMin] = useState(initial.get('minExperience') ?? '')
  const [max, setMax] = useState(initial.get('maxExperience') ?? '')
  const [sort, setSort] = useState(initial.get('sort') ?? 'newest')
  function submit(event: FormEvent) {
    event.preventDefault()
    const params = new URLSearchParams()
    for (const [key, value] of Object.entries({ search: search.trim(), location: location.trim(), companyTypeId: sector, candidateTrack: track, minExperience: min, maxExperience: max, sort })) {
      if (value) params.set(key, value)
    }
    onSearch(params)
  }
  return <form onSubmit={submit} className="surface mb-6 space-y-5" aria-label={tr("Search jobs")}>
    <label className="block"><span className="field-label">{tr("Find your next opportunity")}</span><input className="form-input" type="search" maxLength={200} value={search} onChange={e => setSearch(e.target.value)} placeholder={tr("Search jobs by title, skill, company…")} /></label>
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      <label><span className="field-label">{tr("Location")}</span><input className="form-input" maxLength={255} value={location} onChange={e => setLocation(e.target.value)} placeholder={tr("e.g. Dhaka")} /></label>
      <div><label className="block"><span className="field-label">{tr("Company type / sector")}</span><select className="form-input" value={sector} onChange={e => setSector(e.target.value)} disabled={types.loading}><option value="">{tr("All sectors")}</option>{types.data?.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}{sector && types.data && !types.data.some(t => String(t.id) === sector) && <option value={sector}>{tr("Selected sector (unavailable)")}</option>}</select></label>{types.loading && <p role="status" className="mt-2 text-sm text-slate-500">{tr("Loading sectors…")}</p>}{types.error && <p role="alert" className="mt-2 text-sm text-rose-700">{tr("Unable to load sectors.")}<button type="button" className="underline" onClick={types.reload}>{tr("Retry")}</button></p>}</div>
      <label><span className="field-label">{tr("Candidate track")}</span><select className="form-input" value={track} onChange={e => setTrack(e.target.value)}><option value="">{tr("All tracks")}</option><option value="TECH">{tr("TECH · Professional")}</option><option value="TRADE">{tr("TRADE · Field & household")}</option></select></label>
      <fieldset className="sm:col-span-2"><legend className="field-label">{tr("Experience required (months)")}</legend><div className="grid grid-cols-2 gap-3"><label><span className="field-label text-xs">{tr("Minimum months")}</span><input className="form-input" type="number" min={0} step={1} value={min} onChange={e => setMin(e.target.value)} placeholder={tr("Any")} /></label><label><span className="field-label text-xs">{tr("Maximum months")}</span><input className="form-input" type="number" min={min || 0} step={1} value={max} onChange={e => setMax(e.target.value)} placeholder={tr("Any")} /></label></div><p className="mt-2 text-xs text-slate-500">{tr("Set maximum to 0 for first-time roles, 12 for up to 1 year, or 24 for up to 2 years.")}</p></fieldset>
      <label><span className="field-label">{tr("Sort results")}</span><select className="form-input" value={sort} onChange={e => setSort(e.target.value)}><option value="newest">{tr("Newest first")}</option><option value="oldest">{tr("Oldest first")}</option><option value="experienceAsc">{tr("Lowest experience first")}</option><option value="experienceDesc">{tr("Highest experience first")}</option></select></label>
    </div>
    <div className="flex flex-wrap items-center gap-3"><button className="button-primary">{tr("Search jobs")}</button><button type="button" className="button-secondary" onClick={() => onSearch(new URLSearchParams())}>{tr("Clear filters")}</button><span className="text-sm text-slate-500">{tr("Select filters, then search to update results.")}</span></div>
  </form>
}

export function CandidateJobsPage() {
  const tr = useTradeText()

  const [params, setParams] = useSearchParams()
  const query = params.toString()
  const loader = useCallback(() => jobDiscovery.search(new URLSearchParams(query)), [query])
  const state = useLoad(loader)
  const [filterRevision, setFilterRevision] = useState(0)
  function search(next: URLSearchParams) { setParams(next); setFilterRevision(n => n + 1) }
  function changePage(page: number) { const next = new URLSearchParams(params); next.set('page', String(page)); setParams(next); window.scrollTo({ top: 0, behavior: 'smooth' }) }
  return <Workspace title={tr("Explore Jobs")} subtitle={tr("Discover professional, trade and household opportunities. Find a role that fits your skills and experience.")}>
    <div className="mb-5 flex justify-end"><Link className="font-semibold text-indigo-700" to="/candidate/applications">{tr("My applications →")}</Link></div>
    <Filters key={`${query}:${filterRevision}`} initial={params} onSearch={search} />
    <LoadState {...state} />
    {state.data && <><p role="status" className="mb-4 text-sm text-slate-600">{state.data.totalElements} {state.data.totalElements === 1 ? tr("opportunity") : tr("opportunities")}{' '}{tr("found")}{' '}{state.data.content.length > 0 && ` · ${tr('Page')} ${state.data.page + 1} / ${state.data.totalPages}`}</p>
      {!state.data.content.length ? <Empty title={state.data.totalElements ? tr("No jobs on this page") : tr("No matching jobs")}><p>{tr("Try a broader search or clear your filters to explore available roles.")}</p><button className="button-secondary mt-4" onClick={() => search(new URLSearchParams())}>{tr("Show all jobs")}</button></Empty>
        : <div className="grid gap-5 lg:grid-cols-2">{state.data.content.map(j => <article key={j.id} className="surface flex min-w-0 flex-col"><div className="flex flex-wrap items-center justify-between gap-2"><span className="badge">{tr(j.candidateType)}</span><span className="text-xs text-slate-500">{tr("Posted")}{' '}{posted(j)}</span></div><h2 className="mt-4 break-words text-xl font-bold"><Link to={`/candidate/jobs/${j.id}`} className="hover:text-indigo-700">{j.title}</Link></h2><p className="mt-2 break-words font-medium text-slate-600">{j.companyName} · {j.location}</p><p className="mt-2 break-words text-sm text-slate-500">{j.companyTypeName ?? tr("Sector not specified")}</p><div className="mt-4 flex flex-wrap gap-2"><span className="badge">{tr(experience(j.expectedExperienceMonths))}</span>{j.requiredSkillName && <span className="badge">{j.requiredSkillName}</span>}</div><p className="my-5 line-clamp-3 whitespace-pre-line break-words text-sm leading-relaxed text-slate-600">{j.description}</p><div className="mt-auto flex flex-wrap items-start gap-3"><Link className="button-secondary" to={`/candidate/jobs/${j.id}`}>{tr("View details")}<span className="sr-only">{tr("for")}{' '}{j.title}</span></Link><ApplyAction job={j} onApplied={state.reload} /></div></article>)}</div>}
      {state.data.totalPages > 1 && <nav aria-label={tr("Job result pages")} className="mt-6 flex flex-wrap items-center justify-center gap-4"><button className="button-secondary" disabled={state.data.page === 0} onClick={() => changePage(state.data!.page - 1)}>{tr("Previous")}</button><span className="text-sm">{tr("Page")}{' '}{state.data.page + 1}{' '}{tr("of")}{' '}{state.data.totalPages}</span><button className="button-secondary" disabled={state.data.page + 1 >= state.data.totalPages} onClick={() => changePage(state.data!.page + 1)}>{tr("Next")}</button></nav>}
    </>}
  </Workspace>
}

export function CandidateJobPage() {
  const tr = useTradeText()

  const { id = '' } = useParams()
  const loader = useCallback(() => jobDiscovery.detail(id), [id]), state = useLoad(loader)
  const j = state.data
  return <Workspace title={j?.title ?? tr("Job details")} subtitle={tr("Review the role before submitting your application.")}>
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to="/candidate/jobs">{tr("← Explore Jobs")}</Link><LoadState {...state} />
    {j && <section className="surface max-w-3xl space-y-6"><div><p className="break-words font-semibold">{j.companyName} · {j.location}</p><p className="mt-2 break-words text-sm text-slate-500">{j.companyTypeName ?? tr("Sector not specified")}{' '}{tr("· Posted")}{' '}{posted(j)}</p></div><div className="flex flex-wrap gap-2"><span className="badge">{tr(j.candidateType)}</span><span className="badge">{tr(words(j.status))}</span><span className="badge">{tr(experience(j.expectedExperienceMonths))}</span></div><div><h2 className="font-bold">{tr("About the role")}</h2><p className="mt-3 whitespace-pre-wrap break-words leading-relaxed text-slate-600">{j.description}</p></div><div><h2 className="font-bold">{tr("What the employer expects")}</h2><p className="mt-3 whitespace-pre-wrap break-words leading-relaxed text-slate-600">{j.publicExpectations || tr("See the responsibilities and requirements above.")}</p>{j.requiredSkillName && <p className="mt-3 text-sm">{tr("Required skill:")}{' '}{j.requiredSkillName}</p>}</div><ApplyAction key={j.id} job={j} onApplied={state.reload} /><Link className="inline-block font-semibold text-indigo-700" to="/candidate/applications">{tr("My applications →")}</Link></section>}
  </Workspace>
}
