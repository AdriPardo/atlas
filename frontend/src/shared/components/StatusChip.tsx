import { Chip, type ChipProps } from '@mui/material'

type StatusTone = 'default' | 'success' | 'warning' | 'error' | 'info'

const STATUS_TONE: Record<string, StatusTone> = {
  REGISTERED: 'default',
  READY: 'info',
  DEPLOYING: 'warning',
  RUNNING: 'success',
  STOPPED: 'default',
  FAILED: 'error',
  PENDING: 'default',
  SUCCEEDED: 'success',
  CANCELLED: 'default',
  ONLINE: 'success',
  OFFLINE: 'default',
}

interface StatusChipProps {
  label: string
  size?: ChipProps['size']
}

export function StatusChip({ label, size = 'small' }: StatusChipProps) {
  const tone = STATUS_TONE[label.toUpperCase()] ?? 'default'
  return <Chip size={size} label={label} color={tone === 'default' ? 'default' : tone} variant="outlined" />
}
