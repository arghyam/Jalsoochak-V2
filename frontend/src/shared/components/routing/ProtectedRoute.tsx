import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuthStore } from '@/app/store'
import { AUTH_ROLES, type AuthRole } from '@/shared/constants/auth'
import { ROUTES } from '@/shared/constants/routes'
import { ForbiddenPage, SessionExpiredPage } from '@/shared/components/common'

interface ProtectedRouteProps {
  children: ReactNode
  requireAuth?: boolean
  allowedRoles?: AuthRole[]
}

export function ProtectedRoute({
  children,
  requireAuth = true,
  allowedRoles,
}: ProtectedRouteProps) {
  const location = useLocation()
  const { isAuthenticated, user, sessionExpired } = useAuthStore()

  if (sessionExpired) {
    return <SessionExpiredPage />
  }

  if (requireAuth && !isAuthenticated) {
    return <Navigate to={ROUTES.LOGIN} state={{ from: location }} replace />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role as AuthRole)) {
    return <ForbiddenPage />
  }

  return <>{children}</>
}

export function RedirectIfAuthenticated({ children }: { children: ReactNode }) {
  const { isAuthenticated, user } = useAuthStore()

  if (!isAuthenticated || !user) {
    return <>{children}</>
  }

  if (user.role === AUTH_ROLES.SUPER_ADMIN) {
    return <Navigate to={ROUTES.SUPER_ADMIN} replace />
  }

  if (user.role === AUTH_ROLES.STATE_ADMIN) {
    return <Navigate to={ROUTES.STATE_ADMIN} replace />
  }

  return <Navigate to={ROUTES.DASHBOARD} replace />
}
