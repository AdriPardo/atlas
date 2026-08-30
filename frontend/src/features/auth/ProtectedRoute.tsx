import { Navigate, Outlet } from 'react-router-dom'
import { Box, CircularProgress } from '@mui/material'
import { useAuth } from './AuthContext'
import { isAtlasPublicHost } from './authHost'

export function ProtectedRoute() {
  const { user, loading } = useAuth()

  if (loading || !user) {
    if (isAtlasPublicHost()) {
      return (
        <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
          <CircularProgress />
        </Box>
      )
    }
    if (!loading && !user) {
      return <Navigate to="/login" replace />
    }
    return (
      <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    )
  }

  return <Outlet />
}
