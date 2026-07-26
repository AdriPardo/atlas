import type { ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'

interface PageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <Box
      display="flex"
      justifyContent="space-between"
      alignItems={{ xs: 'stretch', sm: 'flex-start' }}
      gap={2}
      flexDirection={{ xs: 'column', sm: 'row' }}
      mb={0.5}
    >
      <Box maxWidth={640}>
        <Typography variant="h4" component="h1" gutterBottom={Boolean(description)}>
          {title}
        </Typography>
        {description && (
          <Typography color="text.secondary" sx={{ mt: description ? 0.5 : 0 }}>
            {description}
          </Typography>
        )}
      </Box>
      {actions && (
        <Stack direction="row" spacing={1} alignItems="center" flexShrink={0}>
          {actions}
        </Stack>
      )}
    </Box>
  )
}
