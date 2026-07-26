import type { ReactNode } from 'react'
import { Box } from '@mui/material'

interface DataTableFrameProps {
  children: ReactNode
  toolbar?: ReactNode
}

export function DataTableFrame({ children, toolbar }: DataTableFrameProps) {
  return (
    <Box
      sx={{
        border: (t) => `1px solid ${t.palette.divider}`,
        borderRadius: 2,
        bgcolor: 'background.paper',
        overflow: 'hidden',
      }}
    >
      {toolbar && (
        <Box
          sx={{
            px: 2,
            py: 1.5,
            borderBottom: (t) => `1px solid ${t.palette.divider}`,
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            flexWrap: 'wrap',
          }}
        >
          {toolbar}
        </Box>
      )}
      <Box sx={{ overflowX: 'auto' }}>{children}</Box>
    </Box>
  )
}
