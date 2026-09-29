export const validPlacementSalary = (salary: string) => /^\d+(\.\d{1,2})?$/.test(salary) && Number(salary) > 0 && Number(salary) <= 999999999999.99

export function PlacementFeeAgreement({ salary, accepted, onSalary, onAccepted, disabled = false }: {
  salary: string; accepted: boolean; onSalary: (value: string) => void; onAccepted: (value: boolean) => void; disabled?: boolean
}) {
  const valid = validPlacementSalary(salary)
  return <section aria-label="Employer Placement Fee Agreement" className="space-y-5 rounded-xl border border-[var(--line)] bg-[var(--surface)] p-4 text-[var(--ink)] sm:p-5">
    <h3 className="text-lg font-bold">Employer Placement Fee Agreement</h3>
    <div className="space-y-5 text-sm leading-7">
      <section><h4 className="font-bold">Service fee · 20%, once</h4><p>By confirming a normal placement, the Employer acknowledges that a one-time platform service fee equal to twenty percent (20%) of the Candidate’s agreed first-month salary becomes payable for the successful placement.</p></section>
      <section><h4 className="font-bold">Salary verification</h4><p>The Employer declares that the “Agreed first-month salary (BDT)” entered here is accurate. After the placement is finalized, the service team may contact the Employer to verify the declared salary.</p><p className="mt-2">The platform may request reasonable supporting evidence such as the Candidate’s appointment letter, employment agreement, first-month salary payment record/bank statement, or other relevant proof.</p></section>
      <section className="rounded-lg bg-slate-50 p-4"><h4 className="font-bold">Free replacement policy</h4><p>The 20% service fee is charged once for the successful original placement. A replacement provided under the valid replacement guarantee creates no second 20% fee and no additional placement/service fee, because the Employer already incurred the service fee for the original placement.</p></section>
      <section><h4 className="font-bold">Legal acknowledgement</h4><p>The Employer confirms that the information provided is truthful and accurate. Knowingly providing false salary information, submitting falsified evidence, or intentionally breaching these terms may result in suspension of platform services and legal remedies as permitted by applicable law.</p></section>
    </div>
    <label className="block"><span className="field-label">Agreed first-month salary (BDT)</span><input className="form-input" type="number" inputMode="decimal" min="0.01" max="999999999999.99" step="0.01" required disabled={disabled} value={salary} onChange={e => { onSalary(e.target.value); onAccepted(false) }} /></label>
    <div aria-live="polite" className="rounded-xl bg-indigo-50 p-4 text-indigo-950"><p className="text-sm font-semibold">One-time service fee (20%)</p><p className="mt-1 text-2xl font-bold tabular-nums">{valid ? `BDT ${(Number(salary) * 0.2).toLocaleString('en-BD', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : 'Enter a valid salary'}</p><p className="mt-2 text-xs">Calculated and recorded by the platform when you confirm. This form does not collect payment.</p></div>
    <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-[var(--line)] p-4 text-sm leading-relaxed"><input className="mt-1 size-5 shrink-0 accent-indigo-600" type="checkbox" required disabled={disabled} checked={accepted} onChange={e => onAccepted(e.target.checked)} /><span>I have read and agree to the Employer Placement Fee Agreement.</span></label>
  </section>
}
