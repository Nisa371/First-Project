import { useTradeText } from '../trade/useTradeText'
import { useCallback } from 'react'
import { Link, useParams } from 'react-router'
import { api } from '../../services/api'
import { Workspace, LoadState, Empty } from '../marketplace/shared'
import { useLoad } from '../marketplace/useLoad'
import { sections, type BuiltCv } from './model'
import { Avatar } from './ProfilePhoto'
import './cv.css'

export function CvPreviewPage() {
  const tr = useTradeText()

  const { candidateId, jobId, id } = useParams()
  const loader = useCallback(() => api.get<BuiltCv>(candidateId ? `/candidates/${candidateId}/built-cv` : '/candidates/me/built-cv').then(r => r.data), [candidateId])
  const state = useLoad(loader), cv = state.data
  return <div className="cv-preview-page"><Workspace title={tr("CV preview")} subtitle={tr("A professional view of the saved CV. Use your browser’s print dialog to save as PDF.")}><div className="cv-controls mb-6 flex flex-wrap gap-3"><Link className="button-secondary" to={candidateId ? `/employer/jobs/${jobId}/applications/${id}` : '/candidate/cv'}>{candidateId ? '← Back to applicant' : tr("← Edit CV")}</Link>{cv && !cv.empty && <button className="button-primary" onClick={() => window.print()}>{tr("Print / Save as PDF")}</button>}</div><LoadState {...state} />{cv && (cv.empty ? <Empty title={tr("No built CV yet")}>{candidateId ? 'This applicant has not created a structured CV yet. Check their profile for an uploaded PDF.' : tr("Add your summary, education or experience in the CV Builder, then save.")}</Empty> : <CvDocument cv={cv} />)}</Workspace></div>
}
function CvDocument({ cv }: { cv: BuiltCv }) {
  const tr = useTradeText()

  const h = cv.header, c = cv.content
  const links = [[tr("Portfolio"), h.portfolioUrl], [tr("LinkedIn"), c.linkedinUrl], [tr("GitHub"), c.githubUrl]].filter(([, url]) => url)
  return <article className="cv-document mx-auto max-w-[210mm] rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-12">
    <header className="flex flex-wrap items-start justify-between gap-6 border-b-2 border-slate-800 pb-6"><div className="min-w-0 flex-1"><h2 className="break-words text-3xl font-bold tracking-tight text-slate-900">{h.fullName}</h2><p className="mt-3 break-words text-sm text-slate-600">{[h.email, h.phone, h.location].filter(Boolean).join(' · ')}</p><div className="mt-3 flex flex-wrap gap-4 text-sm">{links.map(([label, url]) => <a className="break-all text-indigo-700 underline" key={label} href={url!} target="_blank" rel="noreferrer">{label}</a>)}</div></div>{h.profilePhotoUrl && <Avatar name={h.fullName} url={h.profilePhotoUrl} />}</header>
    {c.summary && <section className="mt-7"><h3 className="cv-heading">{tr("Professional summary")}</h3><p className="whitespace-pre-wrap break-words text-sm leading-7">{c.summary}</p></section>}
    {h.skills.length > 0 && <section className="mt-7"><h3 className="cv-heading">{tr("Skills")}</h3><p className="text-sm leading-7">{h.skills.map(s => `${s.name}${s.proficiencyLevel ? ` (${tr(s.proficiencyLevel)})` : ''}`).join(' · ')}</p></section>}
    {sections.filter(s => c[s.key].length > 0).map(s => <section className="mt-7" key={s.key}><h3 className="cv-heading">{tr(s.label)}</h3><div className="space-y-5">{c[s.key].map((entry, index) => {
      const title = entry.qualification || entry.title || entry.name
      const subtitle = entry.institution || entry.organization || entry.issuer
      const dates = [entry.startDate, entry.current ? tr("Present") : entry.endDate].filter(Boolean).join(' – ') || entry.issueDate || entry.awardDate
      return <div key={index} className="cv-entry text-sm leading-6"><div className="flex flex-wrap items-baseline justify-between gap-x-4"><h4 className="font-bold">{title}</h4>{dates && <p className="text-xs text-slate-500">{dates}</p>}</div>{subtitle && <p className="font-medium text-slate-700">{subtitle}</p>}{entry.fieldOfStudy && <p>{entry.fieldOfStudy}</p>}{entry.grade && <p>{tr("Grade / CGPA:")}{' '}{entry.grade}</p>}{entry.proficiency && <p>{entry.proficiency}</p>}{entry.description && <p className="mt-1 whitespace-pre-wrap break-words text-slate-600">{entry.description}</p>}{entry.technologies && <p className="mt-1">{tr("Technologies:")}{' '}{entry.technologies}</p>}<div className="flex flex-wrap gap-4">{s.fields.filter(f => f.type === 'url' && entry[f.key]).map(f => <a key={f.key} href={String(entry[f.key])} target="_blank" rel="noreferrer" className="break-all text-indigo-700 underline">{tr(f.label)}</a>)}</div></div>
    })}</div></section>)}
  </article>
}
