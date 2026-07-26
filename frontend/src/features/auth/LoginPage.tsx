import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Navigate, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  IconButton,
  Stack,
  TextField,
  Typography,
  useTheme,
} from '@mui/material'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import { useAuth } from './AuthContext'

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
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) })

  if (!loading && user) {
    return <Navigate to="/" replace />
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
        <Box display="flex" alignItems="center" justifyContent="space-between">
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
          {onToggleMode && (
            <IconButton onClick={onToggleMode} aria-label="Toggle theme" size="small">
              {(mode ?? theme.palette.mode) === 'dark' ? (
                <LightModeOutlinedIcon fontSize="small" />
              ) : (
                <DarkModeOutlinedIcon fontSize="small" />
              )}
            </IconButton>
          )}
        </Box>

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
            <Box>
              <Typography variant="h5" component="h2" gutterBottom>
                Sign in
              </Typography>
              <Typography color="text.secondary" variant="body2">
                Use your Atlas credentials to continue.
              </Typography>
            </Box>

            {error && (
              <Alert severity="error" variant="outlined">
                {error}
              </Alert>
            )}

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
          </Stack>
        </Box>
      </Box>
    </Box>
  )
}
