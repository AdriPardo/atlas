import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import ScheduleOutlinedIcon from '@mui/icons-material/ScheduleOutlined'
import { cronJobsApi, hostsApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { QueryState } from '../../shared/components/QueryState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import type { CronTargetType } from '../../shared/types/api'

export function CronJobsPage() {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [cronExpression, setCronExpression] = useState('0 */15 * * * *')
  const [targetType, setTargetType] = useState<CronTargetType>('SYNC_HOST')
  const [targetId, setTargetId] = useState('')

  const cronQuery = useQuery({
    queryKey: ['cron-jobs'],
    queryFn: () => cronJobsApi.list(),
  })
  const hostsQuery = useQuery({
    queryKey: ['hosts'],
    queryFn: () => hostsApi.list({ page: 0, size: 100 }),
  })

  const createMutation = useMutation({
    mutationFn: () =>
      cronJobsApi.create({
        name: name.trim(),
        cronExpression: cronExpression.trim(),
        targetType,
        targetId: targetType === 'SYNC_HOST' ? targetId : undefined,
      }),
    onSuccess: async () => {
      setOpen(false)
      setName('')
      setCronExpression('0 */15 * * * *')
      setTargetType('SYNC_HOST')
      setTargetId('')
      await queryClient.invalidateQueries({ queryKey: ['cron-jobs'] })
    },
  })

  const toggleMutation = useMutation({
    mutationFn: (row: {
      id: string
      name: string
      cronExpression: string
      targetType: CronTargetType
      targetId: string | null
      enabled: boolean
    }) =>
      cronJobsApi.update(row.id, {
        name: row.name,
        cronExpression: row.cronExpression,
        targetType: row.targetType,
        targetId: row.targetId,
        enabled: !row.enabled,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['cron-jobs'] })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (id: string) => cronJobsApi.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['cron-jobs'] })
    },
  })

  const rows = cronQuery.data ?? []
  const hosts = hostsQuery.data?.content ?? []

  return (
    <PageShell>
      <PageHeader
        title="Cron"
        description="Schedules that sync hosts or enqueue database backups."
        actions={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setOpen(true)}>
            New cron
          </Button>
        }
      />
      <QueryState isLoading={cronQuery.isLoading} isError={cronQuery.isError}>
        {rows.length === 0 ? (
          <EmptyState
            icon={<ScheduleOutlinedIcon />}
            title="No cron jobs"
            description="Create a schedule to sync a host or enqueue database backups."
            actionLabel="New cron"
            onAction={() => setOpen(true)}
          />
        ) : (
          <DataTableFrame>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Expression</TableCell>
                  <TableCell>Target</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Last fired</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} hover>
                    <TableCell>{row.name}</TableCell>
                    <TableCell className="atlas-mono">{row.cronExpression}</TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        {row.targetType}
                        {row.targetId ? ` · ${row.targetId.slice(0, 8)}` : ''}
                      </Typography>
                      {row.lastError && (
                        <Typography variant="caption" color="error">
                          {row.lastError}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>
                      <StatusChip label={row.enabled ? 'ENABLED' : 'DISABLED'} />
                    </TableCell>
                    <TableCell>
                      {row.lastFiredAt ? new Date(row.lastFiredAt).toLocaleString() : '—'}
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.25} justifyContent="flex-end" alignItems="center">
                        <Button
                          size="small"
                          disabled={toggleMutation.isPending}
                          onClick={() => toggleMutation.mutate(row)}
                        >
                          {row.enabled ? 'Disable' : 'Enable'}
                        </Button>
                        <RowOverflowMenu
                          aria-label={`More actions for ${row.name}`}
                          items={[
                            {
                              label: 'Delete',
                              destructive: true,
                              disabled: removeMutation.isPending,
                              onClick: () => removeMutation.mutate(row.id),
                            },
                          ]}
                        />
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataTableFrame>
        )}
      </QueryState>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create cron job</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            {createMutation.isError && (
              <Alert severity="error" variant="outlined">
                Unable to create cron job
              </Alert>
            )}
            <TextField label="Name" size="small" value={name} onChange={(e) => setName(e.target.value)} fullWidth />
            <TextField
              label="Cron expression"
              size="small"
              value={cronExpression}
              onChange={(e) => setCronExpression(e.target.value)}
              helperText="Spring 6-field: second minute hour day-of-month month day-of-week"
              fullWidth
            />
            <TextField
              select
              label="Target"
              size="small"
              value={targetType}
              onChange={(e) => setTargetType(e.target.value as CronTargetType)}
              fullWidth
            >
              <MenuItem value="SYNC_HOST">SYNC_HOST</MenuItem>
              <MenuItem value="BACKUP_DATABASE">BACKUP_DATABASE</MenuItem>
            </TextField>
            {targetType === 'SYNC_HOST' && (
              <TextField
                select
                label="Host"
                size="small"
                value={targetId}
                onChange={(e) => setTargetId(e.target.value)}
                fullWidth
              >
                {hosts.map((h) => (
                  <MenuItem key={h.id} value={h.id}>
                    {h.hostname}
                  </MenuItem>
                ))}
              </TextField>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={
              !name.trim() ||
              !cronExpression.trim() ||
              (targetType === 'SYNC_HOST' && !targetId) ||
              createMutation.isPending
            }
            onClick={() => createMutation.mutate()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}
