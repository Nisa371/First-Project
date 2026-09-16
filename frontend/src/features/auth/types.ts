export type Role = 'CANDIDATE' | 'EMPLOYER' | 'EVALUATOR' | 'ADMIN'
export type CandidateType = 'TECH' | 'TRADE'
export interface CurrentUser {
  id: number
  email: string
  role: Role
  candidateType: CandidateType | null
  displayName: string
}
export interface AuthResponse { token: string; tokenType: 'Bearer'; expiresIn: number; user: CurrentUser }
export interface RegisterRequest {
  accountType: 'CANDIDATE' | 'EMPLOYER'
  candidateType?: CandidateType
  email: string
  password: string
  fullName?: string
  companyName?: string
}
export const dashboardPath = (role: Role) => `/${role.toLowerCase()}/dashboard`
