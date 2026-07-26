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
            px: { xs: 1.5, sm: 2 },
            py: 1.5,
            borderBottom: (t) => `1px solid ${t.palette.divider}`,
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            flexWrap: 'wrap',
          }}
        >
          {toolbar}
        </Box>
      )}
      <Box
        sx={{
          overflowX: 'auto',
          WebkitOverflowScrolling: 'touch',
          '& .MuiTable-root': { minWidth: 560 },
          '& .MuiTableCell-root': {
            whiteSpace: { xs: 'nowrap', md: 'normal' },
            py: { xs: 1.1, md: 1.25 },
          },
          '& .MuiTableCell-head': {
            py: 1.15,
          },
        }}
      >
        {children}
      </Box>
    </Box>
  )
}
