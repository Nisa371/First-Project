import { api } from '../../services/api'
export interface Placement { id: number; candidateId: number; candidateName: string; company: string; job: string | null; skill: string; status: string; startDate: string; guaranteeEligible: boolean; guaranteeExpiresAt: string | null }
export interface Replacement { id: number; placement: Placement; reason: string; status: string; selectedCandidate: { id: number; name: string; location: string; skill: string } | null; replacementPlacementId: number | null; requestedAt: string; targetCompletionAt: string; actualCompletionAt: string | null; slaStatus: string; failureReason: string | null }
export interface QueueEntry { id: number; candidateId: number; candidateName: string; skillId: number; skill: string; status: string; joinedAt: string; reservedAt: string | null; position: number | null; exitReason: string | null }
export interface Notice { id: number; title: string; message: string; createdAt: string; readAt: string | null }
export const managed = {
  placements: () => api.get<Placement[]>('/placements/me').then(r => r.data),
  replacements: () => api.get<Replacement[]>('/replacements').then(r => r.data),
  queue: () => api.get<QueueEntry[]>('/waiting-list/me').then(r => r.data),
  adminQueue: () => api.get<QueueEntry[]>('/admin/waiting-list').then(r => r.data),
  notifications: () => api.get<Notice[]>('/notifications/me').then(r => r.data),
}
export const dateTime = (s: string) => new Date(s).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })
