import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { Navigate, useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Button,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useAuth } from './AuthContext'

const schema = z.object({
  username: z.string().min(1, 'Required'),
  password: z.string().min(1, 'Required'),
})

type FormValues = z.infer<typeof schema>

export function LoginPage() {
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
      minHeight="100vh"
      display="flex"
      alignItems="center"
      justifyContent="center"
      px={2}
      sx={{
        background:
          'radial-gradient(1200px 600px at 10% -10%, rgba(15,118,110,0.22), transparent), radial-gradient(900px 500px at 100% 0%, rgba(45,212,191,0.12), transparent)',
      }}
    >
      <Paper sx={{ width: '100%', maxWidth: 420, p: { xs: 3, sm: 4 } }}>
        <Stack spacing={3}>
          <Box>
            <Typography variant="h4" color="primary" gutterBottom>
              Atlas
            </Typography>
            <Typography color="text.secondary">
              Sign in to manage self-hosted application operations.
            </Typography>
          </Box>
          {error && <Alert severity="error">{error}</Alert>}
          <Box component="form" onSubmit={onSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Username"
                autoComplete="username"
                error={!!errors.username}
                helperText={errors.username?.message}
                {...register('username')}
              />
              <TextField
                label="Password"
                type="password"
                autoComplete="current-password"
                error={!!errors.password}
                helperText={errors.password?.message}
                {...register('password')}
              />
              <Button type="submit" variant="contained" size="large" disabled={isSubmitting}>
                Sign in
              </Button>
            </Stack>
          </Box>
        </Stack>
      </Paper>
    </Box>
  )
}
