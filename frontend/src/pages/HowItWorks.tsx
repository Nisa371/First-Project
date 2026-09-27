import { Link } from 'react-router'

const candidateSteps = [
  ['Create account', 'Choose TECH or Bangla-assisted TRADE onboarding.'],
  ['Complete profile', 'Introduce yourself, your experience and your location.'],
  ['Add skills', 'Show the work you can do.'],
  ['Upload CV or use CV Builder', 'Bring your experience together in one place.'],
  ['Add portfolio', 'Share work samples where available; this is recommended, not required.'],
  ['Complete verification', 'Platform approval is required before applying.'],
  ['Mark yourself available', 'Available status is also required before applying.'],
  ['Browse and apply for jobs', 'AI evaluation runs automatically after application.'],
  ['Complete job-specific assessment', 'When required, open the assessment attached to your application.'],
  ['Track application status', 'Follow employer updates in My applications.'],
]
const employerSteps = [
  ['Create employer account', 'Start your hiring workspace.'],
  ['Complete company profile', 'Add company type, contact details and an introduction.'],
  ['Complete verification', 'Employers must be verified before posting jobs.'],
  ['Post a job', 'Create a draft and complete payment to publish.'],
  ['Receive applications', 'Candidates apply directly to your opening.'],
  ['AI evaluation happens automatically', 'Evaluation runs after application; available results appear with applicants.'],
  ['Review ranked applicants', 'Compare available scores and job-specific assessments.'],
  ['Update status / select candidates', 'Review, shortlist and select suitable applicants.'],
]

export function HowItWorks() {
  return <section aria-labelledby="how-it-works-title">
    <p className="eyebrow">YOUR JOURNEY, STEP BY STEP</p><h2 id="how-it-works-title" className="mt-3 text-3xl font-bold tracking-tight">How it works</h2>
    <p className="mt-3 max-w-3xl leading-relaxed text-slate-600">Start with your profile. Your dashboard shows what is complete and where to go next. Verification is a manual platform review.</p>
    <div className="mt-7 grid items-start gap-5 lg:grid-cols-2">{[
      { title: 'For candidates', steps: candidateSteps, to: '/register', cta: 'Create candidate account', note: 'Profile details, skills, CV and portfolio improve your application. Verification and Available status are mandatory before applying.' },
      { title: 'For employers', steps: employerSteps, to: '/register?account=employer', cta: 'Create employer account', note: 'Verification approval unlocks job posting. AI evaluation starts automatically after an application; assessments belong to a specific job application.' },
    ].map(flow => <article className="surface" key={flow.title}>
      <h3 className="text-xl font-bold">{flow.title}</h3><ol className="mt-5 space-y-4">{flow.steps.map(([title, description], index) => <li key={title} className="flex gap-3">
        <span aria-hidden="true" className="grid size-8 shrink-0 place-items-center rounded-full bg-indigo-50 text-sm font-bold text-indigo-700">{index + 1}</span>
        <div><h4 className="text-sm font-semibold">{title}</h4><p className="mt-1 text-sm leading-relaxed text-slate-500">{description}</p></div>
      </li>)}</ol><p className="mt-5 rounded-xl bg-slate-50 p-4 text-xs leading-relaxed text-slate-600">{flow.note}</p><Link className="button-secondary mt-5" to={flow.to}>{flow.cta} →</Link>
    </article>)}</div>
  </section>
}
