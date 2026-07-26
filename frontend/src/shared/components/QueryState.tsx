import { Alert, Box, CircularProgress } from '@mui/material'
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
      <Box display="flex" justifyContent="center" py={6}>
        <CircularProgress />
      </Box>
    )
  }
  if (isError) {
    return <Alert severity="error">{errorMessage ?? 'Something went wrong'}</Alert>
  }
  return <>{children}</>
}
