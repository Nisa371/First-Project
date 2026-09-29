import { NavigationIcon } from '../../layouts/NavigationIcon'
import { useState } from 'react'
import { Modal } from '../../components/Modal'
import { api, apiFailure } from '../../services/api'
import { useLoad } from './useLoad'
import { Feedback, LoadState } from './shared'

const load = () => Promise.all([
  api.get<{ acknowledgedAt: string | null }>('/employers/me/notice').then(r => r.data),
  api.get<{ days: number }>('/placements/replacement-guarantee').then(r => r.data),
])
export function EmployerNotice() {
  const state = useLoad(load)
  const [manual, setManual] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState('')
  const firstVisit = !!state.data && !state.data[0].acknowledgedAt
  async function acknowledge() {
    setBusy(true); setError('')
    try {
      const result = await api.post<{ acknowledgedAt: string }>('/employers/me/notice/acknowledge')
      if (state.data) state.setData([result.data, state.data[1]])
      setManual(false)
    } catch (e) { setError(apiFailure(e).message) } finally { setBusy(false) }
  }
  return <>
    <section className="surface mb-6 flex flex-wrap items-center justify-between gap-4 border-slate-200">
      <div className="flex items-start gap-3"><svg aria-hidden="true" className="mt-1 size-6 shrink-0 text-indigo-700" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><circle cx="12" cy="12" r="9" /><path d="M12 11v6m0-10v1" /></svg><div><h2 className="font-bold">Important employer notice</h2><p className="mt-1 text-sm text-slate-600">Understand the platform benefits, fees and replacement policy.</p></div></div>
      <button className="button-secondary" onClick={() => { setManual(true); state.reload() }}>Read notice</button>
      {!manual && <LoadState {...state} />}
    </section>
    {(manual || firstVisit) && <Modal title="Welcome to Verified Career — Please review this important notice" close={() => { if (!busy && !firstVisit) setManual(false) }}>
      <div className="max-h-[65vh] space-y-4 overflow-y-auto pr-1 text-sm leading-7 text-[var(--ink)]">
        <p>Review the benefits, fees and replacement policy before hiring.</p>
        <LoadState {...state} />
        <section className="rounded-xl border border-[var(--line)] p-4"><h3 className="mb-3 flex items-center gap-3 rounded-lg bg-slate-50 p-3 text-base font-bold"><NavigationIcon name="shield" />Verified talent</h3>
          <p>Identity and required documents are reviewed manually by the platform. Candidates may also provide skill-related supporting documents. Only documents explicitly approved by a reviewer have been reviewed and approved; account verification does not certify every credential.</p>
        </section>
        <section className="rounded-xl border border-[var(--line)] p-4"><h3 className="mb-3 flex items-center gap-3 rounded-lg bg-slate-50 p-3 text-base font-bold"><NavigationIcon name="clipboard" />AI-assisted ranking</h3>
          <p>Candidate evaluation considers CV, portfolio, experience and assessment. Ranking uses the Employer-selected weights for these factors to support hiring decisions.</p>
        </section>
        <section className="rounded-xl border border-[var(--line)] p-4"><h3 className="mb-3 flex items-center gap-3 rounded-lg bg-slate-50 p-3 text-base font-bold"><NavigationIcon name="refresh" />Free replacement guarantee</h3>
          <p>{state.data ? `Replacement is available within ${state.data[1].days} days according to policy when replacement-guaranteed coverage is selected. Your placement’s recorded guarantee deadline applies.` : 'Loading the current guarantee period…'}</p>
        </section>
        <section className="rounded-xl border border-[var(--line)] p-4"><h3 className="mb-3 flex items-center gap-3 rounded-lg bg-slate-50 p-3 text-base font-bold"><NavigationIcon name="payment" />What you pay</h3>
          <ul className="space-y-3"><li><strong>Normal job:</strong> the existing job-post publication fee applies.</li>
            <li><strong>Normal successful placement:</strong> a one-time service fee of <strong>20% of the agreed first-month salary</strong>. The service team may contact you to verify the declared salary and arrange settlement.</li>
            <li className="rounded-lg bg-emerald-50 p-3 text-emerald-950"><strong>Covered replacement: FREE.</strong> No second job-post fee, no additional placement service fee and no second 20% charge.</li></ul>
          <p className="mt-3">Replacement placements covered by the guarantee are provided without an additional placement service fee because the service fee applies only to the original successful placement.</p>
        </section>
      </div>
      <div className="mt-4 border-t border-[var(--line)] pt-4"><Feedback error={error} /><button className="button-primary" disabled={busy || state.loading || !state.data || !!state.error} onClick={() => void acknowledge()}>{busy ? 'Saving…' : 'I understand'}</button></div>
    </Modal>}
  </>
}
