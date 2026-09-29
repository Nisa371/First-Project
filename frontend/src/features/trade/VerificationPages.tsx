import { useCallback, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { Workspace, LoadState, Field, Feedback } from '../marketplace/shared'
import { useLoad, words } from '../marketplace/useLoad'
import { tradeApi, statusClass, type VerificationReview } from './api'
import { VerificationReviews } from '../verification/VerificationReviews'
import { api, apiFailure } from '../../services/api'
export { VerificationPage } from '../verification/VerificationPage'
export function VerificationQueuePage() {
  return <Workspace title="Verification work queue" subtitle="Review candidate and employer submissions, inspect evidence and record a decision."><VerificationReviews /></Workspace>
}
export function VerificationReviewPage() {
  const { id } = useParams(), loader = useCallback(() => tradeApi.detail(Number(id)), [id]), state = useLoad(loader)
  return <Workspace title="Review verification" subtitle="Private verification evidence is available only to authorized reviewers."><LoadState {...state} />{state.data && <Review initial={state.data} key={state.data.id} />}</Workspace>
}
function Review({ initial }: { initial: VerificationReview }) {
  const [v, setReview] = useState(initial), [decision, setDecision] = useState('VERIFIED'), [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function submit(e: FormEvent) {
    e.preventDefault(); setBusy(true); setError('')
    try { setReview((await api.post<VerificationReview>(`/evaluator/verifications/${v.id}/review`, { status: decision, notes })).data) }
    catch (err) { setError(apiFailure(err).message) } finally { setBusy(false) }
  }
  return <section className="surface space-y-5"><h2 className="text-2xl font-bold">{v.candidateName}</h2><p className="whitespace-pre-wrap break-words">{v.identityReference}</p><p className={`rounded-xl p-4 font-semibold ${statusClass(v.status)}`}>{words(v.status)}</p><p className="whitespace-pre-wrap break-words">{v.notes}</p><Feedback error={error} />{['PENDING', 'IN_REVIEW'].includes(v.status) && <form onSubmit={submit}><fieldset disabled={busy} className="space-y-4"><label className="field-label" htmlFor="legacy-decision">Decision</label><select id="legacy-decision" className="form-input" value={decision} onChange={e => setDecision(e.target.value)}><option value="VERIFIED">Approve</option><option value="FAILED">Reject</option><option value="FLAGGED">Flag</option></select><Field name="legacy-notes" label="Reviewer notes" value={notes} onChange={setNotes} required multiline maxLength={3000} /><button className="button-primary">{busy ? 'Recording…' : 'Record decision'}</button></fieldset></form>}<Link className="inline-block font-semibold text-indigo-700" to="/evaluator/verifications">← Back to verification queue</Link></section>
}
