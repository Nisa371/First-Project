import { Link } from 'react-router'
import type { Applicant, Company, Job, Profile } from './api'
import type { Checklist } from '../verification/api'
import { candidateReadiness } from './profileReadiness'

interface Action { title: string; why: string; to: string; cta: string }
interface Step extends Action { done: boolean; required?: boolean; hint?: string }

function WorkflowGuide({ steps, next, note, trade = false, readinessPercent }: { steps: Step[]; next: Action; note: string; trade?: boolean; readinessPercent?: number }) {
  const complete = steps.filter(step => step.done).length
  return <section className="surface mb-6" lang={trade ? 'bn' : 'en'} aria-label={trade ? 'আপনার অগ্রগতি' : 'Your progress'}>
    <div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-xl font-bold">{readinessPercent !== undefined ? (trade ? 'প্রোফাইল প্রস্তুতি' : 'Profile Readiness') : (trade ? 'শুরু করুন · আপনার অগ্রগতি' : 'Getting started · Your progress')}</h2><span className="text-sm font-semibold text-slate-600">{readinessPercent !== undefined ? `${readinessPercent}% · ` : ''}{trade ? `${steps.length.toLocaleString('bn-BD')}টির মধ্যে ${complete.toLocaleString('bn-BD')}টি সম্পূর্ণ` : `${complete} of ${steps.length} completed`}</span></div>
    <progress className="my-4 h-2 w-full accent-indigo-600" max={steps.length} value={complete} aria-label={trade ? 'সম্পূর্ণ ধাপ' : 'Completed steps'} />
    <div className="grid gap-5 lg:grid-cols-[1.2fr_1fr]">
      <ol className="grid content-start gap-2 sm:grid-cols-2">{steps.map(step => <li key={step.title} className="flex items-start gap-2 rounded-xl bg-slate-50 p-3">
        <span className={step.done ? 'text-emerald-700' : 'text-slate-500'} aria-label={step.done ? (trade ? 'সম্পূর্ণ' : 'Complete') : (trade ? 'বাকি' : 'Incomplete')}>{step.done ? '✓' : '○'}</span>
        <div><Link className="text-sm font-semibold hover:underline" to={step.to}>{step.title}</Link><p className="mt-1 text-xs text-slate-500">{step.hint ?? (step.required ? (trade ? 'আবেদনের আগে আবশ্যক' : 'Required before applying') : (trade ? 'প্রস্তুতির জন্য পরামর্শ' : 'Recommended step'))}</p></div>
      </li>)}</ol>
      <div className="rounded-xl bg-indigo-50 p-5"><p className="eyebrow text-indigo-700">{trade ? 'পরের ধাপ' : 'Next step'}</p><h3 className="mt-2 text-lg font-bold">{next.title}</h3><p className="mt-2 text-sm leading-relaxed text-slate-600">{next.why}</p><Link className="button-primary mt-4" to={next.to}>{next.cta} →</Link></div>
    </div><p className="mt-4 text-xs leading-relaxed text-slate-500">{note}</p>
  </section>
}

export function CandidateWorkflow({ profile: p }: { profile: Profile }) {
  const trade = p.candidateType === 'TRADE'
  const readiness = candidateReadiness(p)
  const done = (id: string) => readiness.items.find(item => item.id === id)?.done ?? false
  const text = (en: string, bn: string) => trade ? bn : en
  const steps: Step[] = [
    { title: text('Complete basic profile', 'প্রোফাইল সম্পূর্ণ করুন'), done: done('basic'), to: '/candidate/profile', cta: text('Edit profile', 'প্রোফাইল লিখুন'), why: text('Add your contact details, location, introduction, experience, photo and education or trade so employers can understand your background.', 'যোগাযোগ, এলাকা, পরিচিতি, অভিজ্ঞতা, ছবি ও পেশা যোগ করুন। নিয়োগদাতা আপনার সম্পর্কে জানতে পারবেন।') },
    { title: text('Add skills', 'দক্ষতা যোগ করুন'), done: done('skills'), to: '/candidate/profile', cta: text('Add skills', 'দক্ষতা যোগ করুন'), why: text('Show employers the work you can do and your level of experience.', 'আপনি কোন কাজ পারেন তা নিয়োগদাতাকে জানান।') },
    { title: text('Upload or build a CV', 'সিভি যোগ করুন'), done: done('cv'), hint: readiness.cvMessage, to: '/candidate/cv', cta: text('Open CV Builder', 'সিভি তৈরি করুন'), why: text('Build a CV here, or upload a PDF from My profile. Either counts toward this step.', 'আপনার পড়াশোনা ও কাজের অভিজ্ঞতা দিয়ে সিভি তৈরি করুন।') },
    // The existing TRADE profile has no portfolio editor; do not suggest an unavailable action.
    ...(!trade ? [{ title: 'Add portfolio', done: done('portfolio'), to: '/candidate/profile', cta: 'Add portfolio link', why: 'Share examples of your work. A portfolio is recommended, but is not required to apply.' }] : []),
    { title: text('Complete verification', 'যাচাইকরণ সম্পূর্ণ করুন'), done: done('verification'), required: true, to: '/candidate/verification', cta: text('Go to verification', 'যাচাইকরণ দেখুন'), why: p.verificationStatus === 'IN_REVIEW' ? text('Your documents are in review. Approval is required before applying; check your checklist for any remaining documents.', 'কাগজপত্র যাচাই চলছে। আবেদনের আগে অনুমোদন প্রয়োজন। কোনো কাগজ বাকি আছে কি না দেখুন।') : text('Complete manual platform verification before applying for jobs.', 'চাকরিতে আবেদনের আগে প্ল্যাটফর্মের যাচাইকরণ সম্পূর্ণ করুন।') },
    { title: text('Mark yourself available', 'কাজের জন্য উপলভ্য করুন'), done: done('availability'), required: true, to: '/candidate/profile', cta: text('Update availability', 'কাজের প্রস্তুতি জানান'), why: text('Set availability to Available and save your profile when you are ready to accept work.', 'কাজ করতে প্রস্তুত হলে প্রোফাইলে উপলভ্য নির্বাচন করে সংরক্ষণ করুন।') },
  ]
  const next = steps.find(step => !step.done) ?? { title: text("You're ready to apply for jobs", 'আপনি চাকরিতে আবেদন করতে প্রস্তুত'), to: '/candidate/jobs', cta: text('Browse jobs', 'চাকরি খুঁজুন'), why: text('Find a role, apply, then track your application. Complete the job-specific assessment when required.', 'চাকরি খুঁজে আবেদন করুন। আবেদনের অবস্থা দেখুন এবং প্রয়োজন হলে সেই চাকরির মূল্যায়ন সম্পূর্ণ করুন।') }
  return <><WorkflowGuide steps={steps} next={next} readinessPercent={readiness.percent} trade={trade} note={text('Verification and Available status are mandatory before applying. Profile, skills, CV and portfolio are recommendations, not application blockers.', 'আবেদনের আগে যাচাইকরণ ও কাজের জন্য উপলভ্য থাকা আবশ্যক। প্রোফাইল, দক্ষতা ও সিভি সম্পূর্ণ করার পরামর্শ দেওয়া হয়; এগুলো অসম্পূর্ণ থাকলেও আবেদন করা যায়।')} />
    <div className="mb-6 flex flex-wrap gap-4 text-sm font-semibold text-indigo-700"><Link to="/candidate/jobs">{text('Browse jobs', 'চাকরি খুঁজুন')} →</Link><Link to="/candidate/applications">{text('Track applications & job assessments', 'আবেদন ও চাকরির মূল্যায়ন দেখুন')} →</Link></div></>
}

