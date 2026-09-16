import { api } from '../../services/api'
export interface Skill { id: number; name: string; category: string; proficiencyLevel?: string }
export interface Result { score: number; recommendation: string }
export interface Candidate { id: number; candidateType: 'TECH' | 'TRADE'; fullName: string; location: string; bio: string; experienceSummary: string; availability: 'AVAILABLE' | 'UNAVAILABLE'; primaryTradeCategory: string; portfolioUrl: string; skills: Skill[]; verificationStatus: string; releasedResults: Result[]; hasCv: boolean }
export interface Profile extends Candidate { phone: string; educationSummary: string; cvOriginalName: string | null }
export interface Company { companyName: string; industry: string; contactPhone: string; address: string; description: string }
export interface Job { id: number; title: string; description: string; location: string; candidateType: 'TECH' | 'TRADE'; requiredSkillId: number | null; requiredSkillName: string | null; status: 'ACTIVE' | 'CLOSED' | 'DRAFT'; shortlistCount: number; createdAt: string }
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
