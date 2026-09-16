import { lazy } from 'react'
export const AdminPage = lazy(() => import('../features/admin/AdminPage').then(m => ({ default: m.AdminPage })))
export const TrainingPage = lazy(() => import('../features/training/TrainingPage').then(m => ({ default: m.TrainingPage })))
