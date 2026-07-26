import { Paper, Stack, Typography } from '@mui/material'
import { useAuth } from '../auth/AuthContext'

export function ProfilePage() {
  const { user } = useAuth()

  return (
    <Stack spacing={3}>
      <Typography variant="h4">Profile</Typography>
      <Paper sx={{ p: 3, maxWidth: 560 }}>
        <Stack spacing={1.5}>
          <Typography>
            <strong>Username:</strong> {user?.username}
          </Typography>
          <Typography>
            <strong>Role:</strong> {user?.role}
          </Typography>
          <Typography>
            <strong>User ID:</strong> {user?.id}
          </Typography>
        </Stack>
      </Paper>
    </Stack>
  )
}
