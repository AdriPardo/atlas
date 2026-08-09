import type { ReactNode } from 'react'
import { Box, Stack } from '@mui/material'

interface PageShellProps {
  children: ReactNode
  /** Optional cap for dense forms; omit for full-width ops pages. */
  maxWidth?: number | string
}

export function PageShell({ children, maxWidth }: PageShellProps) {
  return (
    <Box
      className="atlas-page"
      sx={{
        width: '100%',
        minWidth: 0,
        maxWidth: maxWidth ?? '100%',
        mx: maxWidth ? 'auto' : 0,
      }}
    >
      <Stack spacing={3}>{children}</Stack>
    </Box>
  )
}
