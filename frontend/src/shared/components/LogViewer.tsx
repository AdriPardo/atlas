import { useEffect, useRef } from 'react'
import { Box, Typography } from '@mui/material'

interface LogViewerProps {
  logs: string
  live?: boolean
  maxHeight?: number
}

export function LogViewer({ logs, live = false, maxHeight = 480 }: LogViewerProps) {
  const scrollerRef = useRef<HTMLPreElement>(null)
  const content = logs.trim() ? logs : live ? 'Waiting for worker output…' : 'No logs recorded.'

  useEffect(() => {
    if (!live || !scrollerRef.current) return
    scrollerRef.current.scrollTop = scrollerRef.current.scrollHeight
  }, [logs, live])

  return (
    <Box
      sx={{
        border: (t) => `1px solid ${t.palette.divider}`,
        borderRadius: 2,
        bgcolor: 'background.paper',
        overflow: 'hidden',
      }}
    >
      <Box
        sx={{
          px: 2,
          py: 1.25,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 1,
          borderBottom: (t) => `1px solid ${t.palette.divider}`,
          bgcolor: (t) => (t.palette.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(15,23,42,0.02)'),
        }}
      >
        <Typography variant="subtitle2">Logs</Typography>
        {live && (
          <Typography
            variant="caption"
            color="warning.main"
            sx={{ fontWeight: 650, display: 'inline-flex', alignItems: 'center', gap: 0.75 }}
          >
            <Box
              component="span"
              className="atlas-status-pulse"
              sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: 'currentColor' }}
            />
            Live
          </Typography>
        )}
      </Box>
      <Box
        component="pre"
        ref={scrollerRef}
        className="atlas-mono"
        sx={{
          m: 0,
          p: 2,
          bgcolor: (t) => (t.palette.mode === 'dark' ? '#070B12' : '#0F172A'),
          color: (t) => (t.palette.mode === 'dark' ? '#CBD5E1' : '#E2E8F0'),
          fontSize: 12.5,
          lineHeight: 1.55,
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          maxHeight,
          overflow: 'auto',
          tabSize: 2,
        }}
      >
        {content}
      </Box>
    </Box>
  )
}
