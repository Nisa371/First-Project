import { api } from '../../services/api'
export type Option = 'A' | 'B' | 'C' | 'D'
export type Recommendation = 'HIRE_READY' | 'NEEDS_TRAINING' | 'REJECTED'
export interface Assessment { id: number; title: string; candidateType: 'TECH' | 'TRADE'; passingScore: number | null; questions: { id: number; prompt: string; options: string[] }[] }
export interface Result { score: number; recommendation: Recommendation; feedback: string }
export interface Attempt { id: number; assessment: Assessment; status: 'IN_PROGRESS' | 'SUBMITTED' | 'EVALUATED'; answers: Record<string, Option>; startedAt: string; submittedAt: string | null; autoScore: number | null; result: Result | null }
export interface Review { attempt: Attempt; candidateName: string; autoScore: number | null; draft: Result | null; internalNotes: string | null; released: boolean; editable: boolean }
export interface Slot { id: number; startTime: string; endTime: string; capacity: number; remaining: number; active: boolean }
export interface Booking { id: number; slot: Slot; candidateName: string; purpose: string; status: string; notes: string | null }
export const assessmentApi = {
  list: () => api.get<Assessment[]>('/assessments').then(r => r.data),
  mine: () => api.get<Attempt[]>('/assessment-attempts/me').then(r => r.data),
  start: (id: number) => api.post<Attempt>(`/assessments/${id}/attempts`).then(r => r.data),
  attempt: (id: number) => api.get<Attempt>(`/assessment-attempts/${id}`).then(r => r.data),
  save: (id: number, answers: Record<string, Option>) => api.put<Attempt>(`/assessment-attempts/${id}/answers`, { answers }).then(r => r.data),
  submit: (id: number) => api.post<Attempt>(`/assessment-attempts/${id}/submit`).then(r => r.data),
  queue: () => api.get<Review[]>('/evaluator/attempts').then(r => r.data),
  review: (id: number) => api.get<Review>(`/evaluator/attempts/${id}`).then(r => r.data),
  evaluate: (id: number, body: Result & { internalNotes: string }) => api.put<Review>(`/evaluator/attempts/${id}/evaluate`, body).then(r => r.data),
  release: (id: number) => api.post<Review>(`/evaluator/attempts/${id}/release`).then(r => r.data),
  slots: () => api.get<Slot[]>('/appointment-slots').then(r => r.data),
  ownSlots: () => api.get<Slot[]>('/evaluator/appointment-slots').then(r => r.data),
  bookings: () => api.get<Booking[]>('/bookings/me').then(r => r.data),
  appointments: () => api.get<Booking[]>('/evaluator/bookings').then(r => r.data),
  book: (slotId: number, purpose: string, notes: string) => api.post<Booking>('/bookings', { slotId, purpose, notes }).then(r => r.data),
  cancel: (id: number) => api.post(`/bookings/${id}/cancel`),
  createSlot: (startTime: string, endTime: string, capacity: number) => api.post('/evaluator/appointment-slots', { startTime, endTime, capacity }),
  closeSlot: (id: number) => api.post(`/evaluator/appointment-slots/${id}/close`),
}
export const time = (value: string) => new Date(value).toLocaleString('en-BD', { dateStyle: 'medium', timeStyle: 'short' })
