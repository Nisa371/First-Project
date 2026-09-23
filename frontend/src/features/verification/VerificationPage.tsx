import { useTradeText } from '../trade/useTradeText'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useLoad, words } from '../marketplace/useLoad'
import { Workspace, LoadState, Feedback } from '../marketplace/shared'
import { api, apiFailure } from '../../services/api'
import { verification, documentLabel, downloadDocument, type Requirement, type Document } from './api'
import { statusClass } from '../trade/api'

export function VerificationPage() {
  const tr = useTradeText()

  const state = useLoad(verification.checklist)
  const [error, setError] = useState('')
  async function download(id: number) { setError(''); try { await downloadDocument(id) } catch (e) { setError(apiFailure(e).message) } }
  return <Workspace title={tr("যাচাই · Account verification")} subtitle={tr("Upload the documents on your checklist for manual platform review. There is no government NID integration.")}>
    <LoadState {...state} /><Feedback error={error} />
    {state.data && <div className="space-y-6">
      <section className="surface"><div className="flex flex-wrap items-center justify-between gap-4"><div><p className="eyebrow">{tr("Overall verification")}</p><h2 className={`mt-3 text-2xl font-bold ${state.data.status === 'VERIFIED' ? 'text-emerald-700' : 'text-slate-900'}`}>{tr(words(state.data.status))}</h2></div><button className="button-secondary" onClick={state.reload}>{tr("Refresh checklist")}</button></div><p className="mt-4 text-sm text-slate-600">{tr("All required documents must be approved. Only you and authorized administrators can download your documents. Replacing a document starts a new review.")}</p>{state.data.companyTypeRequired && <p role="alert" className="mt-4 rounded-xl bg-amber-50 p-4 text-amber-900"><Link className="font-semibold underline" to="/employer/profile">Choose your company type</Link> to confirm which documents you need.</p>}</section>
      <div className="grid items-start gap-5 lg:grid-cols-2">{state.data.items.map(item => <DocumentCard key={`${item.requirement.id}-${item.submission?.id ?? 'new'}`} requirement={item.requirement} submission={item.submission} reload={state.reload} download={download} />)}</div>
      {!state.data.items.length && <p className="surface">{tr("There are no active document requirements for your account.")}</p>}
      {!!state.data.history.length && <details className="surface"><summary className="cursor-pointer font-bold">{tr("Submission history ·")}{' '}{state.data.history.length}</summary><p className="mt-3 text-sm text-slate-600">{tr("Earlier submissions are retained. Documents for requirements that no longer apply do not count toward your current checklist.")}</p><ul className="mt-4 divide-y divide-slate-100">{state.data.history.map(d => <li key={d.id} className="flex flex-wrap items-center justify-between gap-3 py-4"><div><p className="font-semibold break-words">{tr(d.requirementName)} · {tr(documentLabel(d.status))}</p><p className="text-sm text-slate-500">{new Date(d.submittedAt).toLocaleString()}{d.legacy ? tr(" · Previous platform review") : ''}</p>{d.reviewNote && <p className="mt-2 whitespace-pre-wrap break-words text-sm">{d.reviewNote}</p>}</div>{d.hasFile && <button className="button-secondary" onClick={() => void download(d.id)}>{tr("Download")}<span className="sr-only">{tr("submission")}{' '}{d.id}</span></button>}</li>)}</ul></details>}
    </div>}
  </Workspace>
}
function DocumentCard({ requirement: r, submission: d, reload, download }: { requirement: Requirement; submission: Document | null; reload: () => void; download: (id: number) => Promise<void> }) {
  const tr = useTradeText()

  const [file, setFile] = useState<File | null>(null), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function upload(event: FormEvent) {
    event.preventDefault(); if (!file) return
    if (file.size > 5 * 1024 * 1024) { setError(tr("Choose a file up to 5 MB.")); return }
    setBusy(true); setError('')
    const form = new FormData(); form.append('file', file)
    try { await api.post(`/verifications/me/requirements/${r.id}/document`, form); reload() }
    catch (cause) { setError(apiFailure(cause).message) } finally { setBusy(false) }
  }
  return <article className="surface min-w-0 space-y-4"><div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-xl font-bold break-words">{tr(r.name)}</h2><p className="mt-1 text-sm text-slate-500">{r.required ? tr("Required · আবশ্যক") : tr("Optional")}</p></div><span className={`rounded-full px-3 py-2 text-sm font-semibold ${d ? statusClass(d.status) : 'bg-slate-100 text-slate-600'}`}>{tr(documentLabel(d?.status))}</span></div>
    {r.description && <p className="whitespace-pre-wrap break-words text-sm text-slate-600">{tr(r.description)}</p>}
    {d && <div className="rounded-xl bg-slate-50 p-4"><p className="text-sm break-words">{d.originalName ?? tr("Previous platform identity review")}</p><p className="mt-2 text-xs text-slate-500">{tr("Submitted")}{' '}{new Date(d.submittedAt).toLocaleString()}</p>{d.reviewNote && <p className="mt-3 whitespace-pre-wrap break-words text-sm"><strong>{tr("Review feedback:")}</strong> {d.reviewNote}</p>}{d.hasFile && <button className="button-secondary mt-3" onClick={() => void download(d.id)}>{tr("Download document")}</button>}{d.legacy && <p className="mt-2 text-xs text-slate-500">{tr("Your previous decision has been preserved.")}</p>}</div>}
    <Feedback error={error} /><form onSubmit={upload}><fieldset disabled={busy} className="space-y-3"><label className="block"><span className="field-label">{d ? tr("Replace / resubmit document") : tr("Upload document")}</span><input type="file" required accept=".pdf,.jpg,.jpeg,.png" className="form-input max-w-full text-sm" onChange={e => { setFile(e.target.files?.[0] ?? null); setError('') }} /></label><p className="text-xs text-slate-500">{tr("PDF, JPG or PNG · Maximum 5 MB. Use fictional documents for the showcase.")}</p><button disabled={busy || !file} className="button-primary">{busy ? tr("Uploading…") : d ? tr("Submit replacement") : tr("Submit for review")}</button></fieldset></form>
  </article>
}
