import type { Profile } from './api'

// Use the same recommended profile details as the application confirmation.
export function hasBasicProfile(profile: Profile) {
  return [profile.fullName, profile.phone, profile.location, profile.bio,
    profile.experienceSummary, profile.profilePhotoUrl,
    ...(profile.candidateType === 'TECH' ? [profile.educationSummary] : []),
  ].every(value => Boolean(value?.trim()))
}

export function portfolioState(value: string | null | undefined) {
  if (!value?.trim()) return 'missing'
  try {
    const url = new URL(value)
    if (/^https?:$/.test(url.protocol) && url.hostname && !url.username && !url.password && !/\s/.test(value)) return 'added'
  } catch { /* Show an actionable field error. */ }
  return 'invalid'
}

export function candidateReadiness(p: Profile) {
  const cv = p.hasBuiltCv || p.cvAiReady ? 'ready' : p.cvOriginalName ? 'unreadable' : 'missing'
  const items = [
    { id: 'basic', label: 'Basic profile', done: hasBasicProfile(p), required: false },
    { id: 'skills', label: 'Skills', done: p.skills.length > 0, required: false },
    { id: 'cv', label: 'CV', done: cv !== 'missing', required: false },
    ...(p.candidateType === 'TECH' ? [{ id: 'portfolio', label: 'Portfolio', done: portfolioState(p.portfolioUrl) === 'added', required: false }] : []),
    { id: 'verification', label: 'Verification', done: p.verificationStatus === 'VERIFIED', required: true },
    { id: 'availability', label: 'Availability', done: p.availability === 'AVAILABLE', required: true },
  ]
  const cvMessage = cv === 'ready' ? 'CV ready for AI evaluation' : cv === 'unreadable'
    ? 'CV uploaded, but its text could not be extracted. This PDF cannot be used for AI evaluation. Please upload a text-based PDF or complete your CV using the CV Builder.' : 'No CV added'
  return { items, cv, cvMessage, percent: Math.round(items.filter(item => item.done).length / items.length * 100),
    warnings: items.filter(item => !item.required && !item.done).map(item => item.label).concat(cv === 'unreadable' ? ['CV is not ready for AI evaluation'] : []) }
}
