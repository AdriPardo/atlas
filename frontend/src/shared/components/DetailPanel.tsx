import type { ReactNode } from 'react'
import { Box, Divider, Stack, Typography } from '@mui/material'

interface DetailFieldProps {
  label: string
  children: ReactNode
  mono?: boolean
}

export function DetailField({ label, children, mono }: DetailFieldProps) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: '160px 1fr' },
        gap: { xs: 0.5, sm: 2 },
        py: 1.5,
        alignItems: 'baseline',
      }}
    >
      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 560 }}>
        {label}
      </Typography>
      <Typography
        variant="body2"
        component="div"
        className={mono ? 'atlas-mono' : undefined}
        sx={{ wordBreak: 'break-word' }}
      >
        {children}
      </Typography>
    </Box>
  )
}

interface DetailPanelProps {
  children: ReactNode
  title?: string
}

export function DetailPanel({ children, title }: DetailPanelProps) {
  return (
    <Box
      sx={{
        border: (t) => `1px solid ${t.palette.divider}`,
        borderRadius: 2,
        bgcolor: 'background.paper',
        overflow: 'hidden',
      }}
    >
      {title && (
        <>
          <Box px={2.5} py={1.75}>
            <Typography variant="subtitle2">{title}</Typography>
          </Box>
          <Divider />
        </>
      )}
      <Stack divider={<Divider />} px={2.5} py={0.5}>
        {children}
      </Stack>
    </Box>
  )
}
