import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'
import { dashboardPath, type Role } from './types'

export function ProtectedRoute({ role }: { role: Role }) {
  const auth = useAuth()
  const location = useLocation()
  if (auth.loading) return <div role="status" className="surface mx-auto max-w-lg text-center">Checking your session…</div>
  if (auth.error) return <div className="surface mx-auto max-w-lg"><h1 className="text-2xl font-bold">Let’s reconnect</h1><p role="alert" className="my-5 text-slate-600">{auth.error}</p><button className="button-primary" onClick={auth.retry}>Try again</button><button className="ml-4 min-h-11 underline" onClick={auth.logout}>Sign out</button></div>
  if (!auth.user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (auth.user.role !== role) return <Navigate to={dashboardPath(auth.user.role)} replace />
  return <Outlet />
}
