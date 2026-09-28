import type { Role } from '../auth/types'
import type { Notice } from './api'

// Only link known notices to destinations available to their recipient's role.
export function notificationDestination(notice: Notice, role: Role): string | null {
  const root = `/${role.toLowerCase()}`
  if (role === 'ADMIN' && ['Candidate verification submitted', 'Employer verification submitted'].includes(notice.title)) return '/admin/dashboard?tab=Verifications'
  if (role === 'EMPLOYER' && notice.title === 'New application received') return '/employer/candidates'
  if (role === 'CANDIDATE' && notice.title === 'Assessment available') {
    const id = /^You can now start the assessment for your application #(\d+) to /.exec(notice.message)?.[1]
    return id ? `/candidate/applications/${id}/assessment` : '/candidate/applications'
  }
  if (role === 'CANDIDATE' && notice.title === 'Application status updated') {
    const id = /^Your application #(\d+) for /.exec(notice.message)?.[1]
    return id ? `/candidate/applications/${id}` : '/candidate/applications'
  }
  if (['CANDIDATE', 'EMPLOYER'].includes(role) && notice.title === 'Verification result') return `${root}/verification`
  if (role !== 'EMPLOYER' && notice.title === 'Your next learning step') return `${root}/training`
  if (notice.title === 'Account status updated' || notice.title.startsWith('Welcome')) return `${root}/dashboard`
  if (role !== 'EVALUATOR') {
    if (/^Replacement #\d+ · /.test(notice.message)) return role === 'CANDIDATE' ? `${root}/notifications` : `${root}/replacements`
    if (['Placement active', 'Placement ended'].includes(notice.title)) return `${root}/placements`
  }
  return null
}
