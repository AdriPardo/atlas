import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Box,
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
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined'
import { alertsApi, notificationChannelsApi } from '../../shared/api/endpoints'
import { QueryState } from '../../shared/components/QueryState'
import { PageHeader } from '../../shared/components/PageHeader'
import { PageShell } from '../../shared/components/PageShell'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import type { AlertEventType, NotificationChannelType } from '../../shared/types/api'

export function AlertsPage() {
  const queryClient = useQueryClient()
  const [channelOpen, setChannelOpen] = useState(false)
  const [ruleOpen, setRuleOpen] = useState(false)
  const [channelName, setChannelName] = useState('')
  const [channelType, setChannelType] = useState<NotificationChannelType>('WEBHOOK')
  const [channelTarget, setChannelTarget] = useState('stub://local')
  const [ruleName, setRuleName] = useState('')
  const [eventType, setEventType] = useState<AlertEventType>('DEPLOY_FAILED')
  const [channelId, setChannelId] = useState('')

  const channelsQuery = useQuery({
    queryKey: ['notification-channels'],
    queryFn: () => notificationChannelsApi.list(),
  })
  const alertsQuery = useQuery({
    queryKey: ['alerts'],
    queryFn: () => alertsApi.list(),
  })

  const createChannel = useMutation({
    mutationFn: () =>
      notificationChannelsApi.create({
        name: channelName.trim(),
        type: channelType,
        target: channelTarget.trim(),
      }),
    onSuccess: async () => {
      setChannelOpen(false)
      setChannelName('')
      setChannelTarget(channelType === 'EMAIL' ? '' : 'stub://local')
      await queryClient.invalidateQueries({ queryKey: ['notification-channels'] })
    },
  })

  const removeChannel = useMutation({
    mutationFn: (id: string) => notificationChannelsApi.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notification-channels'] })
    },
  })

  const createRule = useMutation({
    mutationFn: () =>
      alertsApi.create({
        name: ruleName.trim(),
        eventType,
        channelId,
      }),
    onSuccess: async () => {
      setRuleOpen(false)
      setRuleName('')
      await queryClient.invalidateQueries({ queryKey: ['alerts'] })
    },
  })

  const silenceRule = useMutation({
    mutationFn: (id: string) => alertsApi.silence(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['alerts'] })
    },
  })

  const removeRule = useMutation({
    mutationFn: (id: string) => alertsApi.remove(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['alerts'] })
    },
  })

  const channels = channelsQuery.data ?? []
  const rules = alertsQuery.data ?? []

  return (
    <PageShell>
      <PageHeader
        title="Alerts"
        description="Product alert rules and notification destinations (webhook / email stubs)."
        actions={
          <Stack direction="row" spacing={1}>
            <Button variant="outlined" startIcon={<AddIcon />} onClick={() => setChannelOpen(true)}>
              New channel
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                if (channels[0] && !channelId) setChannelId(channels[0].id)
                setRuleOpen(true)
              }}
              disabled={channels.length === 0}
            >
              New alert
            </Button>
          </Stack>
        }
      />

      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650, mb: 1 }}>
        Notification channels
      </Typography>
      <DataTableFrame>
        <QueryState isLoading={channelsQuery.isLoading} isError={channelsQuery.isError}>
          {channels.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<NotificationsOutlinedIcon />}
                title="No channels"
                description="Create a webhook or email channel before adding alert rules."
                actionLabel="New channel"
                onAction={() => setChannelOpen(true)}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Target</TableCell>
                  <TableCell>Enabled</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {channels.map((ch) => (
                  <TableRow key={ch.id} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {ch.name}
                      </Typography>
                    </TableCell>
                    <TableCell>{ch.type}</TableCell>
                    <TableCell>
                      <Typography variant="body2" className="atlas-mono" color="text.secondary">
                        {ch.target}
                      </Typography>
                    </TableCell>
                    <TableCell>{ch.enabled ? 'Yes' : 'No'}</TableCell>
                    <TableCell align="right">
                      <RowOverflowMenu
                        aria-label={`Actions for channel ${ch.name}`}
                        items={[
                          {
                            label: 'Delete',
                            destructive: true,
                            onClick: () => removeChannel.mutate(ch.id),
                          },
                        ]}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650, mb: 1, mt: 3 }}>
        Alert rules
      </Typography>
      <DataTableFrame>
        <QueryState isLoading={alertsQuery.isLoading} isError={alertsQuery.isError}>
          {rules.length === 0 ? (
            <Box p={2}>
              <EmptyState
                icon={<NotificationsOutlinedIcon />}
                title="No alert rules"
                description="Route deploy or job failures to a notification channel."
                actionLabel={channels.length === 0 ? undefined : 'New alert'}
                onAction={channels.length === 0 ? undefined : () => setRuleOpen(true)}
              />
            </Box>
          ) : (
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Event</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Channel</TableCell>
                  <TableCell>Last fired</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rules.map((rule) => {
                  const channel = channels.find((c) => c.id === rule.channelId)
                  return (
                    <TableRow key={rule.id} hover>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontWeight: 600 }}>
                          {rule.name}
                        </Typography>
                        {rule.lastError && (
                          <Typography variant="caption" color="error">
                            {rule.lastError}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>{rule.eventType}</TableCell>
                      <TableCell>
                        <StatusChip label={rule.status} />
                      </TableCell>
                      <TableCell>{channel?.name ?? rule.channelId.slice(0, 8)}</TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {rule.lastFiredAt ? new Date(rule.lastFiredAt).toLocaleString() : '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.25} justifyContent="flex-end" alignItems="center">
                          {rule.status !== 'SILENCED' && (
                            <Button size="small" onClick={() => silenceRule.mutate(rule.id)}>
                              Silence
                            </Button>
                          )}
                          <RowOverflowMenu
                            aria-label={`More actions for ${rule.name}`}
                            items={[
                              {
                                label: 'Delete',
                                destructive: true,
                                onClick: () => removeRule.mutate(rule.id),
                              },
                            ]}
                          />
                        </Stack>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          )}
        </QueryState>
      </DataTableFrame>

      <Dialog open={channelOpen} onClose={() => setChannelOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create notification channel</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {createChannel.isError && (
              <Alert severity="error" variant="outlined">
                Unable to create channel (name may already exist or target invalid)
              </Alert>
            )}
            <TextField
              label="Name"
              value={channelName}
              onChange={(e) => setChannelName(e.target.value)}
              fullWidth
              autoFocus
            />
            <TextField
              select
              label="Type"
              value={channelType}
              onChange={(e) => {
                const next = e.target.value as NotificationChannelType
                setChannelType(next)
                if (next === 'WEBHOOK' && !channelTarget) setChannelTarget('stub://local')
              }}
              fullWidth
            >
              <MenuItem value="WEBHOOK">WEBHOOK</MenuItem>
              <MenuItem value="EMAIL">EMAIL</MenuItem>
            </TextField>
            <TextField
              label="Target"
              value={channelTarget}
              onChange={(e) => setChannelTarget(e.target.value)}
              helperText={
                channelType === 'WEBHOOK'
                  ? 'http(s) URL or stub://local for dry-run delivery'
                  : 'Email address'
              }
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setChannelOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!channelName.trim() || !channelTarget.trim() || createChannel.isPending}
            onClick={() => createChannel.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={ruleOpen} onClose={() => setRuleOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Create alert rule</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {createRule.isError && (
              <Alert severity="error" variant="outlined">
                Unable to create alert rule
              </Alert>
            )}
            <TextField
              label="Name"
              value={ruleName}
              onChange={(e) => setRuleName(e.target.value)}
              fullWidth
              autoFocus
            />
            <TextField
              select
              label="Event"
              value={eventType}
              onChange={(e) => setEventType(e.target.value as AlertEventType)}
              fullWidth
            >
              <MenuItem value="DEPLOY_FAILED">DEPLOY_FAILED</MenuItem>
              <MenuItem value="JOB_FAILED">JOB_FAILED</MenuItem>
            </TextField>
            <TextField
              select
              label="Channel"
              value={channelId}
              onChange={(e) => setChannelId(e.target.value)}
              fullWidth
            >
              {channels.map((ch) => (
                <MenuItem key={ch.id} value={ch.id}>
                  {ch.name} ({ch.type})
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRuleOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!ruleName.trim() || !channelId || createRule.isPending}
            onClick={() => createRule.mutate()}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  )
}
