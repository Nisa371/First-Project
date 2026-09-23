import { useIsTrade, useTradeText } from '../trade/useTradeText'
import { assessmentLabel } from '../marketplace/api'
import { useCallback, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, apiFailure } from '../../services/api'
import { useLoad } from '../marketplace/useLoad'
import { Workspace, LoadState, Feedback, Empty } from '../marketplace/shared'
import { VoiceInput } from '../trade/VoiceInput'

interface Message { senderRole: 'AI' | 'CANDIDATE'; content: string; sequenceNumber: number; createdAt: string }
interface Session {
  id: number | null; applicationId: number; jobTitle: string; status: string; currentTurn: number; maxTurns: number
  startedAt: string | null; completedAt: string | null; failureCode: string | null; canStart: boolean; canAnswer: boolean; messages: Message[]
}
interface Review { session: Session; assessmentScore: number | null; summary: string | null }
function providerMessage(code: string | null) {
  if (!code) return ''
  if (code === 'AI_PROVIDER_NOT_CONFIGURED') return 'The AI interview provider is not configured yet. Please return when the service is available.'
  if (code === 'INVALID_AI_RESPONSE') return 'The interview service returned an unusable response. Please contact the platform team.'
  return 'The interview service is temporarily unavailable. Your saved conversation is safe.'
}
function Transcript({ messages }: { messages: Message[] }) {
  const tr = useTradeText()

  return <ol className="space-y-4" aria-label={tr("Assessment conversation")}>{messages.map(m => <li key={m.sequenceNumber} className={`rounded-2xl border p-4 sm:p-5 ${m.senderRole === 'AI' ? 'border-slate-200 bg-white sm:mr-12' : 'border-indigo-100 bg-indigo-50 sm:ml-12'}`}>
    <div className="mb-2 flex items-center justify-between gap-3 text-xs text-slate-500"><strong className="text-slate-800">{m.senderRole === 'AI' ? tr("AI interviewer") : tr("Candidate")}</strong><span>{tr("Message")}{' '}{m.sequenceNumber}</span></div>
    <p className="whitespace-pre-wrap break-words leading-relaxed">{m.content}</p>
  </li>)}</ol>
}
export function ApplicationAssessmentPage() {
  const isTrade = useIsTrade()
  const tr = useTradeText()

  const { id } = useParams()
  const loader = useCallback(() => api.get<Session>(`/candidate/applications/${id}/assessment`).then(r => r.data), [id])
  const state = useLoad(loader), [answer, setAnswer] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const pending = useRef(false)
  const [language, setLanguage] = useState(isTrade ? 'bn-BD' : 'en-US')
  const s = state.data
  async function submit(start: boolean) {
    if (!s || pending.current) return
    pending.current = true; setBusy(true); setError('')
    try {
      const { data } = start
        ? await api.post<Session>(`/candidate/applications/${id}/assessment/start`)
        : await api.post<Session>(`/candidate/assessment/${s.id}/messages`, { response: answer }, { params: { expectedTurn: s.currentTurn } })
      state.setData(data)
      if (data.currentTurn > s.currentTurn) setAnswer('')
    } catch (e) {
      setError(apiFailure(e).message + tr(" Refresh the conversation before trying again. Your typed answer is retained."))
      state.reload()
    } finally { pending.current = false; setBusy(false) }
  }
  return <Workspace title={s?.jobTitle ?? tr("Job assessment")} subtitle={tr("A job-specific interview. Answer in your own words; typing is always available.")}>
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to="/candidate/applications">{tr("← My applications")}</Link>
    <LoadState {...state} /><Feedback error={error} />
    {s && <div className="mx-auto max-w-3xl space-y-6" aria-busy={busy}>
      <section className="surface"><div className="flex flex-wrap items-center justify-between gap-3"><span className="badge">{tr(assessmentLabel(s.status))}</span><span className="text-sm text-slate-600">{s.currentTurn} / {s.maxTurns}{' '}{tr("answers")}</span></div>
        <progress className="mt-4 h-2 w-full accent-indigo-600" value={s.currentTurn} max={s.maxTurns} aria-label={tr("Assessment progress")} />
        {s.failureCode && <p role="status" className="mt-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-900">{tr(providerMessage(s.failureCode))}{' '}{s.canAnswer && tr(" Your latest answer was not submitted; you can edit it and retry.")}</p>}
        {s.status === 'COMPLETED' && <p role="status" className="mt-4 text-emerald-800">{tr("Assessment completed. Your responses have been submitted for this application.")}</p>}
        {s.status === 'FAILED' && <p role="status" className="mt-4 text-slate-700">{tr("Your interview is submitted. Evaluation could not finish; the employer can review its status. Answers are now locked.")}</p>}
        {!s.canStart && !s.canAnswer && !s.completedAt && <p className="mt-4 text-sm text-slate-600">{tr("This application is not currently eligible for an assessment.")}</p>}
        {s.canStart && <div className="mt-4"><p className="mb-4 text-sm leading-relaxed text-slate-600">{tr("One interview per application, with up to")}{' '}{s.maxTurns}{' '}{tr("answers. You can return to this page to continue your saved conversation.")}</p><button className="button-primary" disabled={busy} onClick={() => void submit(true)}>{busy ? tr("Starting…") : tr("Start assessment")}</button></div>}
      </section>
      {s.messages.length > 0 ? <Transcript messages={s.messages} /> : <Empty title={tr("Your conversation will appear here")}>{tr("The first question appears when the interview service starts your assessment.")}</Empty>}
      {s.canAnswer && <form className="surface space-y-5" onSubmit={e => { e.preventDefault(); void submit(false) }}>
        <fieldset disabled={busy} className="space-y-4">{!isTrade && <div><label className="field-label" htmlFor="interview-language">{tr("Voice input language")}</label><select id="interview-language" className="form-input" value={language} onChange={e => setLanguage(e.target.value)}><option value="en-US">English</option><option value="bn-BD">{tr("বাংলা · Bangla")}</option></select></div>}
          <VoiceInput key={language} language={language} mode="assessment" maxLength={4000} disabled={busy} onApply={text => setAnswer(old => `${old}${old ? '\n' : ''}${text}`.slice(0, 4000))} />
          <div><label className="field-label" htmlFor="assessment-answer">{tr("Your answer")}</label><textarea id="assessment-answer" className="form-input" rows={6} required maxLength={4000} value={answer} onChange={e => setAnswer(e.target.value)} aria-describedby="answer-help" /><p id="answer-help" className="mt-2 text-xs text-slate-500">{tr("Review before sending. Submitted answers cannot be edited.")}{' '}{answer.length} / 4000</p></div>
          <button className="button-primary w-full sm:w-auto" disabled={busy || !answer.trim()}>{busy ? tr("Processing answer…") : tr("Send answer")}</button>
        </fieldset>
      </form>}
      <button className="button-secondary" disabled={busy} onClick={state.reload}>{tr("Refresh conversation")}</button>
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
    try { state.setData((await api.post<Review>(`/employer/applications/${id}/assessment/retry-evaluation`)).data) }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  const review = state.data, s = review?.session
  return <Workspace title={s?.jobTitle ?? 'Assessment review'} subtitle="Read-only interview transcript and application-specific evaluation.">
    <Link className="mb-5 inline-block font-semibold text-indigo-700" to={`/employer/jobs/${jobId}/applications/${id}`}>← Applicant details</Link>
    <LoadState {...state} /><Feedback error={error} />
    {s && review && <div className="mx-auto max-w-3xl space-y-6"><section className="surface space-y-4"><span className="badge">{tr(assessmentLabel(s.status))}</span>
      <p className="text-lg font-bold">Assessment score: {review.assessmentScore === null ? 'Not evaluated' : review.assessmentScore.toFixed(2)}</p>
      {s.startedAt && <p className="text-sm text-slate-500">{tr("Started")}{' '}{new Date(s.startedAt).toLocaleString()}</p>}{s.completedAt && <p className="text-sm text-slate-500">{tr("Submitted")}{' '}{new Date(s.completedAt).toLocaleString()}</p>}
      {review.summary && <p className="whitespace-pre-wrap break-words text-slate-700">{review.summary}</p>}
      {s.failureCode && <p role="status" className="text-sm text-amber-800">{tr(providerMessage(s.failureCode))}</p>}
      {s.status === 'FAILED' && s.failureCode !== 'INVALID_AI_RESPONSE' && <button className="button-secondary" disabled={busy} onClick={() => void retry()}>{busy ? 'Evaluating…' : 'Retry final evaluation'}</button>}
    </section>{s.messages.length ? <Transcript messages={s.messages} /> : <Empty title="Assessment not started">No interview messages have been submitted yet.</Empty>}</div>}
  </Workspace>
}
