import { useTradeText } from '../trade/useTradeText'
import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router'
import { payments } from './api'
import { useLoad, words } from '../marketplace/useLoad'
import { Workspace, LoadState, Feedback } from '../marketplace/shared'
import { apiFailure } from '../../services/api'
export function DemoPaymentPage() {
  const tr = useTradeText()

  const { id = '' } = useParams()
  const load = useCallback(() => payments.get(id), [id])
  const state = useLoad(load)
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  const payment = state.data
  async function finish(cancel: boolean) {
    if (!payment) return
    setBusy(true); setError('')
    try { state.setData(await (cancel ? payments.cancel(payment.id) : payments.complete(payment.id))) }
    catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  return <Workspace title={tr("Demo Payment")} subtitle={tr("No Real Money Will Be Charged")}><LoadState {...state} /><Feedback error={error} success={payment?.status === 'SUCCESS' ? (payment.purpose === 'JOB_POSTING' ? 'Demo payment successful. Your job is now published.' : tr("Demo payment successful. Your appointment is confirmed.")) : ''} />
    {payment && <section className="surface mx-auto max-w-2xl"><p className="rounded-xl bg-amber-50 p-4 text-sm leading-relaxed text-amber-900">{tr("This is a demo payment. No real payment will be made. No bank, card or mobile banking details are needed.")}</p>
      <h2 className="mt-6 break-words text-xl font-bold">{payment.purpose === 'SESSION_BOOKING' ? tr(payment.description) : payment.description}</h2><p className="mt-2 text-slate-600">{tr(words(payment.purpose))}</p>
      <p className="my-6 text-4xl font-bold">{payment.amount.toLocaleString('en-BD', { minimumFractionDigits: 2 })} <span className="text-xl">{payment.currency}</span></p>
      <dl className="space-y-3 text-sm"><div><dt className="text-slate-500">{tr("Internal demo reference")}</dt><dd className="mt-1 break-all font-mono">{payment.reference}</dd></div><div><dt className="text-slate-500">{tr("Status")}</dt><dd className="badge mt-1">{tr(words(payment.status))}</dd></div></dl>
      {payment.status === 'PENDING' && <><p className="mt-5 text-sm text-slate-600">{payment.purpose === 'JOB_POSTING' ? 'Complete this simulation to publish your job for applicants.' : tr("Your place is confirmed only after payment succeeds, subject to current availability.")}</p><div className="mt-6 flex flex-wrap gap-3"><button disabled={busy} className="button-primary" onClick={() => void finish(false)}>{busy ? tr("Processing…") : tr("Complete Demo Payment")}</button><button disabled={busy} className="button-secondary" onClick={() => void finish(true)}>{tr("Cancel Payment")}</button></div></>}
      {payment.status === 'CANCELLED' && <p className="mt-5 text-sm">{tr("Demo payment cancelled.")}{' '}{payment.purpose === 'JOB_POSTING' ? 'Your job remains unpublished. You can resume payment from your jobs dashboard.' : tr("Your appointment was not confirmed. Choose a session to try again.")}</p>}
      <Link className="workspace-link mt-6 inline-block" to={payment.purpose === 'JOB_POSTING' ? '/employer/jobs' : '/candidate/bookings'}>{payment.purpose === 'JOB_POSTING' ? 'Return to jobs' : tr("Return to bookings")}</Link>
    </section>}
  </Workspace>
}
