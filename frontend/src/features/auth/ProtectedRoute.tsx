import { useEffect } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Box, CircularProgress } from '@mui/material'
import { useAuth } from './AuthContext'
import { isAtlasPublicHost, redirectToAuthentikSignIn } from './authHost'

export function ProtectedRoute() {
  const { user, loading, authReady } = useAuth()
  const location = useLocation()

  useEffect(() => {
    if (!loading && !user && isAtlasPublicHost()) {
      const returnTo = `${window.location.origin}${location.pathname}${location.search}`
      redirectToAuthentikSignIn(returnTo)
    }
  }, [loading, user, location.pathname, location.search])

  if (loading || (user && !authReady)) {
    return (
      <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    )
  }

  if (!user) {
    if (isAtlasPublicHost()) {
      return (
        <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
          <CircularProgress />
        </Box>
      )
    }
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
