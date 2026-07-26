import { useQuery } from '@tanstack/react-query'
import { Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material'
import { auditApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { QueryState } from '../../shared/components/QueryState'
import { useAuth } from '../auth/AuthContext'

export function AuditListPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const query = useQuery({
    queryKey: ['audit'],
    queryFn: () => auditApi.list({ page: 0, size: 100 }),
    enabled: isAdmin,
  })

  const rows = query.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Audit"
        description="Append-only trail of privileged actions (deploy, pipeline run)."
      />
      {!isAdmin ? (
        <EmptyState title="Admin only" description="Audit log is restricted to ADMIN users." />
      ) : (
        <DataTableFrame>
          <QueryState isLoading={query.isLoading} isError={query.isError}>
            {rows.length === 0 ? (
              <EmptyState title="No entries" description="Deploy or run a pipeline to create audit events." />
            ) : (
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>When</TableCell>
                    <TableCell>Actor</TableCell>
                    <TableCell>Action</TableCell>
                    <TableCell>Resource</TableCell>
                    <TableCell>Metadata</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} hover>
                      <TableCell>{new Date(row.createdAt).toLocaleString()}</TableCell>
                      <TableCell>{row.actorUsername}</TableCell>
                      <TableCell>{row.action}</TableCell>
                      <TableCell>
                        <Typography variant="body2" className="atlas-mono">
                          {row.resourceType}:{row.resourceId?.slice(0, 8) || '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" className="atlas-mono">
                          {row.metadata}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </QueryState>
        </DataTableFrame>
      )}
    </PageShell>
  )
}
