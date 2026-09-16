import { Link } from 'react-router'

export function NotFoundPage() {
  return <section className="mx-auto max-w-xl rounded-2xl border border-slate-200 bg-white p-8 text-center sm:my-12 sm:p-12"><p className="eyebrow">404 / PAGE NOT FOUND</p><h1 className="mt-4 text-3xl font-semibold tracking-tight">Let’s get you back on track.</h1><p className="mt-4 text-slate-600">This address doesn’t lead to a page. Return home to check the marketplace connection.</p><Link to="/" className="button-primary mt-8">Back to home <span aria-hidden="true">→</span></Link></section>
}
