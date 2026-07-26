import { Typography } from '@mui/material'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DetailField, DetailPanel } from '../../shared/components/DetailPanel'

export function ProfilePage() {
  const { user } = useAuth()

  return (
    <PageShell maxWidth={560}>
      <PageHeader title="Profile" description="Signed-in account details." />
      <DetailPanel>
        <DetailField label="Username">
          <Typography variant="body2" sx={{ fontWeight: 600 }}>
            {user?.username}
          </Typography>
        </DetailField>
        <DetailField label="Role" mono>
          {user?.role}
        </DetailField>
        <DetailField label="User ID" mono>
          {user?.id}
        </DetailField>
      </DetailPanel>
    </PageShell>
  )
}
