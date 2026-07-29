import type { ReactNode } from 'react'
import { Alert, Box, Typography } from '@mui/material'
import { CLOUDFLARE_API_TOKEN_SECRET, CLOUDFLARE_TOKEN_SCOPES } from './knownSecretHints'

type Props = {
  /** Extra sentence after the scopes line (e.g. resolution order). */
  footer?: ReactNode
}

/**
 * Always-visible operator hint: Autopilot Tunnel/DNS need these Cloudflare scopes
 * on the shared secret name {@link CLOUDFLARE_API_TOKEN_SECRET}.
 */
export function CloudflareTokenScopesHint({ footer }: Props) {
  return (
    <Alert severity="info" variant="outlined">
      <Typography variant="body2" component="div">
        For PUBLIC Autopilot (Tunnel + DNS), create{' '}
        <Box component="code" className="atlas-mono" sx={{ fontSize: '0.9em' }}>
          {CLOUDFLARE_API_TOKEN_SECRET}
        </Box>{' '}
        with scopes: {CLOUDFLARE_TOKEN_SCOPES}. One token covering both is enough.
      </Typography>
      {footer ? (
        <Typography variant="body2" component="div" sx={{ mt: 1 }}>
          {footer}
        </Typography>
      ) : null}
    </Alert>
  )
}
