import { Box, Chip, type ChipProps } from '@mui/material'

type StatusTone = 'default' | 'success' | 'warning' | 'error' | 'info'

const STATUS_TONE: Record<string, StatusTone> = {
  REGISTERED: 'default',
  READY: 'info',
  DEPLOYING: 'warning',
  RUNNING: 'warning',
  STOPPED: 'default',
  FAILED: 'error',
  PENDING: 'info',
  SUCCEEDED: 'success',
  CANCELLED: 'default',
  ONLINE: 'success',
  OFFLINE: 'default',
  LOCAL: 'info',
  SSH: 'default',
  OK: 'success',
  FIRING: 'error',
  SILENCED: 'default',
  ACTIVE: 'success',
  PENDING_DNS: 'info',
  ERROR: 'error',
}

const LIVE_STATUSES = new Set(['DEPLOYING', 'RUNNING', 'PENDING'])

interface StatusChipProps {
  label: string
  size?: ChipProps['size']
}

export function StatusChip({ label, size = 'small' }: StatusChipProps) {
  const key = label.toUpperCase()
  const tone = STATUS_TONE[key] ?? 'default'
  const live = LIVE_STATUSES.has(key)

  return (
    <Chip
      size={size}
      label={
        live ? (
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75 }}>
            <Box
              component="span"
              className="atlas-status-pulse"
              sx={{
                width: 6,
                height: 6,
                borderRadius: '50%',
                bgcolor: 'currentColor',
                flexShrink: 0,
              }}
            />
            {label}
          </Box>
        ) : (
          label
        )
      }
      color={tone === 'default' ? 'default' : tone}
      variant="outlined"
    />
  )
}
