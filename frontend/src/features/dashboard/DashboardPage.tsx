import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import {
  Box,
  Link,
  Paper,
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

export function DashboardPage() {
  const statsQuery = useQuery({ queryKey: ['dashboard-stats'], queryFn: meApi.stats })
  const deploymentsQuery = useQuery({
    queryKey: ['deployments', 'recent'],
    queryFn: () => deploymentsApi.list({ page: 0, size: 5, sort: 'createdAt,desc' }),
  })

  return (
    <Stack spacing={3}>
      <Box>
        <Typography variant="h4" gutterBottom>
          Dashboard
        </Typography>
        <Typography color="text.secondary">
          Overview of registered applications, hosts and deployments.
        </Typography>
      </Box>

      <QueryState isLoading={statsQuery.isLoading} isError={statsQuery.isError}>
        <Box
          display="grid"
          gap={2}
          gridTemplateColumns={{ xs: '1fr', md: 'repeat(3, 1fr)' }}
        >
          {[
            { label: 'Applications', value: statsQuery.data?.applications ?? 0, to: '/applications' },
            { label: 'Hosts', value: statsQuery.data?.hosts ?? 0, to: '/hosts' },
            { label: 'Deployments', value: statsQuery.data?.deployments ?? 0, to: '/deployments' },
          ].map((card) => (
            <Paper key={card.label} sx={{ p: 3 }}>
              <Typography variant="overline" color="text.secondary">
                {card.label}
              </Typography>
              <Typography variant="h3" sx={{ my: 1 }}>
                {card.value}
              </Typography>
              <Link component={RouterLink} to={card.to} underline="hover">
                View all
              </Link>
            </Paper>
          ))}
        </Box>
      </QueryState>

      <Paper sx={{ p: 2 }}>
        <Typography variant="h6" sx={{ mb: 2, px: 1 }}>
          Recent deployments
        </Typography>
        <QueryState isLoading={deploymentsQuery.isLoading} isError={deploymentsQuery.isError}>
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
                    <Link component={RouterLink} to={`/deployments/${item.id}`}>
                      {item.id.slice(0, 8)}
                    </Link>
                  </TableCell>
                  <TableCell>{item.status}</TableCell>
                  <TableCell>{new Date(item.createdAt).toLocaleString()}</TableCell>
                </TableRow>
              ))}
              {(deploymentsQuery.data?.content.length ?? 0) === 0 && (
                <TableRow>
                  <TableCell colSpan={3}>No deployments yet</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </QueryState>
      </Paper>
    </Stack>
  )
}
