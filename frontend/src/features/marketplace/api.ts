import { api } from '../../services/api'
export interface Skill { id: number; name: string; category: string; proficiencyLevel?: string }
export interface Result { score: number; recommendation: string }
export interface Candidate { profilePhotoUrl: string | null; id: number; candidateType: 'TECH' | 'TRADE'; fullName: string; location: string; bio: string; experienceSummary: string; totalExperienceMonths: number; availability: 'AVAILABLE' | 'UNAVAILABLE'; primaryTradeCategory: string; portfolioUrl: string; skills: Skill[]; verificationStatus: string; releasedResults: Result[]; hasCv: boolean }
export interface Profile extends Candidate { hasBuiltCv: boolean; phone: string; educationSummary: string; cvOriginalName: string | null }
export interface Company { companyName: string; companyTypeId: number | null; companyTypeName: string | null; companyTypeOther: boolean; companyTypeActive: boolean; customCompanyType: string | null; contactPhone: string; address: string; description: string }
export interface Job { id: number; title: string; description: string; location: string; candidateType: 'TECH' | 'TRADE'; requiredSkillId: number | null; requiredSkillName: string | null; status: 'ACTIVE' | 'CLOSED' | 'DRAFT'; shortlistCount: number; applicationCount: number; publicExpectations: string | null; privateExpectations: string | null; expectedExperienceMonths: number; createdAt: string }
export interface SearchPage { content: Candidate[]; totalElements: number; page: number; totalPages: number }
export const marketplace = {
  profile: () => api.get<Profile>('/candidates/me').then(r => r.data),
  company: () => api.get<Company>('/employers/me').then(r => r.data),
  jobs: () => api.get<Job[]>('/jobs').then(r => r.data),
  skills: () => api.get<Skill[]>('/skills').then(r => r.data),
}
export async function downloadCv(path: string) {
  const response = await api.get<Blob>(path, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data)
  const a = document.createElement('a'); a.href = url; a.download = 'resume.pdf'; a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export type ApplicationStatus = 'APPLIED' | 'UNDER_REVIEW' | 'SHORTLISTED' | 'REJECTED' | 'WITHDRAWN'
export interface PublicJob { hasApplied: boolean; companyTypeId: number | null; companyTypeName: string | null; id: number; title: string; description: string; companyName: string; location: string; candidateType: string; requiredSkillName: string | null; status: string; publicExpectations: string | null; expectedExperienceMonths: number; createdAt: string; applicationId: number | null; applicationStatus: ApplicationStatus | null }
export interface Application { assessmentStatus: string; id: number; job: PublicJob; status: ApplicationStatus; appliedAt: string; updatedAt: string }
export interface EvaluationWeights { cvWeight: number; portfolioWeight: number; experienceWeight: number; assessmentWeight: number }
export interface EvaluationAttempt { status: 'NOT_EVALUATED' | 'COMPLETED' | 'FAILED' | 'UNAVAILABLE'; failureCode: string | null; attemptedAt: string | null; evaluatedAt: string | null }
export interface Applicant { assessmentStatus: string; cvEvaluation: EvaluationAttempt; portfolioEvaluation: EvaluationAttempt; cvScore: number | null; portfolioScore: number | null; experienceScore: number; assessmentScore: number | null; finalScore: number; evaluationStatus: 'NOT_EVALUATED' | 'PARTIALLY_EVALUATED' | 'EVALUATED'; id: number; jobId: number; candidate: Candidate; status: ApplicationStatus; appliedAt: string; updatedAt: string }
export const experience = (months: number) => {
  if (months === 0) return 'No experience required'
  const years = Math.floor(months / 12), remainder = months % 12
  return [years ? `${years} ${years === 1 ? 'year' : 'years'}` : '', remainder ? `${remainder} ${remainder === 1 ? 'month' : 'months'}` : ''].filter(Boolean).join(', ')
}

export interface JobPage { content: PublicJob[]; totalElements: number; page: number; size: number; totalPages: number }
export const jobDiscovery = {
  search: (params: URLSearchParams) => api.get<JobPage>('/candidates/me/jobs', { params }).then(r => r.data),
  detail: (id: string) => api.get<PublicJob>(`/candidates/me/jobs/${id}`).then(r => r.data),
  apply: (id: number) => api.post<Application>(`/jobs/${id}/apply`).then(r => r.data),
}

export function assessmentLabel(status: string) {
  return ({ NOT_STARTED: 'Not started', IN_PROGRESS: 'In progress', COMPLETED: 'Completed', FAILED: 'Evaluation failed' } as Record<string, string>)[status] ?? status
}
