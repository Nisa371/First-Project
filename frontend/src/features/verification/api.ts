import { api } from '../../services/api'
import type { VerificationStatus } from '../trade/api'
export type Target = 'CANDIDATE' | 'EMPLOYER' | 'COMPANY_EMPLOYER' | 'HOUSEHOLD_EMPLOYER'
export interface Requirement { id: number; name: string; description: string | null; targetType: Target; companyTypeId: number | null; companyTypeName: string | null; required: boolean; active: boolean; baseline: boolean }
export interface Document { id: number; status: VerificationStatus; originalName: string | null; requirementName: string; fileSize: number | null; reviewNote: string | null; submittedAt: string; reviewedAt: string | null; hasFile: boolean; legacy: boolean }
export interface Checklist { status: string; companyTypeRequired: boolean; items: { requirement: Requirement; submission: Document | null }[]; history: Document[] }
export interface Review { id: number; ownerName: string; role: string; companyTypeId: number | null; companyTypeName: string | null; requirementName: string; status: VerificationStatus; reviewNote: string | null; identityReference: string | null; submittedAt: string; reviewedAt: string | null; reviewerId: number | null; hasFile: boolean; latest: boolean; applicable: boolean }
export const verification = {
  checklist: () => api.get<Checklist>('/verifications/me/checklist').then(r => r.data),
  requirements: () => api.get<Requirement[]>('/admin/verification-requirements').then(r => r.data),
  reviews: () => api.get<Review[]>('/admin/verification-submissions').then(r => r.data),
}
export const documentLabel = (status?: VerificationStatus) => status === 'VERIFIED' ? 'Approved' : status === 'FAILED' || status === 'FLAGGED' ? 'Rejected' : status === 'IN_REVIEW' ? 'In review' : status === 'PENDING' ? 'Pending' : 'Not submitted'
export async function downloadDocument(id: number) {
  const { data } = await api.get<Blob>(`/verifications/documents/${id}`, { responseType: 'blob' })
  const url = URL.createObjectURL(data), a = document.createElement('a')
  a.href = url; a.download = `verification-${id}.${data.type === 'image/png' ? 'png' : data.type === 'image/jpeg' ? 'jpg' : 'pdf'}`; a.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
