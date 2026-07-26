import { Alert, Box, Button, Skeleton, Stack } from '@mui/material'
import type { ReactNode } from 'react'

interface QueryStateProps {
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  onRetry?: () => void
  skeleton?: 'page' | 'table' | 'detail'
  children: ReactNode
}

function LoadingSkeleton({ variant }: { variant: 'page' | 'table' | 'detail' }) {
  if (variant === 'table') {
    return (
      <Stack spacing={0} py={0.5}>
        <Skeleton variant="rectangular" height={40} sx={{ opacity: 0.55 }} />
        {[0, 1, 2, 3, 4].map((i) => (
          <Skeleton key={i} variant="rectangular" height={44} sx={{ opacity: 0.35 + i * 0.04 }} />
        ))}
      </Stack>
    )
  }
  if (variant === 'detail') {
    return (
      <Stack spacing={0} sx={{ border: (t) => `1px solid ${t.palette.divider}`, borderRadius: 2, overflow: 'hidden' }}>
        {[0, 1, 2, 3].map((i) => (
          <Box key={i} sx={{ px: 2.5, py: 1.75, borderBottom: (t) => (i < 3 ? `1px solid ${t.palette.divider}` : 'none') }}>
            <Skeleton width="28%" height={14} sx={{ mb: 1 }} />
            <Skeleton width="62%" height={18} />
          </Box>
        ))}
      </Stack>
    )
  }
  return (
    <Stack spacing={1.5} py={1}>
      <Skeleton variant="rounded" height={36} sx={{ borderRadius: 1 }} />
      <Skeleton variant="rounded" height={120} sx={{ borderRadius: 2 }} />
      <Box display="grid" gap={1}>
        <Skeleton variant="rounded" height={28} />
        <Skeleton variant="rounded" height={28} />
        <Skeleton variant="rounded" height={28} width="72%" />
      </Box>
    </Stack>
  )
}

export function QueryState({
  isLoading,
  isError,
  errorMessage,
  onRetry,
  skeleton = 'page',
  children,
}: QueryStateProps) {
  if (isLoading) {
    return <LoadingSkeleton variant={skeleton} />
  }
  if (isError) {
    return (
      <Alert
        severity="error"
        variant="outlined"
        action={
          onRetry ? (
            <Button color="inherit" size="small" onClick={onRetry}>
              Retry
            </Button>
          ) : undefined
        }
      >
        {errorMessage ?? 'Something went wrong. Check your connection and try again.'}
      </Alert>
    )
  }
  return <>{children}</>
}
