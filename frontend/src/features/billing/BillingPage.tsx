import { useQuery } from '@tanstack/react-query'
import {
  Button,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import { billingApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { QueryState } from '../../shared/components/QueryState'
import type { UsageRecord } from '../../shared/types/api'
import { useAuth } from '../auth/AuthContext'
import { useFeatureFlags } from '../platform/useFeatureFlags'

function exportUsageCsv(rows: UsageRecord[]) {
  const header = ['createdAt', 'meter', 'quantity', 'periodStart', 'periodEnd', 'dimensions']
  const lines = [
    header.join(','),
    ...rows.map((row) =>
      [
        row.createdAt,
        row.meter,
        row.quantity,
        row.periodStart,
        row.periodEnd,
        `"${row.dimensions.replaceAll('"', '""')}"`,
      ].join(','),
    ),
  ]
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `atlas-usage-${new Date().toISOString().slice(0, 10)}.csv`
  anchor.click()
  URL.revokeObjectURL(url)
}

export function BillingPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const { planCode, billingEnabled, isEnterprise } = useFeatureFlags()

  const entitlementsQuery = useQuery({
    queryKey: ['billing', 'entitlements'],
    queryFn: () => billingApi.entitlements(),
    enabled: isAdmin && billingEnabled,
  })

  const usageQuery = useQuery({
    queryKey: ['billing', 'usage'],
    queryFn: () => billingApi.usage({ page: 0, size: 200, sort: 'createdAt,desc' }),
    enabled: isAdmin && billingEnabled,
  })

  const usageRows = usageQuery.data?.content ?? []
  const entitlements = entitlementsQuery.data

  return (
    <PageShell>
      <PageHeader
        title="Billing"
        description="Usage meters and local plan entitlements (price may be 0). No Stripe required."
        actions={
          isAdmin && billingEnabled ? (
            <Button
              variant="outlined"
              startIcon={<DownloadOutlinedIcon />}
              disabled={usageRows.length === 0}
              onClick={() => exportUsageCsv(usageRows)}
            >
              Export CSV
            </Button>
          ) : undefined
        }
      />
      {!isAdmin ? (
        <EmptyState title="Admin only" description="Billing usage is restricted to ADMIN users." />
      ) : !billingEnabled ? (
        <EmptyState
          title="Billing disabled"
          description="Feature flag billing is off for this installation (ATLAS_FEATURE_BILLING)."
        />
      ) : (
        <Stack spacing={3}>
          <DataTableFrame>
            <QueryState isLoading={entitlementsQuery.isLoading} isError={entitlementsQuery.isError}>
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                Plan: {entitlements?.planCode ?? planCode}
                {isEnterprise ? ' · enterprise' : ''}
              </Typography>
              <Table size="small" sx={{ mb: 2 }}>
                <TableHead>
                  <TableRow>
                    <TableCell>Meter</TableCell>
                    <TableCell>Live</TableCell>
                    <TableCell>Limit</TableCell>
                    <TableCell>Unit</TableCell>
                    <TableCell>Price</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {(entitlements?.entitlements ?? []).map((row) => {
                    const live = entitlements?.gauges.find((g) => g.meter === row.meter)?.quantity
                    return (
                      <TableRow key={row.meter} hover>
                        <TableCell className="atlas-mono">{row.meter}</TableCell>
                        <TableCell>{live == null ? '—' : live}</TableCell>
                        <TableCell>{row.unlimited ? 'unlimited' : row.limitQuantity}</TableCell>
                        <TableCell>{row.unit}</TableCell>
                        <TableCell>{(row.priceCents / 100).toFixed(2)}</TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </QueryState>
          </DataTableFrame>

          <DataTableFrame>
            <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
              Usage records
            </Typography>
            <QueryState isLoading={usageQuery.isLoading} isError={usageQuery.isError}>
              {usageRows.length === 0 ? (
                <EmptyState
                  title="No usage yet"
                  description="Deploy a service or run a job/backup to record deploy.count, job.minutes, and backup.gb."
                />
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>When</TableCell>
                      <TableCell>Meter</TableCell>
                      <TableCell>Qty</TableCell>
                      <TableCell>Period</TableCell>
                      <TableCell>Dimensions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {usageRows.map((row) => (
                      <TableRow key={row.id} hover>
                        <TableCell>{new Date(row.createdAt).toLocaleString()}</TableCell>
                        <TableCell className="atlas-mono">{row.meter}</TableCell>
                        <TableCell>{row.quantity}</TableCell>
                        <TableCell>
                          <Typography variant="caption" className="atlas-mono">
                            {new Date(row.periodStart).toLocaleDateString()} –{' '}
                            {new Date(row.periodEnd).toLocaleDateString()}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" className="atlas-mono">
                            {row.dimensions}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </QueryState>
          </DataTableFrame>
        </Stack>
      )}
    </PageShell>
  )
}
