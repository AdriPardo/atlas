import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import {
  Box,
  Button,
  Link,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import RocketLaunchOutlinedIcon from '@mui/icons-material/RocketLaunchOutlined'
import { deploymentsApi, meApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'
import { useAuth } from '../auth/AuthContext'

function relativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.round(diff / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.round(mins / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.round(hours / 24)
  if (days < 14) return `${days}d ago`
  return new Date(iso).toLocaleDateString()
}

export function DashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const statsQuery = useQuery({ queryKey: ['dashboard-stats'], queryFn: meApi.stats })
  const deploymentsQuery = useQuery({
    queryKey: ['deployments', 'recent'],
    queryFn: () => deploymentsApi.list({ page: 0, size: 8, sort: 'createdAt,desc' }),
  })

  const projectCount = statsQuery.data?.projects ?? statsQuery.data?.applications ?? 0
  const stats = [
    { label: 'Projects', value: projectCount, to: '/projects', hint: 'Repos & services' },
    { label: 'Hosts', value: statsQuery.data?.hosts ?? 0, to: '/hosts', hint: 'LOCAL / SSH' },
    { label: 'Deployments', value: statsQuery.data?.deployments ?? 0, to: '/deployments', hint: 'Release runs' },
  ]

  const recent = deploymentsQuery.data?.content ?? []
  const activeCount = recent.filter((d) => d.status === 'PENDING' || d.status === 'RUNNING').length

  return (
    <PageShell>
      <PageHeader
        title={user?.username ? `Welcome, ${user.username}` : 'Dashboard'}
        description="One view of inventory and the latest deployment activity."
        actions={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate('/projects/new')}
          >
            New project
          </Button>
        }
      />

      <Box
        sx={{
          border: (t) => `1px solid ${t.palette.divider}`,
          borderRadius: 2,
          bgcolor: 'background.paper',
          overflow: 'hidden',
        }}
      >
        <QueryState
          isLoading={statsQuery.isLoading}
          isError={statsQuery.isError}
          onRetry={() => statsQuery.refetch()}
          errorMessage="Could not load dashboard stats."
        >
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
            }}
          >
            {stats.map((stat, index) => (
              <Box
                key={stat.label}
                component={RouterLink}
                to={stat.to}
                sx={{
                  px: 2.75,
                  py: 2.5,
                  textDecoration: 'none',
                  color: 'inherit',
                  borderLeft: (t) =>
                    index === 0 ? 'none' : { xs: 'none', sm: `1px solid ${t.palette.divider}` },
                  borderTop: (t) =>
                    index === 0 ? 'none' : { xs: `1px solid ${t.palette.divider}`, sm: 'none' },
                  transition: (t) => `background-color 160ms ${t.transitions.easing.easeOut}`,
                  '&:hover': {
                    bgcolor: (t) =>
                      t.palette.mode === 'dark' ? 'rgba(45,212,191,0.06)' : 'rgba(15,118,110,0.04)',
                  },
                }}
              >
                <Typography variant="overline" color="text.secondary">
                  {stat.label}
                </Typography>
                <Typography
                  variant="h3"
                  className="atlas-mono"
                  sx={{ mt: 0.75, mb: 0.35, fontSize: { xs: '2rem', md: '2.35rem' } }}
                >
                  {stat.value}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {stat.hint}
                </Typography>
              </Box>
            ))}
          </Box>
        </QueryState>

        <Box
          sx={{
            borderTop: (t) => `1px solid ${t.palette.divider}`,
            px: { xs: 2, sm: 2.75 },
            py: 2,
          }}
        >
          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="baseline"
            gap={2}
            mb={1.75}
            flexWrap="wrap"
          >
            <Box>
              <Typography variant="subtitle1">Recent activity</Typography>
              <Typography variant="caption" color="text.secondary">
                {activeCount > 0
                  ? `${activeCount} deploy${activeCount === 1 ? '' : 's'} in progress`
                  : 'Latest deployment records'}
              </Typography>
            </Box>
            <Link component={RouterLink} to="/deployments" underline="hover" variant="body2">
              All deployments
            </Link>
          </Box>

          <QueryState
            isLoading={deploymentsQuery.isLoading}
            isError={deploymentsQuery.isError}
            onRetry={() => deploymentsQuery.refetch()}
            skeleton="table"
            errorMessage="Could not load recent deployments."
          >
            {recent.length === 0 ? (
              <EmptyState
                icon={<RocketLaunchOutlinedIcon />}
                title="No deployments yet"
                description="Create a project and deploy a service to a host to see activity here."
                actionLabel="New project"
                onAction={() => navigate('/projects/new')}
                secondaryLabel="New deployment"
                onSecondary={() => navigate('/deployments/new')}
              />
            ) : (
              <DataTableFrame>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Run</TableCell>
                      <TableCell>Service</TableCell>
                      <TableCell>Host</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>When</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {recent.map((item) => (
                      <TableRow key={item.id} hover>
                        <TableCell>
                          <Link
                            component={RouterLink}
                            to={`/deployments/${item.id}`}
                            className="atlas-mono"
                            underline="hover"
                            sx={{ fontWeight: 600 }}
                          >
                            {item.id.slice(0, 8)}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" className="atlas-mono" color="text.secondary">
                            {(item.serviceId ?? item.applicationId ?? '—').slice(0, 8)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Link
                            component={RouterLink}
                            to={`/hosts/${item.hostId}`}
                            className="atlas-mono"
                            underline="hover"
                          >
                            {item.hostId.slice(0, 8)}
                          </Link>
                        </TableCell>
                        <TableCell>
                          <StatusChip label={item.status} />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" color="text.secondary" title={new Date(item.createdAt).toLocaleString()}>
                            {relativeTime(item.createdAt)}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </DataTableFrame>
            )}
          </QueryState>
        </Box>
      </Box>
    </PageShell>
  )
}
