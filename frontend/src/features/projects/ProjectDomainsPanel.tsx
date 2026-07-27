import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
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
import { useState } from 'react'
import { domainsApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { QueryState } from '../../shared/components/QueryState'
import { StatusChip } from '../../shared/components/StatusChip'
import type { Service } from '../../shared/types/api'

export function ProjectDomainsPanel({
  projectId,
  services,
}: {
  projectId: string
  services: Service[]
}) {
  const queryClient = useQueryClient()
  const [hostname, setHostname] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [labelsPreview, setLabelsPreview] = useState<string | null>(null)
  const [tunnelPreview, setTunnelPreview] = useState<{
    title: string
    body: string
    hint?: string
  } | null>(null)
  const [copyStatus, setCopyStatus] = useState<string | null>(null)

  const domainsQuery = useQuery({
    queryKey: ['projects', projectId, 'domains'],
    queryFn: () => domainsApi.list(projectId),
    enabled: !!projectId,
  })

  const createMutation = useMutation({
    mutationFn: () =>
      domainsApi.create(projectId, {
        hostname: hostname.trim(),
        serviceId: serviceId || undefined,
      }),
    onSuccess: async () => {
      setHostname('')
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'domains'] })
    },
  })

  const verifyMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.verify(domainId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'domains'] })
    },
  })

  const removeMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.remove(domainId),
    onSuccess: async () => {
      setLabelsPreview(null)
      setTunnelPreview(null)
      await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'domains'] })
    },
  })

  const traefikMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.traefik(domainId),
    onSuccess: (data) => {
      setTunnelPreview(null)
      setLabelsPreview(
        Object.entries(data.labels)
          .map(([k, v]) => `${k}=${v}`)
          .join('\n'),
      )
    },
  })

  const tunnelMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.tunnelIngress(domainId),
    onSuccess: (data) => {
      setLabelsPreview(null)
      setTunnelPreview({
        title: 'Cloudflare Tunnel ingress',
        body: data.copyBlock,
        hint: data.zeroTrustHint,
      })
    },
  })

  const ensureTunnelMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.ensureTunnel(domainId),
    onSuccess: (data) => {
      setLabelsPreview(null)
      const modeLabel = data.mode ? `[${data.mode}] ` : ''
      setTunnelPreview({
        title: `${modeLabel}Cloudflare Tunnel`,
        body: [data.message, '', data.copyBlock].filter(Boolean).join('\n'),
        hint: data.zeroTrustHint,
      })
    },
  })

  const copyTunnel = async () => {
    if (!tunnelPreview?.body) return
    await navigator.clipboard.writeText(tunnelPreview.body)
    setCopyStatus('Copied')
    window.setTimeout(() => setCopyStatus(null), 2000)
  }

  const rows = domainsQuery.data ?? []

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
        Domains
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
        <TextField
          label="Hostname"
          size="small"
          value={hostname}
          onChange={(e) => setHostname(e.target.value)}
          sx={{ minWidth: 220 }}
          placeholder="app.example.com"
        />
        <TextField
          select
          label="Service"
          size="small"
          value={serviceId}
          onChange={(e) => setServiceId(e.target.value)}
          sx={{ minWidth: 180 }}
        >
          <MenuItem value="">None</MenuItem>
          {services.map((svc) => (
            <MenuItem key={svc.id} value={svc.id}>
              {svc.name}
            </MenuItem>
          ))}
        </TextField>
        <Button
          variant="outlined"
          disabled={!hostname.trim() || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          Add
        </Button>
      </Stack>
      {createMutation.isError && (
        <Alert severity="error" variant="outlined">
          Unable to add domain (need WRITE membership or ADMIN).
        </Alert>
      )}
      <QueryState isLoading={domainsQuery.isLoading} isError={domainsQuery.isError}>
        {rows.length === 0 ? (
          <EmptyState
            title="No domains"
            description="Register a hostname to track DNS challenge, Traefik labels, and Tunnel ingress."
          />
        ) : (
          <DataTableFrame>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Hostname</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Certificate</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((d) => (
                  <TableRow key={d.id}>
                    <TableCell className="atlas-mono">{d.hostname}</TableCell>
                    <TableCell>
                      <StatusChip label={d.status} />
                    </TableCell>
                    <TableCell>
                      {d.certificateIssuer
                        ? `${d.certificateIssuer}${
                            d.certificateExpiresAt
                              ? ` · exp ${new Date(d.certificateExpiresAt).toLocaleDateString()}`
                              : ''
                          }`
                        : '—'}
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={0.5} justifyContent="flex-end" flexWrap="wrap">
                        {d.status !== 'ACTIVE' && (
                          <Button
                            size="small"
                            disabled={verifyMutation.isPending}
                            onClick={() => verifyMutation.mutate(d.id)}
                          >
                            Verify
                          </Button>
                        )}
                        <Button
                          size="small"
                          disabled={traefikMutation.isPending}
                          onClick={() => traefikMutation.mutate(d.id)}
                        >
                          Traefik
                        </Button>
                        <Button
                          size="small"
                          disabled={tunnelMutation.isPending}
                          onClick={() => tunnelMutation.mutate(d.id)}
                        >
                          Tunnel
                        </Button>
                        <Button
                          size="small"
                          disabled={ensureTunnelMutation.isPending}
                          onClick={() => ensureTunnelMutation.mutate(d.id)}
                        >
                          Ensure
                        </Button>
                        <Button
                          size="small"
                          color="error"
                          disabled={removeMutation.isPending}
                          onClick={() => removeMutation.mutate(d.id)}
                        >
                          Remove
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </DataTableFrame>
        )}
      </QueryState>
      {labelsPreview && (
        <Alert severity="info" variant="outlined" onClose={() => setLabelsPreview(null)}>
          <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
            Traefik labels
          </Typography>
          <Typography
            component="pre"
            variant="body2"
            sx={{ m: 0, whiteSpace: 'pre-wrap', fontFamily: 'ui-monospace, monospace' }}
          >
            {labelsPreview}
          </Typography>
        </Alert>
      )}
      {tunnelPreview && (
        <Alert severity="info" variant="outlined" onClose={() => setTunnelPreview(null)}>
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
            <Typography variant="subtitle2" sx={{ flex: 1 }}>
              {tunnelPreview.title}
            </Typography>
            <Button size="small" onClick={() => void copyTunnel()}>
              {copyStatus ?? 'Copy ingress'}
            </Button>
          </Stack>
          {tunnelPreview.hint && (
            <Typography variant="caption" display="block" sx={{ mb: 0.5, opacity: 0.85 }}>
              {tunnelPreview.hint}
            </Typography>
          )}
          <Typography
            component="pre"
            variant="body2"
            sx={{ m: 0, whiteSpace: 'pre-wrap', fontFamily: 'ui-monospace, monospace' }}
          >
            {tunnelPreview.body}
          </Typography>
        </Alert>
      )}
    </Stack>
  )
}