export function EmployerWorkflow({ company: c, jobs, checklist, applicants }: { company: Company; jobs: Job[]; checklist: Checklist; applicants: Applicant[] }) {
  const profileDone = [c.companyName, c.contactPhone, c.address, c.description].every(value => value?.trim()) && Boolean(c.companyTypeId) && (!c.companyTypeOther || Boolean(c.customCompanyType?.trim()))
  const verified = checklist.status === 'VERIFIED'
  const required = checklist.items.filter(item => item.requirement.required)
  const pending = !verified && !checklist.companyTypeRequired && required.length > 0 && required.every(item => item.submission && ['VERIFIED', 'PENDING', 'IN_REVIEW'].includes(item.submission.status))
  const received = jobs.some(job => job.applicationCount > 0)
  const reviewable = applicants.find(applicant => applicant.status !== 'WITHDRAWN')
  const posted = jobs.some(job => job.status !== 'DRAFT')
  const active = jobs.some(job => job.status === 'ACTIVE')
  const draft = jobs.some(job => job.status === 'DRAFT')
  const profile: Action = { title: 'Complete company profile', to: '/employer/profile', cta: 'Edit company profile', why: 'Add your company type, contact details, address and introduction so candidates understand who is hiring.' }
  const verify: Action = { title: pending ? 'Verification pending' : 'Complete verification', to: '/employer/verification', cta: pending ? 'View verification status' : 'Go to verification', why: pending ? 'Your required documents are awaiting approval. Job posting becomes available after approval.' : 'Complete platform verification before posting jobs. Submit any missing documents or review requested corrections.' }
  const post: Action = { title: draft ? 'Publish your job draft' : 'Post your first job', to: '/employer/jobs', cta: draft ? 'Continue in Jobs' : 'Create a job', why: draft ? 'Your draft is not live yet. Open Jobs and complete the existing payment step to publish it.' : 'Create a clear opening, then complete payment to make it visible to candidates.' }
  const review: Action = { title: 'Review applicants', to: reviewable ? `/employer/jobs/${reviewable.jobId}/applications` : '/employer/candidates', cta: 'Review ranked applicants', why: 'AI evaluation runs automatically after application. Review available scores and job-specific assessments, then update application status or select candidates. Missing evaluations do not prevent review.' }
  const next = !profileDone ? profile : !verified ? verify : reviewable ? review : !active && draft ? post : !posted ? post : active ? { title: 'Your job is live', to: '/employer/jobs', cta: 'Manage openings', why: received ? 'No active applicants remain to review. New applications will appear when candidates apply.' : 'Applications will appear here when candidates apply.' } : { title: 'Open your next role', to: '/employer/jobs', cta: 'Manage openings', why: 'Your previous openings are closed. Create a new opening when you are ready to hire again.' }
  const steps: Step[] = [
    { ...profile, done: profileDone }, { ...verify, title: 'Verification completed', done: verified, hint: 'Required before posting' },
    { ...post, title: 'First job posted', done: posted, hint: 'Published opening' },
    { ...review, title: 'Applications received', done: received, hint: 'Updates when candidates apply' },
    { ...review, title: 'Applicants ready for review', done: Boolean(reviewable), hint: 'Applications not withdrawn' },
  ]
  return <WorkflowGuide steps={steps} next={next} note="Verification is required to publish jobs. Review readiness means an application has not been withdrawn; it does not guarantee completed AI evaluation or assessment." />
}
