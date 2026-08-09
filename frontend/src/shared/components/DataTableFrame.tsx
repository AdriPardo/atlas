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
        // Clip radius only — scroll lives on the inner scroller so sticky
        // action cells and portaled menus are not trapped by overflow:hidden.
        overflow: 'clip',
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
          '& .MuiTable-root': { minWidth: 560, width: '100%' },
          '& .MuiTableCell-root': {
            py: { xs: 1.1, md: 1.25 },
          },
          '& .MuiTableCell-head': {
            py: 1.15,
            whiteSpace: 'nowrap',
          },
          // Keep primary row text from crushing on narrow viewports; do NOT
          // force whiteSpace:normal on md — that overrode Actions nowrap and
          // wrapped/cut off Publish + ⋮ buttons.
          '& .MuiTableCell-body': {
            whiteSpace: { xs: 'nowrap', md: 'normal' },
          },
          '& .MuiTableCell-body[align="right"], & .MuiTableCell-head[align="right"]': {
            whiteSpace: 'nowrap',
          },
        }}
      >
        {children}
      </Box>
    </Box>
  )
}
