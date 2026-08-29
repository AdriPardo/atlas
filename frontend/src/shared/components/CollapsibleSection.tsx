import { useState, type ReactNode } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Typography,
} from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'

interface CollapsibleSectionProps {
  title: string
  summary?: string
  defaultExpanded?: boolean
  children: ReactNode
}

/** Collapsed advanced panel — reduces scroll on dense detail pages. */
export function CollapsibleSection({
  title,
  summary,
  defaultExpanded = false,
  children,
}: CollapsibleSectionProps) {
  const [expanded, setExpanded] = useState(defaultExpanded)

  return (
    <Accordion
      expanded={expanded}
      onChange={(_, next) => setExpanded(next)}
      disableGutters
      elevation={0}
      sx={{
        border: (t) => `1px solid ${t.palette.divider}`,
        borderRadius: '8px !important',
        '&:before': { display: 'none' },
        overflow: 'hidden',
      }}
    >
      <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ px: 2.5, py: 0.5 }}>
        <div>
          <Typography variant="subtitle2" sx={{ fontWeight: 650 }}>
            {title}
          </Typography>
          {summary && (
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.25 }}>
              {summary}
            </Typography>
          )}
        </div>
      </AccordionSummary>
      <AccordionDetails sx={{ px: 2.5, pt: 0, pb: 2.5 }}>{children}</AccordionDetails>
    </Accordion>
  )
}
