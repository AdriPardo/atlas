import type { ReactNode } from 'react'
import { Box, Stack } from '@mui/material'

interface PageShellProps {
  children: ReactNode
  maxWidth?: number | string
}

export function PageShell({ children, maxWidth = 1120 }: PageShellProps) {
  return (
    <Box className="atlas-page" sx={{ maxWidth, mx: 'auto', width: '100%' }}>
      <Stack spacing={3}>{children}</Stack>
    </Box>
  )
}
