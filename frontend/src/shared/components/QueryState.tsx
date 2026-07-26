import { Alert, Box, Skeleton, Stack } from '@mui/material'
import type { ReactNode } from 'react'

interface QueryStateProps {
  isLoading: boolean
  isError: boolean
  errorMessage?: string
  children: ReactNode
}

export function QueryState({ isLoading, isError, errorMessage, children }: QueryStateProps) {
  if (isLoading) {
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
  if (isError) {
    return (
      <Alert severity="error" variant="outlined">
        {errorMessage ?? 'Something went wrong'}
      </Alert>
    )
  }
  return <>{children}</>
}
