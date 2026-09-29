import { useIsTrade, useTradeText } from '../trade/useTradeText'
import { assessmentLabel } from '../marketplace/api'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { Workspace, LoadState, Feedback, Empty } from '../marketplace/shared'
import { Modal } from '../../components/Modal'
import { HoldToTalk } from '../trade/HoldToTalk'

interface Message { senderRole: 'AI' | 'CANDIDATE'; content: string; sequenceNumber: number; createdAt: string }
interface Session {
  id: number | null; applicationId: number; jobTitle: string; status: string; currentTurn: number; maxTurns: number
  deadlineAt: string | null; serverNow: string; receivedAt?: number; startedAt: string | null; completedAt: string | null; failureCode: string | null; canStart: boolean; canAnswer: boolean; messages: Message[]
}
interface Review { session: Session; assessmentScore: number | null; summary: string | null }
function providerMessage(code: string | null) {
  if (!code) return ''
  if (code === 'AI_PROVIDER_NOT_CONFIGURED') return 'The AI interview provider is not configured yet. Please return when the service is available.'
  if (code === 'INVALID_AI_RESPONSE') return 'The interview service returned an unusable response. Please contact the platform team.'
  return 'The AI service is temporarily unavailable. Please try again. Your saved conversation is safe.'
}
function Transcript({ messages }: { messages: Message[] }) {
  const tr = useTradeText()

  return <ol className="space-y-4" aria-label={tr("Assessment conversation")}>{messages.map(m => <li key={m.sequenceNumber} className={`rounded-2xl border p-4 sm:p-5 ${m.senderRole === 'AI' ? 'border-slate-200 bg-white sm:mr-12' : 'border-indigo-100 bg-indigo-50 sm:ml-12'}`}>
    <div className="mb-2 flex items-center justify-between gap-3 text-xs text-slate-500"><strong className="text-slate-800">{m.senderRole === 'AI' ? tr("AI interviewer") : tr("Candidate")}</strong><span>{tr("Message")}{' '}{m.sequenceNumber}</span></div>
    <p className="whitespace-pre-wrap break-words leading-relaxed">{m.content}</p>
  </li>)}</ol>
}
export function ApplicationAssessmentPage() {
  const tr = useTradeText()
  const trade = useIsTrade()
  const [confirmStart, setConfirmStart] = useState(false)

  const { id } = useParams()
  const loader = useCallback(() => api.get<Session>(`/candidate/applications/${id}/assessment`, { timeout: 40000 }).then(r => ({ ...r.data, receivedAt: performance.now() })), [id])
  const state = useLoad(loader), [answer, setAnswer] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const pending = useRef(false), requestVersion = useRef(0)
  const s = state.data
  const [tick, setTick] = useState(() => performance.now())
  const update = useRef(state.setData)
  update.current = state.setData
  const remaining = s?.deadlineAt ? Math.max(0, Math.ceil((Date.parse(s.deadlineAt) - Date.parse(s.completedAt ?? s.serverNow) - (s.completedAt ? 0 : Math.max(0, tick - (s.receivedAt ?? tick)))) / 1000)) : null
  const expired = remaining === 0
  useEffect(() => {
    if (s?.status !== 'IN_PROGRESS') return
    const timer = window.setInterval(() => setTick(performance.now()), 250)
    return () => window.clearInterval(timer)
  }, [s?.status])
  useEffect(() => {
    if (s?.status !== 'IN_PROGRESS') return
    let active = true, refreshing = false
    const refresh = async () => {
      if (pending.current || refreshing) return
      refreshing = true
      const version = requestVersion.current
      try { const data = await loader(); if (active && !pending.current && version === requestVersion.current) { update.current(data); setTick(performance.now()) } }
      catch { /* Keep the immutable deadline and typed answer during network loss. */ }
      finally { refreshing = false }
    }
    const timer = window.setInterval(() => void refresh(), expired ? 2000 : 10000)
    const resume = () => { if (document.visibilityState === 'visible') void refresh() }
    window.addEventListener('focus', resume); window.addEventListener('online', resume); document.addEventListener('visibilitychange', resume)
    if (expired) void refresh()
    return () => { active = false; window.clearInterval(timer); window.removeEventListener('focus', resume); window.removeEventListener('online', resume); document.removeEventListener('visibilitychange', resume) }
  }, [loader, s?.status, expired])
  async function submit(start: boolean) {
    if (!s || pending.current || (!start && expired)) return
    pending.current = true; requestVersion.current++; setBusy(true); setError('')
    try {
      const { data } = start
        ? await api.post<Session>(`/candidate/applications/${id}/assessment/start`, undefined, { timeout: 40000 })
        : await api.post<Session>(`/candidate/assessment/${s.id}/messages`, { response: answer }, { params: { expectedTurn: s.currentTurn }, timeout: 40000 })
      state.setData({ ...data, receivedAt: performance.now() }); setTick(performance.now())
      if (data.currentTurn > s.currentTurn) setAnswer('')
    } catch (e) {
      setError(apiFailure(e).message)
      try { state.setData(await loader()) } catch { /* Retain answer and countdown offline. */ }
    } finally { pending.current = false; setBusy(false) }
  }
  return <Workspace title={s?.jobTitle ?? tr("Job assessment")} subtitle={tr("A job-specific interview. Answer in your own words; typing is always available.")}>
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to={`/candidate/applications/${id}`}>{tr("← Application details")}</Link>
    {!error && <LoadState {...state} />}<Feedback error={error} />
    {confirmStart && s?.canStart && !s.startedAt && <Modal title={trade ? 'শুরু করার আগে' : 'Before you start'} close={() => setConfirmStart(false)}>
      <p lang={trade ? 'bn' : 'en'} className="leading-relaxed text-slate-600">{trade ? 'এই মূল্যায়নের সময় ৫ মিনিট। একবার শুরু করলে সময় আর থামানো, বিরতি দেওয়া বা নতুন করে শুরু করা যাবে না। তাই প্রস্তুত হলে তবেই শুরু করুন।' : 'This assessment has a 5-minute time limit. Once the assessment starts, the timer cannot be paused, stopped, or reset. Start only when you are ready.'}</p>
      <div className="mt-6 flex flex-wrap justify-end gap-3">
        <button type="button" className="button-secondary" autoFocus onClick={() => setConfirmStart(false)}>{trade ? 'এখন নয়' : 'Not now'}</button>
        <button type="button" className="button-primary" disabled={busy} onClick={() => { setConfirmStart(false); void submit(true) }}>{trade ? 'মূল্যায়ন শুরু করুন' : 'Start assessment'}</button>
      </div>
    </Modal>}
    {s && <div className="mx-auto max-w-3xl space-y-6 pb-32" aria-busy={busy}>
      <section className="surface">
        {remaining !== null && <div className={`mb-4 flex items-center justify-between rounded-xl p-4 ${remaining <= 60 ? 'bg-amber-50 text-amber-900' : 'bg-indigo-50 text-indigo-900'}`}><span className="font-semibold">{tr('Time remaining')}</span><span role="timer" aria-label={tr('Time remaining')} className="text-2xl font-bold tabular-nums">{String(Math.floor(remaining / 60)).padStart(2, '0')}:{String(remaining % 60).padStart(2, '0')}</span></div>}
        {expired && s.status === 'IN_PROGRESS' && <p role="status" className="mb-4 text-amber-900">{tr('Time is up. Your saved answers are locked and are being evaluated.')}</p>}
        <div className="flex flex-wrap items-center justify-between gap-3"><span className="badge">{tr(assessmentLabel(s.status))}</span><span className="text-sm text-slate-600">{s.currentTurn} / {s.maxTurns}{' '}{tr("answers")}</span></div>
        <progress className="mt-4 h-2 w-full accent-indigo-600" value={s.currentTurn} max={s.maxTurns} aria-label={tr("Assessment progress")} />
        {!error && s.failureCode && <p role="status" className="mt-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">{tr(providerMessage(s.failureCode))}{' '}{s.canAnswer && !expired && tr(" Your latest answer was not submitted; you can edit it and retry.")}</p>}
        {s.status === 'COMPLETED' && <p role="status" className="mt-4 text-emerald-800">{tr("Assessment completed. Your responses have been submitted for this application.")}</p>}
        {s.status === 'FAILED' && <p role="status" className="mt-4 text-slate-700">{tr("Your interview is submitted. Evaluation could not finish; the employer can review its status. Answers are now locked.")}</p>}
        {!s.canStart && !s.canAnswer && !s.completedAt && <p className="mt-4 text-sm text-slate-600">{tr("This application is not currently eligible for an assessment.")}</p>}
        {s.canStart && <div className="mt-4"><p className="mb-4 text-sm leading-relaxed text-slate-600">{tr("One interview per application, with up to")}{' '}{s.maxTurns}{' '}{tr("answers. You can return to this page to continue your saved conversation.")}</p><button className="button-primary" disabled={busy} onClick={() => { if (s.startedAt) void submit(true); else setConfirmStart(true) }}>{busy ? tr("Starting…") : tr("Start assessment")}</button></div>}
      </section>
      {s.messages.length > 0 ? <Transcript messages={s.messages} /> : <Empty title={tr("Your conversation will appear here")}>{tr("The first question appears when the interview service starts your assessment.")}</Empty>}
      {s.canAnswer && <form className="surface space-y-5" onSubmit={e => { e.preventDefault(); void submit(false) }}>
        <fieldset disabled={busy || expired} className="space-y-4">
          <div><label className="field-label" htmlFor="assessment-answer">{tr("Your answer")}</label><textarea id="assessment-answer" className="form-input" rows={6} required maxLength={4000} value={answer} onChange={e => setAnswer(e.target.value)} aria-describedby="answer-help" /><p id="answer-help" className="mt-2 text-xs text-slate-500">{tr("Review before sending. Submitted answers cannot be edited.")}{' '}{answer.length} / 4000</p></div>
          <button className="button-primary w-full sm:w-auto" disabled={busy || expired || !answer.trim()}>{busy ? tr("Processing answer…") : tr("Send answer")}</button>
        </fieldset>
      </form>}
      {s.canAnswer && <HoldToTalk targetId="assessment-answer" disabled={busy || expired} />}
      <button className="button-secondary" disabled={busy} onClick={() => { setError(''); state.reload() }}>{tr("Refresh conversation")}</button>
    </div>}
  </Workspace>
}
export function EmployerAssessmentPage() {
  const tr = useTradeText()

  const { jobId, id } = useParams()
  const loader = useCallback(() => api.get<Review>(`/employer/applications/${id}/assessment`).then(r => r.data), [id])
  const state = useLoad(loader), [busy, setBusy] = useState(false), [error, setError] = useState('')
  async function retry() {
    if (busy) return
    setBusy(true); setError('')
    try { state.setData((await api.post<Review>(`/employer/applications/${id}/assessment/retry-evaluation`, undefined, { timeout: 40000 })).data) }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  const review = state.data, s = review?.session
  return <Workspace title={s?.jobTitle ?? 'Assessment review'} subtitle="Read-only interview transcript and application-specific evaluation.">
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to={`/employer/jobs/${jobId}/applications/${id}`}>← Applicant details</Link>
    {!error && <LoadState {...state} />}<Feedback error={error} />
    {s && review && <div className="mx-auto max-w-3xl space-y-6"><section className="surface space-y-4"><span className="badge">{tr(assessmentLabel(s.status))}</span>
      <p className="text-lg font-bold">Assessment score: {review.assessmentScore === null ? 'Not evaluated' : review.assessmentScore.toFixed(2)}</p>
      {s.startedAt && <p className="text-sm text-slate-500">{tr("Started")}{' '}{new Date(s.startedAt).toLocaleString()}</p>}{s.completedAt && <p className="text-sm text-slate-500">{tr("Submitted")}{' '}{new Date(s.completedAt).toLocaleString()}</p>}
      {review.summary && <p className="whitespace-pre-wrap break-words text-slate-700">{review.summary}</p>}
      {!error && s.failureCode && <p role="status" className="text-sm text-amber-800">{tr(providerMessage(s.failureCode))}</p>}
      {s.status === 'FAILED' && s.failureCode !== 'INVALID_AI_RESPONSE' && <button className="button-secondary" disabled={busy} onClick={() => void retry()}>{busy ? 'Evaluating…' : 'Retry final evaluation'}</button>}
    </section>{s.messages.length ? <Transcript messages={s.messages} /> : <Empty title="Assessment not started">No interview messages have been submitted yet.</Empty>}</div>}
  </Workspace>
}
