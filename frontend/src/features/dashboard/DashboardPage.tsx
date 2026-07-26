import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import {
  Box,
  Link,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { deploymentsApi, meApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { StatusChip } from '../../shared/components/StatusChip'

export function DashboardPage() {
  const statsQuery = useQuery({ queryKey: ['dashboard-stats'], queryFn: meApi.stats })
  const deploymentsQuery = useQuery({
    queryKey: ['deployments', 'recent'],
    queryFn: () => deploymentsApi.list({ page: 0, size: 5, sort: 'createdAt,desc' }),
  })

  const stats = [
    { label: 'Applications', value: statsQuery.data?.applications ?? 0, to: '/applications' },
    { label: 'Hosts', value: statsQuery.data?.hosts ?? 0, to: '/hosts' },
    { label: 'Deployments', value: statsQuery.data?.deployments ?? 0, to: '/deployments' },
  ]

  return (
    <PageShell>
      <PageHeader
        title="Dashboard"
        description="Inventory snapshot and the latest deployment activity."
      />

      <QueryState isLoading={statsQuery.isLoading} isError={statsQuery.isError}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
            border: (t) => `1px solid ${t.palette.divider}`,
            borderRadius: 2,
            bgcolor: 'background.paper',
            overflow: 'hidden',
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
                sx={{ mt: 0.75, mb: 0.5, fontSize: { xs: '2rem', md: '2.35rem' } }}
              >
                {stat.value}
              </Typography>
              <Typography variant="body2" color="primary.main" sx={{ fontWeight: 600 }}>
                View all
              </Typography>
            </Box>
          ))}
        </Box>
      </QueryState>

      <Stack spacing={1.5}>
        <Box display="flex" justifyContent="space-between" alignItems="baseline" gap={2}>
          <Typography variant="h6">Recent deployments</Typography>
          <Link component={RouterLink} to="/deployments" underline="hover" variant="body2">
            All deployments
          </Link>
        </Box>

        <QueryState isLoading={deploymentsQuery.isLoading} isError={deploymentsQuery.isError}>
          {(deploymentsQuery.data?.content.length ?? 0) === 0 ? (
            <EmptyState
              title="No deployments yet"
              description="Create a deployment record when you are ready to track a release."
            />
          ) : (
            <DataTableFrame>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Created</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(deploymentsQuery.data?.content ?? []).map((item) => (
                    <TableRow key={item.id} hover>
                      <TableCell>
                        <Link
                          component={RouterLink}
                          to={`/deployments/${item.id}`}
                          className="atlas-mono"
                          underline="hover"
                        >
                          {item.id.slice(0, 8)}
                        </Link>
                      </TableCell>
                      <TableCell>
                        <StatusChip label={item.status} />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {new Date(item.createdAt).toLocaleString()}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </DataTableFrame>
          )}
        </QueryState>
      </Stack>
    </PageShell>
  )
}
