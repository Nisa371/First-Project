import { useTradeText } from '../trade/useTradeText'
import { Link } from 'react-router'
import { assessmentApi as service, time } from './api'
import { useLoad } from '../marketplace/useLoad'
import { LoadState } from '../marketplace/shared'
const load = async () => {
  const [attempts, bookings] = await Promise.all([service.mine(), service.bookings()])
  const next = bookings.filter(b => b.status === 'BOOKED' && new Date(b.slot.startTime).getTime() > Date.now()).sort((a, b) => a.slot.startTime.localeCompare(b.slot.startTime))[0]
  return { attempts, next }
}
export function CareerActivity() {
  const tr = useTradeText()

  const state = useLoad(load)
  return <section className="surface mb-6"><h2 className="mb-5 text-xl font-bold">{tr("Your next steps")}</h2><LoadState {...state} />{state.data && <div className="grid gap-6 sm:grid-cols-2"><div><p className="font-semibold">{tr("Assessments & feedback")}</p><p className="my-3 text-sm text-slate-600">{state.data.attempts.length ? `${state.data.attempts.filter(a => a.status === 'IN_PROGRESS').length} ${tr('In progress')} · ${state.data.attempts.filter(a => a.status === 'SUBMITTED').length} ${tr('Awaiting review')} · ${state.data.attempts.filter(a => a.result).length} ${tr('Results released')}` : tr("Start your first assessment and show employers what you know.")}</p><Link className="text-sm font-semibold text-indigo-700" to="/candidate/assessments">{tr("Open assessments →")}</Link></div><div><p className="font-semibold">{tr("Next appointment")}</p><p className="my-3 text-sm text-slate-600">{state.data.next ? time(state.data.next.slot.startTime) : tr("No upcoming appointment. Book time to discuss your career.")}</p><Link className="text-sm font-semibold text-indigo-700" to="/candidate/bookings">{tr("Manage appointments →")}</Link></div></div>}</section>
}
