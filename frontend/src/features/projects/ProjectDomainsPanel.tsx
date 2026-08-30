import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Link,
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
import { Link as RouterLink } from 'react-router-dom'
import { domainsApi } from '../../shared/api/endpoints'
import { DataTableFrame } from '../../shared/components/DataTableFrame'
import { EmptyState } from '../../shared/components/EmptyState'
import { QueryState } from '../../shared/components/QueryState'
import { RowOverflowMenu } from '../../shared/components/RowOverflowMenu'
import { StatusChip } from '../../shared/components/StatusChip'
import type { DnsCname, Service, TunnelIngress } from '../../shared/types/api'
import {
  cloudflareScopesFailureHint,
  isCloudflareScopesFailure,
} from './cloudflareScopesFailure'

import { useAuthQuery } from '../auth/useAuthQuery'
type PreviewState = {
  title: string
  body: string
  hint?: string
  severity?: 'info' | 'warning' | 'error'
  scopesFailure?: boolean
}

function previewFromEnsure(
  productTitle: string,
  mode: string | null | undefined,
  message: string | null | undefined,
  copyBlock: string,
  hint?: string,
): PreviewState {
  const scopesFailure = isCloudflareScopesFailure(mode, message)
  const failed = mode === 'FAILED'
  return {
    title: formatModeTitle(mode, productTitle),
    body: [message, '', copyBlock].filter(Boolean).join('\n'),
    hint: scopesFailure ? cloudflareScopesFailureHint() : hint,
    severity: scopesFailure ? 'warning' : failed ? 'error' : 'info',
    scopesFailure,
  }
}

