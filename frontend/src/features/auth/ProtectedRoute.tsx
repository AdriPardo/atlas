import { Navigate, Outlet } from 'react-router-dom'
import { Alert, Box, Button, CircularProgress, Stack, Typography } from '@mui/material'
import { useAuth } from './AuthContext'
import { shouldShowSsoError, ssoErrorMessage } from '../../shared/api/authSession'
import { isAtlasPublicHost, redirectToAuthentikSignOut } from './authHost'

export function ProtectedRoute() {
  const { user, loading, authReady, ssoFailure, retrySso } = useAuth()

  if (loading || (user && !authReady)) {
    return (
      <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center">
        <CircularProgress />
      </Box>
    )
  }

  if (!user) {
    if (isAtlasPublicHost()) {
      if (ssoFailure && shouldShowSsoError(ssoFailure)) {
        return (
          <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center" px={2}>
            <Stack spacing={2} maxWidth={480} textAlign="center">
              <Alert severity="error">{ssoErrorMessage(ssoFailure)}</Alert>
              <Typography variant="body2" color="text.secondary">
                Código: {ssoFailure}
              </Typography>
              <Button variant="contained" onClick={() => void retrySso()}>
                Reintentar inicio de sesión
              </Button>
              <Button variant="outlined" onClick={() => redirectToAuthentikSignOut()}>
                Cerrar sesión Authentik
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
