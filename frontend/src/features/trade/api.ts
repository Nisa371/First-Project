import { api } from '../../services/api'
export type VerificationStatus = 'PENDING' | 'IN_REVIEW' | 'VERIFIED' | 'FAILED' | 'FLAGGED'
export interface Verification { id: number; status: VerificationStatus; submittedAt: string; reviewedAt: string | null }
export interface VerificationReview extends Verification { candidateName: string; candidateType: string; trade: string | null; location: string | null; identityReference: string; notes: string | null }
export interface Readiness { eligible: boolean; checks: { code: string; passed: boolean }[] }
export const tradeApi = {
  history: () => api.get<Verification[]>('/verifications/me').then(r => r.data),
  readiness: () => api.get<Readiness>('/candidates/me/readiness').then(r => r.data),
  pending: () => api.get<VerificationReview[]>('/evaluator/verifications').then(r => r.data),
  detail: (id: number) => api.get<VerificationReview>(`/evaluator/verifications/${id}`).then(r => r.data),
}
export const statusLabels: Record<VerificationStatus, string> = { PENDING: 'অপেক্ষায় · Pending', IN_REVIEW: 'যাচাই চলছে · In review', VERIFIED: 'যাচাইকৃত · Verified', FAILED: 'যাচাই হয়নি · Failed', FLAGGED: 'আরও যাচাই প্রয়োজন · Flagged' }
export function statusClass(status: VerificationStatus) { return status === 'VERIFIED' ? 'bg-emerald-50 text-emerald-800' : status === 'FAILED' ? 'bg-rose-50 text-rose-800' : 'bg-amber-50 text-amber-800' }
