import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Navigate, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  IconButton,
  Link,
  Stack,
  TextField,
  Typography,
  useTheme,
} from '@mui/material'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import { useAuth } from './AuthContext'
import { redirectToSsoBootstrap } from '../../shared/api/authSession'
import {
  PUBLIC_ATLAS_URL,
  allowLocalLogin,
  isAtlasPublicHost,
  isDirectAccessHost,
  redirectToAuthentikSignIn,
} from './authHost'

const schema = z.object({
  username: z.string().min(1, 'Required'),
  password: z.string().min(1, 'Required'),
})

type FormValues = z.infer<typeof schema>

interface LoginPageProps {
  mode?: 'light' | 'dark'
  onToggleMode?: () => void
}

export function LoginPage({ mode, onToggleMode }: LoginPageProps) {
  const theme = useTheme()
  const isDark = theme.palette.mode === 'dark'
  const { login, user, loading } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const [ssoBusy, setSsoBusy] = useState(false)
  const publicHost = isAtlasPublicHost()
  const directAccess = isDirectAccessHost()
  const localAllowed = allowLocalLogin()

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (publicHost && !loading && !user) {
      redirectToSsoBootstrap('/')
    }
  }, [publicHost, loading, user])

  if (!loading && user) {
    return <Navigate to="/" replace />
  }

  if (publicHost && !user) {
    return (
      <Box minHeight="100dvh" display="flex" alignItems="center" justifyContent="center">
        <Typography color="text.secondary">Redirecting to Authentik…</Typography>
      </Box>
    )
  }

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      await login(values.username, values.password)
      navigate('/')
    } catch {
      setError('Invalid username or password')
    }
  })

  const continueWithAuthentik = () => {
    setSsoBusy(true)
    setError(null)
    if (publicHost) {
      redirectToSsoBootstrap('/')
      return
    }
    redirectToAuthentikSignIn(PUBLIC_ATLAS_URL)
  }

  return (
    <Box
      className="atlas-page"
      minHeight="100dvh"
      display="grid"
      gridTemplateColumns={{ xs: '1fr', md: '1.05fr 0.95fr' }}
      position="relative"
      overflow="hidden"
      sx={{
        background: isDark
          ? 'linear-gradient(145deg, #0A101A 0%, #0B1220 45%, #0E1A24 100%)'
          : 'linear-gradient(145deg, #E8EEEC 0%, #F1F5F4 50%, #E6F0EE 100%)',
      }}
    >
      <Box className="atlas-grain" />

      {onToggleMode && (
        <IconButton
          onClick={onToggleMode}
          aria-label="Toggle theme"
          size="small"
          sx={{
            position: 'absolute',
            top: { xs: 12, sm: 16 },
            right: { xs: 12, sm: 16 },
            zIndex: 2,
            bgcolor: isDark ? 'rgba(15,23,42,0.55)' : 'rgba(255,255,255,0.72)',
            border: (t) => `1px solid ${t.palette.divider}`,
            '&:hover': {
              bgcolor: isDark ? 'rgba(15,23,42,0.8)' : 'rgba(255,255,255,0.92)',
            },
          }}
        >
          {(mode ?? theme.palette.mode) === 'dark' ? (
            <LightModeOutlinedIcon fontSize="small" />
          ) : (
            <DarkModeOutlinedIcon fontSize="small" />
          )}
        </IconButton>
      )}

      <Box
        sx={{
          position: 'relative',
          zIndex: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
          p: { xs: 3, sm: 4, md: 6 },
          minHeight: { xs: 'auto', md: '100dvh' },
          borderRight: { md: (t) => `1px solid ${t.palette.divider}` },
          background: isDark
            ? `radial-gradient(700px 420px at 20% 10%, rgba(45,212,191,0.16), transparent 60%),
               radial-gradient(500px 300px at 80% 90%, rgba(56,189,248,0.06), transparent 55%)`
            : `radial-gradient(700px 420px at 20% 10%, rgba(15,118,110,0.14), transparent 60%),
               radial-gradient(500px 300px at 80% 90%, rgba(14,165,233,0.06), transparent 55%)`,
        }}
      >
        <Typography
          variant="h5"
          sx={{
            fontWeight: 700,
            letterSpacing: '-0.03em',
            color: 'primary.main',
          }}
        >
          Atlas
        </Typography>

        <Box sx={{ py: { xs: 6, md: 0 }, maxWidth: 460 }}>
          <Typography
            component="h1"
            sx={{
              fontSize: { xs: '2.4rem', sm: '3rem', md: '3.35rem' },
              fontWeight: 700,
              letterSpacing: '-0.04em',
              lineHeight: 1.05,
              mb: 2,
            }}
          >
            Operations for self-hosted apps
          </Typography>
          <Typography color="text.secondary" sx={{ fontSize: '1.05rem', maxWidth: 38 * 8 }}>
            Register applications, hosts and deployments from one quiet console.
          </Typography>
        </Box>

        <Typography variant="caption" color="text.secondary" sx={{ display: { xs: 'none', md: 'block' } }}>
          Atlas console
        </Typography>
      </Box>

      <Box
        sx={{
          position: 'relative',
          zIndex: 1,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          p: { xs: 3, sm: 4, md: 6 },
          minHeight: { xs: 'auto', md: '100dvh' },
        }}
      >
        <Box sx={{ width: '100%', maxWidth: 400 }}>
          <Stack spacing={3}>
            {publicHost ? (
              <>
                <Box>
                  <Typography variant="h5" component="h2" gutterBottom>
                    Authentik SSO
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    Production uses Authentik via Traefik ForwardAuth. Local username/password is
                    disabled on this host.
                  </Typography>
                </Box>

                {error && (
                  <Alert severity="error" variant="outlined">
                    {error}
                  </Alert>
                )}

                <Button
                  variant="contained"
                  size="large"
                  fullWidth
                  disabled={loading || ssoBusy}
                  onClick={() => void continueWithAuthentik()}
                >
                  {ssoBusy || loading ? 'Connecting…' : 'Complete Authentik login'}
                </Button>

                <Typography variant="caption" color="text.secondary">
                  Opens Atlas through ForwardAuth (
                  <Link href={PUBLIC_ATLAS_URL} underline="hover">
                    atlas.atlasops.dev
                  </Link>
                  ).
                </Typography>
              </>
            ) : (
              <>
                <Box>
                  <Typography variant="h5" component="h2" gutterBottom>
                    {directAccess ? 'Sign in (direct access)' : 'Sign in'}
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    {directAccess
                      ? 'You opened Atlas via localhost or a LAN IP/port. Authentik only runs on '
                      : 'Local development credentials. For SSO use '}
                    <Link href={PUBLIC_ATLAS_URL} underline="hover">
                      https://atlas.atlasops.dev
                    </Link>
                    {directAccess ? ' — use that URL for SSO, or sign in locally below.' : '.'}
                  </Typography>
                </Box>

                {directAccess && (
                  <Button
                    variant="outlined"
                    size="large"
                    fullWidth
                    disabled={ssoBusy}
                    onClick={() => void continueWithAuthentik()}
                  >
                    Open Authentik SSO (public URL)
                  </Button>
                )}

                {error && (
                  <Alert severity="error" variant="outlined">
                    {error}
                  </Alert>
                )}

                {localAllowed && (
                  <Box component="form" onSubmit={onSubmit}>
                    <Stack spacing={2}>
                      <TextField
                        label="Username"
                        autoComplete="username"
                        autoFocus
                        fullWidth
                        size="medium"
                        error={!!errors.username}
                        helperText={errors.username?.message}
                        {...register('username')}
                      />
                      <TextField
                        label="Password"
                        type="password"
                        autoComplete="current-password"
                        fullWidth
                        size="medium"
                        error={!!errors.password}
                        helperText={errors.password?.message}
                        {...register('password')}
                      />
                      <Button
                        type="submit"
                        variant="contained"
                        size="large"
                        fullWidth
                        disabled={isSubmitting}
                        sx={{ mt: 0.5 }}
                      >
                        {isSubmitting ? 'Signing in…' : 'Sign in'}
                      </Button>
                    </Stack>
                  </Box>
                )}
              </>
            )}
          </Stack>
        </Box>
      </Box>
    </Box>
  )
}
