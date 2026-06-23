import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export default function ProtectedRoute() {
  const location = useLocation()
  const { isAuthenticated, isRestoring } = useAuthStore()

  if (isRestoring) {
    return <div className="route-loading">Carregando...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <Outlet />
}
