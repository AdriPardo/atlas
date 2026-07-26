import type { ReactNode } from 'react'
import { Box, Button, Stack, Typography } from '@mui/material'

interface EmptyStateProps {
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
  secondaryLabel?: string
  onSecondary?: () => void
  icon?: ReactNode
}

export function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
  secondaryLabel,
  onSecondary,
  icon,
}: EmptyStateProps) {
  return (
    <Box
      sx={{
        py: { xs: 5, md: 6.5 },
        px: 3,
        textAlign: 'center',
        border: (t) => `1px dashed ${t.palette.divider}`,
        borderRadius: 2,
        bgcolor: (t) => (t.palette.mode === 'dark' ? 'rgba(255,255,255,0.015)' : 'rgba(15,23,42,0.015)'),
      }}
    >
      <Stack spacing={1.5} alignItems="center" maxWidth={440} mx="auto">
        {icon && (
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: 1.5,
              display: 'grid',
              placeItems: 'center',
              color: 'primary.main',
              bgcolor: (t) =>
                t.palette.mode === 'dark' ? 'rgba(45,212,191,0.1)' : 'rgba(15,118,110,0.08)',
              '& svg': { fontSize: 26 },
            }}
          >
            {icon}
          </Box>
        )}
        <Typography variant="h6">{title}</Typography>
        <Typography color="text.secondary" variant="body2" sx={{ maxWidth: 36 * 8 }}>
          {description}
        </Typography>
        {(actionLabel && onAction) || (secondaryLabel && onSecondary) ? (
          <Stack direction="row" spacing={1} flexWrap="wrap" justifyContent="center" sx={{ mt: 0.5 }}>
            {actionLabel && onAction && (
              <Button variant="contained" onClick={onAction}>
                {actionLabel}
              </Button>
            )}
            {secondaryLabel && onSecondary && (
              <Button variant="outlined" onClick={onSecondary}>
                {secondaryLabel}
              </Button>
            )}
          </Stack>
        ) : null}
      </Stack>
    </Box>
  )
}
