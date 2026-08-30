import { Button, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import { auditApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { QueryState } from '../../shared/components/QueryState'
import type { AuditEntry } from '../../shared/types/api'
import { useAuth } from '../auth/AuthContext'
import { useFeatureFlags } from '../platform/useFeatureFlags'

import { useAuthQuery } from '../auth/useAuthQuery'
function exportAuditJson(rows: AuditEntry[]) {
  const blob = new Blob([JSON.stringify(rows, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `atlas-audit-${new Date().toISOString().slice(0, 10)}.json`
  anchor.click()
  URL.revokeObjectURL(url)
}

export function AuditListPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const { auditExportEnabled } = useFeatureFlags()

  const query = useAuthQuery({
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
        actions={
          isAdmin && auditExportEnabled ? (
            <Button
              variant="outlined"
              startIcon={<DownloadOutlinedIcon />}
              disabled={rows.length === 0}
              onClick={async () => {
                const exported = await auditApi.export()
                exportAuditJson(exported)
              }}
            >
              Export JSON
            </Button>
          ) : undefined
        }
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
