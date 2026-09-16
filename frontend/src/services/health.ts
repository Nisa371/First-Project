import { api } from './api'
import type { HealthResponse } from '../types/health'

export async function getHealth(signal?: AbortSignal): Promise<HealthResponse> {
  const { data } = await api.get<HealthResponse>('/health', { signal })
  if (data?.status !== 'UP') throw new Error('Unexpected health response')
  return data
}
