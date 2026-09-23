import { api } from '../../services/api'
import type { Skill } from '../marketplace/api'
export type Section = 'education' | 'experience' | 'projects' | 'certifications' | 'languages' | 'achievements'
export type Entry = Record<string, string | boolean | null>
export type CvContent = Record<Section, Entry[]> & { summary: string | null; linkedinUrl: string | null; githubUrl: string | null }
export interface BuiltCv {
  header: { fullName: string; email: string; phone: string | null; location: string | null; profilePhotoUrl: string | null; portfolioUrl: string | null; skills: Skill[] }
  content: CvContent; updatedAt: string | null; empty: boolean
}
interface CvField { key: string; label: string; type?: 'date' | 'url' | 'checkbox' | 'textarea'; required?: boolean; max?: number }
interface CvSection { key: Section; label: string; singular: string; fields: CvField[] }
const start: CvField = { key: 'startDate', label: 'Start date', type: 'date' }
const end: CvField = { key: 'endDate', label: 'End date', type: 'date' }
const description: CvField = { key: 'description', label: 'Description / responsibilities', type: 'textarea', max: 3000 }
export const sections: CvSection[] = [
  { key: 'education', label: 'Education', singular: 'education', fields: [{ key: 'institution', label: 'Institution', required: true }, { key: 'qualification', label: 'Degree / qualification', required: true }, { key: 'fieldOfStudy', label: 'Field of study' }, start, end, { key: 'current', label: 'Currently studying', type: 'checkbox' }, { key: 'grade', label: 'Grade / CGPA', max: 80 }, description] },
  { key: 'experience', label: 'Work experience', singular: 'experience', fields: [{ key: 'organization', label: 'Organization', required: true }, { key: 'title', label: 'Role / title', required: true }, start, end, { key: 'current', label: 'Currently working', type: 'checkbox' }, description] },
  { key: 'projects', label: 'Projects', singular: 'project', fields: [{ key: 'name', label: 'Project name', required: true }, description, { key: 'technologies', label: 'Technologies', max: 500 }, { key: 'projectUrl', label: 'Project URL', type: 'url' }, { key: 'repositoryUrl', label: 'Repository URL', type: 'url' }, start, end] },
  { key: 'certifications', label: 'Certifications & training', singular: 'certification', fields: [{ key: 'name', label: 'Certification / course', required: true }, { key: 'organization', label: 'Issuing organization', required: true }, { key: 'issueDate', label: 'Issue date', type: 'date' }, { key: 'credentialUrl', label: 'Credential URL', type: 'url' }] },
  { key: 'languages', label: 'Languages', singular: 'language', fields: [{ key: 'name', label: 'Language', required: true, max: 80 }, { key: 'proficiency', label: 'Proficiency', required: true, max: 80 }] },
  { key: 'achievements', label: 'Achievements & awards', singular: 'achievement', fields: [{ key: 'title', label: 'Title', required: true }, { key: 'issuer', label: 'Issuer / organization' }, { key: 'awardDate', label: 'Award date', type: 'date' }, description] },
]
export const loadCv = () => api.get<BuiltCv>('/candidates/me/built-cv').then(r => r.data)
export function photoUrl(path: string) {
  // API returns a route, never a machine-specific storage path.
  return `${(api.defaults.baseURL ?? '/api').replace(/\/api\/?$/, '')}${path}`
}
