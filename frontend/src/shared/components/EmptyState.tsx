import type { ReactNode } from 'react'
import { Box, Button, Stack, Typography } from '@mui/material'

interface EmptyStateProps {
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
  icon?: ReactNode
}

export function EmptyState({ title, description, actionLabel, onAction, icon }: EmptyStateProps) {
  return (
    <Box
      sx={{
        py: { xs: 5, md: 7 },
        px: 3,
        textAlign: 'center',
        border: (t) => `1px dashed ${t.palette.divider}`,
        borderRadius: 2,
        bgcolor: (t) => (t.palette.mode === 'dark' ? 'rgba(255,255,255,0.015)' : 'rgba(15,23,42,0.015)'),
      }}
    >
      <Stack spacing={1.5} alignItems="center" maxWidth={420} mx="auto">
        {icon && (
          <Box
            sx={{
              color: 'primary.main',
              opacity: 0.85,
              display: 'flex',
              '& svg': { fontSize: 32 },
            }}
          >
            {icon}
          </Box>
        )}
        <Typography variant="h6">{title}</Typography>
        <Typography color="text.secondary" variant="body2">
          {description}
        </Typography>
        {actionLabel && onAction && (
          <Button variant="contained" onClick={onAction} sx={{ mt: 1 }}>
            {actionLabel}
          </Button>
        )}
      </Stack>
    </Box>
  )
}
