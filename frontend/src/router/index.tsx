import { ApplicationAssessmentPage, EmployerAssessmentPage } from '../features/assessment/ApplicationAssessmentPage'
import { CvBuilderPage } from '../features/cv/CvBuilderPage'
import { CvPreviewPage } from '../features/cv/CvPreviewPage'
import { DemoPaymentPage } from '../features/payment/DemoPaymentPage'
import { MyApplicationsPage } from '../features/marketplace/ApplicationsPage'
import { CandidateJobsPage, CandidateJobPage } from '../features/marketplace/ExploreJobsPage'
import { PlacementsPage, ReplacementsPage, QueuePage, AdminQueuePage, NotificationsPage } from '../features/placement/ManagedPages'
import { TradeOnboardingPage } from '../features/trade/TradeOnboardingPage'
import { VerificationPage, VerificationQueuePage, VerificationReviewPage } from '../features/trade/VerificationPages'
import { AssessmentsPage, AttemptPage } from '../features/assessment/AssessmentPages'
import { EvaluatorDashboard, EvaluationPage } from '../features/assessment/EvaluatorPages'
import { BookingPage } from '../features/assessment/BookingPage'
import { CandidateDashboard, EmployerDashboard } from '../features/marketplace/DashboardPage'
import { CandidateProfilePage } from '../features/marketplace/CandidateProfilePage'
import { EmployerProfilePage } from '../features/marketplace/EmployerProfilePage'
import { JobsPage } from '../features/marketplace/JobsPage'
import { CandidateSearchPage, ApplicantDetailPage } from '../features/marketplace/CandidateSearchPage'
import { AuthPage } from '../features/auth/AuthPage'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { AdminPage, TrainingPage } from '../components/LazyPages'
import type { Role } from '../features/auth/types'
import { createBrowserRouter } from 'react-router'
import { AppLayout } from '../layouts/AppLayout'
import { HomePage } from '../pages/HomePage'
import { NotFoundPage } from '../pages/NotFoundPage'

export const router = createBrowserRouter([
  { element: <AppLayout />, children: [
    { path: '/', element: <HomePage /> },
    { path: '/login', element: <AuthPage key="login" mode="login" /> },
    { path: '/register', element: <AuthPage key="register" mode="register" /> },
    ...(['CANDIDATE', 'EMPLOYER', 'EVALUATOR', 'ADMIN'] as Role[]).map(role => ({
      element: <ProtectedRoute role={role} />,
      children: [{ path: `/${role.toLowerCase()}/dashboard`, element: role === 'CANDIDATE' ? <CandidateDashboard /> : role === 'EMPLOYER' ? <EmployerDashboard /> : role === 'EVALUATOR' ? <EvaluatorDashboard /> : <AdminPage /> }],
    })),
    { element: <ProtectedRoute role="CANDIDATE" />, children: [{ path: '/candidate/applications/:id/assessment', element: <ApplicationAssessmentPage /> }, { path: '/candidate/cv', element: <CvBuilderPage /> }, { path: '/candidate/cv/preview', element: <CvPreviewPage /> }, { path: '/candidate/payments/:id', element: <DemoPaymentPage /> }, { path: '/candidate/jobs', element: <CandidateJobsPage /> }, { path: '/candidate/jobs/:id', element: <CandidateJobPage /> }, { path: '/candidate/applications', element: <MyApplicationsPage /> }, { path: '/candidate/onboarding', element: <TradeOnboardingPage /> }, { path: '/candidate/verification', element: <VerificationPage /> }, { path: '/candidate/profile', element: <CandidateProfilePage /> }, { path: '/candidate/assessments', element: <AssessmentsPage /> }, { path: '/candidate/assessments/:id', element: <AttemptPage /> }, { path: '/candidate/bookings', element: <BookingPage /> }] },
    { element: <ProtectedRoute role="EMPLOYER" />, children: [
      { path: '/employer/jobs/:jobId/applications/:id/cv/:candidateId', element: <CvPreviewPage /> },
      { path: '/employer/jobs/:jobId/applications/:id/assessment', element: <EmployerAssessmentPage /> },
      { path: '/employer/verification', element: <VerificationPage /> },
      { path: '/employer/profile', element: <EmployerProfilePage /> },
      { path: '/employer/payments/:id', element: <DemoPaymentPage /> },
      { path: '/employer/jobs', element: <JobsPage /> },
      { path: '/employer/jobs/:jobId/applications', element: <CandidateSearchPage /> },
      { path: '/employer/jobs/:jobId/applications/:id', element: <ApplicantDetailPage /> },
      { path: '/employer/candidates', element: <CandidateSearchPage /> },
    ] },
    { element: <ProtectedRoute role="EVALUATOR" />, children: [{ path: '/evaluator/verifications', element: <VerificationQueuePage /> }, { path: '/evaluator/verifications/:id', element: <VerificationReviewPage /> }, { path: '/evaluator/attempts/:id', element: <EvaluationPage /> }, { path: '/evaluator/bookings', element: <BookingPage /> }] },
    ...(['CANDIDATE', 'EMPLOYER', 'ADMIN', 'EVALUATOR'] as Role[]).map(role => ({ element: <ProtectedRoute role={role} />, children: [
      { path: `/${role.toLowerCase()}/notifications`, element: <NotificationsPage /> },
      ...(role !== 'EMPLOYER' ? [{ path: `/${role.toLowerCase()}/training`, element: <TrainingPage /> }] : []),
      ...(role !== 'EVALUATOR' ? [{ path: `/${role.toLowerCase()}/placements`, element: <PlacementsPage /> }, { path: `/${role.toLowerCase()}/replacements`, element: <ReplacementsPage /> }] : []),
      ...(role === 'CANDIDATE' ? [{ path: '/candidate/queue', element: <QueuePage /> }] : []),
      ...(role === 'ADMIN' ? [{ path: '/admin/queue', element: <AdminQueuePage /> }] : []),
    ] })),
    { path: '*', element: <NotFoundPage /> },
  ] },
])
