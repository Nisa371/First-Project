import { api } from '../../services/api'
export interface Payment { id: number; reference: string; purpose: 'JOB_POSTING' | 'SESSION_BOOKING'; amount: number; currency: string; status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED'; description: string; jobId: number | null; bookingId: number | null }
export const payments = {
  job: (id: number) => api.post<Payment>(`/jobs/${id}/payment`).then(r => r.data),
  booking: (id: number) => api.post<Payment>(`/bookings/${id}/payment`).then(r => r.data),
  get: (id: string) => api.get<Payment>(`/payments/${id}`).then(r => r.data),
  complete: (id: number) => api.post<Payment>(`/payments/${id}/demo-success`).then(r => r.data),
  cancel: (id: number) => api.post<Payment>(`/payments/${id}/cancel`).then(r => r.data),
}
