import { Navigate, Outlet } from 'react-router-dom'
import { Alert, Box, Button, CircularProgress, Stack } from '@mui/material'
import { useAuth } from './AuthContext'
import { isAtlasPublicHost } from './authHost'

export function ProtectedRoute() {
  const { user, loading, authReady, ssoFailed, retrySso } = useAuth()

  if (loading || (user && !authReady)) {
    return (
      <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    )
  }

  if (!user) {
    if (isAtlasPublicHost()) {
      if (ssoFailed) {
        return (
          <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center" px={2}>
            <Stack spacing={2} maxWidth={420} textAlign="center">
              <Alert severity="warning">
                No se pudo completar el inicio de sesión SSO. Comprueba que tu usuario tiene acceso
                a Atlas en Authentik.
              </Alert>
              <Button variant="contained" onClick={() => void retrySso()}>
                Reintentar inicio de sesión
              </Button>
            </Stack>
          </Box>
        )
      }
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
