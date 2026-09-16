import { useCallback, useState } from 'react'
import { useAuth } from '../auth/useAuth'
import { api, apiFailure } from '../../services/api'
import { useLoad, words } from '../marketplace/useLoad'
import { Empty, Feedback, LoadState, Workspace } from '../marketplace/shared'
interface Program { id: number; title: string; providerName: string; description: string; skill: string | null }
interface Referral { id: number; candidate: string; program: Program; status: string; createdAt: string }
interface Eligible { evaluationId: number; candidate: string; assessment: string }
export function TrainingPage() {
  const { user } = useAuth(); const staff = user?.role !== 'CANDIDATE'
  const loader = useCallback(async () => {
    const [programs, referrals, eligible] = await Promise.all([api.get<Program[]>('/training-programs'), api.get<Referral[]>('/referrals'), staff ? api.get<Eligible[]>('/referrals/eligible') : Promise.resolve({ data: [] as Eligible[] })])
    return { programs: programs.data, referrals: referrals.data, eligible: eligible.data }
  }, [staff])
  const state = useLoad(loader)
  const [evaluationId, setEvaluation] = useState(''); const [programId, setProgram] = useState('')
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [success, setSuccess] = useState('')
  async function refer(e: React.FormEvent) {
    e.preventDefault(); setBusy(true); setError(''); setSuccess('')
    try { await api.post('/referrals', { evaluationId: Number(evaluationId), programId: Number(programId) }); setSuccess('Referral created. The candidate has been notified.'); setEvaluation(''); setProgram(''); state.reload() }
    catch (err) { setError(apiFailure(err).message) } finally { setBusy(false) }
  }
  return <Workspace title={user?.candidateType === 'TRADE' ? 'প্রশিক্ষণ · Your next step' : 'A little learning. More possibility.'} subtitle="Explore practical learning pathways. A referral gives you a next step to discuss with your evaluator; it does not automatically enroll you.">
    <Feedback error={error} success={success} /><LoadState {...state} />
    {state.data && <div className="space-y-8">
      {staff && <section className="surface"><h2 className="text-xl font-bold">Refer a candidate</h2><p className="mt-2 text-sm text-slate-600">Choose a released Needs training evaluation{user?.role === 'EVALUATOR' ? ' that you reviewed' : ''}.</p>
        {state.data.eligible.length ? <form onSubmit={refer} className="mt-5 grid gap-4 sm:grid-cols-2"><div><label htmlFor="evaluation" className="field-label">Candidate & evaluation</label><select id="evaluation" className="form-input" required value={evaluationId} onChange={e => setEvaluation(e.target.value)}><option value="">Choose a candidate</option>{state.data.eligible.map(e => <option key={e.evaluationId} value={e.evaluationId}>{e.candidate} · {e.assessment}</option>)}</select></div><div><label htmlFor="program" className="field-label">Training program</label><select id="program" required className="form-input" value={programId} onChange={e => setProgram(e.target.value)}><option value="">Choose a program</option>{state.data.programs.map(p => <option key={p.id} value={p.id}>{p.title}</option>)}</select></div><button disabled={busy} className="button-primary sm:col-span-2">{busy ? 'Sending referral…' : 'Create referral & notify candidate'}</button></form> : <p className="mt-5 rounded-xl bg-slate-50 p-4">No eligible evaluations yet. Release a Needs training result before making a referral.</p>}
      </section>}
      <section><h2 className="mb-4 text-xl font-bold">{staff ? 'Referral activity' : 'My referrals · আমার প্রশিক্ষণ'}</h2><div className="grid gap-4 sm:grid-cols-2">{state.data.referrals.map(r => <article key={r.id} className="surface"><span className="badge">{words(r.status)}</span><h3 className="mt-4 text-lg font-bold">{r.program.title}</h3><p className="mt-2 text-sm text-slate-600">{staff ? `${r.candidate} · ` : ''}{r.program.providerName}</p><p className="mt-3 leading-relaxed text-slate-600">{r.program.description}</p><p className="mt-4 text-sm text-slate-500">Referred {new Date(r.createdAt).toLocaleDateString()}</p></article>)}</div>{!state.data.referrals.length && <Empty title="Your next chapter starts here">No referrals yet. Your evaluator can recommend a program after reviewing your assessment.</Empty>}</section>
      <section><h2 className="mb-4 text-xl font-bold">Learning catalog</h2><div className="grid gap-4 sm:grid-cols-2">{state.data.programs.map(p => <article key={p.id} className="surface"><p className="eyebrow">{p.skill ?? 'FOUNDATION SKILLS'}</p><h3 className="mt-3 text-xl font-bold">{p.title}</h3><p className="mt-2 text-sm font-medium text-indigo-700">{p.providerName}</p><p className="mt-4 leading-relaxed text-slate-600">{p.description}</p></article>)}</div>{!state.data.programs.length && <Empty title="New programs are on the way">Check back with your evaluator for learning options.</Empty>}</section>
    </div>}
  </Workspace>
}