function formatModeTitle(mode: string | null | undefined, productTitle: string) {
  return mode ? `${productTitle} · ${mode}` : productTitle
}

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
  const [preview, setPreview] = useState<PreviewState | null>(null)
  const [copyStatus, setCopyStatus] = useState<string | null>(null)
  const [busyDomainId, setBusyDomainId] = useState<string | null>(null)

  const domainsQuery = useAuthQuery({
    queryKey: ['projects', projectId, 'domains'],
    queryFn: () => domainsApi.list(projectId),
    enabled: !!projectId,
  })

  const invalidateDomains = async () => {
    await queryClient.invalidateQueries({ queryKey: ['projects', projectId, 'domains'] })
  }

  const createMutation = useMutation({
    mutationFn: () =>
      domainsApi.create(projectId, {
        hostname: hostname.trim(),
        serviceId: serviceId || undefined,
      }),
    onSuccess: async () => {
      setHostname('')
      await invalidateDomains()
    },
  })

  const verifyMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.verify(domainId),
    onSuccess: async () => {
      await invalidateDomains()
    },
  })

  const removeMutation = useMutation({
    mutationFn: (domainId: string) => domainsApi.remove(domainId),
    onSuccess: async () => {
      setPreview(null)
      await invalidateDomains()
    },
  })

  const showIngress = useMutation({
    mutationFn: (domainId: string) => domainsApi.traefik(domainId),
    onSuccess: (data) => {
      setPreview({
        title: 'Ingress labels',
        body: Object.entries(data.labels)
          .map(([k, v]) => `${k}=${v}`)
          .join('\n'),
        hint: 'Advanced · gateway labels for this hostname',
      })
    },
  })

  const showPublicAccess = useMutation({
    mutationFn: (domainId: string) => domainsApi.tunnelIngress(domainId),
    onSuccess: (data: TunnelIngress) => {
      setPreview({
        title: 'Public access config',
        body: data.copyBlock,
        hint: data.zeroTrustHint,
      })
    },
  })

  const ensurePublicAccessOnly = useMutation({
    mutationFn: (domainId: string) => domainsApi.ensureTunnel(domainId),
    onSuccess: (data: TunnelIngress) => {
      setPreview(
        previewFromEnsure(
          'Public access',
          data.mode,
          data.message,
          data.copyBlock,
          data.zeroTrustHint ?? undefined,
        ),
      )
    },
  })

  const showDns = useMutation({
    mutationFn: (domainId: string) => domainsApi.dnsCname(domainId),
    onSuccess: (data: DnsCname) => {
      setPreview({
        title: 'DNS record',
        body: data.copyBlock,
        hint: 'Add a proxied CNAME at your DNS provider if ensure is not configured',
      })
    },
  })

  const ensureDnsOnly = useMutation({
    mutationFn: (domainId: string) => domainsApi.ensureDnsCname(domainId),
    onSuccess: async (data: DnsCname) => {
      setPreview(
        previewFromEnsure(
          'DNS',
          data.mode,
          data.message,
          data.copyBlock,
          'Proxied CNAME → public access target',
        ),
      )
      await invalidateDomains()
    },
  })

  /** Happy path: ensure public access + DNS in one click. */
  const publishMutation = useMutation({
    mutationFn: async (domainId: string) => {
      setBusyDomainId(domainId)
      const tunnel = await domainsApi.ensureTunnel(domainId)
      const dns = await domainsApi.ensureDnsCname(domainId)
      return { tunnel, dns }
    },
    onSuccess: async ({ tunnel, dns }) => {
      const scopesFailure =
        isCloudflareScopesFailure(tunnel.mode, tunnel.message) ||
        isCloudflareScopesFailure(dns.mode, dns.message)
      const failed = tunnel.mode === 'FAILED' || dns.mode === 'FAILED'
      setPreview({
        title: scopesFailure
          ? 'Publish blocked · token scopes'
          : failed
            ? 'Publish incomplete'
            : 'Published',
        body: [
          tunnel.message,
          dns.message,
          '',
          '— Public access —',
          tunnel.copyBlock,
          '',
          '— DNS —',
          dns.copyBlock,
        ]
          .filter((line) => line !== undefined)
          .join('\n'),
        hint: scopesFailure
          ? cloudflareScopesFailureHint()
          : (tunnel.zeroTrustHint ?? undefined),
        severity: scopesFailure ? 'warning' : failed ? 'error' : 'info',
        scopesFailure,
      })
      await invalidateDomains()
    },
    onSettled: () => setBusyDomainId(null),
  })

  const copyPreview = async () => {
    if (!preview?.body) return
    await navigator.clipboard.writeText(preview.body)
    setCopyStatus('Copied')
    window.setTimeout(() => setCopyStatus(null), 2000)
  }

  const rows = domainsQuery.data ?? []

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6" sx={{ fontSize: 16, fontWeight: 650 }}>
        Domains
      </Typography>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        alignItems={{ sm: 'center' }}
        sx={{ width: '100%' }}
      >
        <TextField
          label="Hostname"
          size="small"
          value={hostname}
          onChange={(e) => setHostname(e.target.value)}
          sx={{ flex: 1, minWidth: { xs: '100%', sm: 220 } }}
          placeholder="app.example.com"
        />
        <TextField
          select
          label="Service"
          size="small"
          value={serviceId}
          onChange={(e) => setServiceId(e.target.value)}
          sx={{ flex: { sm: '0 1 220px' }, minWidth: { xs: '100%', sm: 180 } }}
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
      {publishMutation.isError && (
        <Alert severity="error" variant="outlined">
          Publish failed — check public access credentials, then retry or use Advanced in the row menu.
        </Alert>
      )}
      <QueryState isLoading={domainsQuery.isLoading} isError={domainsQuery.isError}>
        {rows.length === 0 ? (
          <EmptyState
            title="No domains"
            description="Register a hostname so Atlas can publish public access and track certificates."
          />
        ) : (
          <DataTableFrame>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Hostname</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Certificate</TableCell>
                  <TableCell
                    align="right"
                    sx={{
                      position: 'sticky',
                      right: 0,
                      bgcolor: 'background.paper',
                      zIndex: 2,
                      boxShadow: (t) =>
                        t.palette.mode === 'dark'
                          ? '-8px 0 12px -10px rgba(0,0,0,0.55)'
                          : '-8px 0 12px -10px rgba(15,23,42,0.18)',
                    }}
                  >
                    Actions
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((d) => {
                  const publishing = busyDomainId === d.id && publishMutation.isPending
                  return (
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
                      <TableCell
                        align="right"
                        sx={{
                          whiteSpace: 'nowrap',
                          // Stay visible while the table scrolls horizontally.
                          position: 'sticky',
                          right: 0,
                          bgcolor: 'background.paper',
                          zIndex: 1,
                          pl: 1.5,
                          boxShadow: (t) =>
                            t.palette.mode === 'dark'
                              ? '-8px 0 12px -10px rgba(0,0,0,0.55)'
                              : '-8px 0 12px -10px rgba(15,23,42,0.18)',
                        }}
                      >
                        <Stack
                          direction="row"
                          spacing={0.5}
                          justifyContent="flex-end"
                          alignItems="center"
                          flexWrap="nowrap"
                          sx={{ minWidth: 0 }}
                        >
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
                            variant="outlined"
                            disabled={publishing}
                            onClick={() => publishMutation.mutate(d.id)}
                          >
                            {publishing ? 'Publishing…' : 'Publish'}
                          </Button>
                          <RowOverflowMenu
                            aria-label={`More actions for ${d.hostname}`}
                            items={[
                              {
                                label: 'Copy ingress',
                                disabled: showIngress.isPending,
                                onClick: () => showIngress.mutate(d.id),
                              },
                              {
                                label: 'Copy public access',
                                disabled: showPublicAccess.isPending,
                                onClick: () => showPublicAccess.mutate(d.id),
                              },
                              {
                                label: 'Copy DNS',
                                disabled: showDns.isPending,
                                onClick: () => showDns.mutate(d.id),
                              },
                              {
                                label: 'Ensure public access only',
                                dividerBefore: true,
                                disabled: ensurePublicAccessOnly.isPending,
                                onClick: () => ensurePublicAccessOnly.mutate(d.id),
                              },
                              {
                                label: 'Ensure DNS only',
                                disabled: ensureDnsOnly.isPending,
                                onClick: () => ensureDnsOnly.mutate(d.id),
                              },
                              {
                                label: 'Remove',
                                dividerBefore: true,
                                destructive: true,
                                disabled: removeMutation.isPending,
                                onClick: () => removeMutation.mutate(d.id),
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
          </DataTableFrame>
        )}
      </QueryState>
      {preview && (
        <Alert
          severity={preview.severity ?? 'info'}
          variant="outlined"
          onClose={() => setPreview(null)}
        >
          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
            <Typography variant="subtitle2" sx={{ flex: 1 }}>
              {preview.title}
            </Typography>
            <Button size="small" onClick={() => void copyPreview()}>
              {copyStatus ?? 'Copy'}
            </Button>
          </Stack>
          {preview.hint && (
            <Typography variant="caption" display="block" sx={{ mb: 0.5, opacity: 0.85 }}>
              {preview.hint}
            </Typography>
          )}
          {preview.scopesFailure && (
            <Typography variant="body2" sx={{ mb: 0.5 }}>
              Open{' '}
              <Link component={RouterLink} to="/secrets">
                Org secrets
              </Link>{' '}
              (or Project secrets) to fix scopes on the Cloudflare token, then retry.
            </Typography>
          )}
          <Typography
            component="pre"
            variant="body2"
            sx={{ m: 0, whiteSpace: 'pre-wrap', fontFamily: 'ui-monospace, monospace' }}
          >
            {preview.body}
          </Typography>
        </Alert>
      )}
    </Stack>
  )
}
