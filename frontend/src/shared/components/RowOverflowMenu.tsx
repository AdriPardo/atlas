import { useId, useState, type MouseEvent, type ReactNode } from 'react'
import { Divider, IconButton, ListItemIcon, ListItemText, Menu, MenuItem } from '@mui/material'
import MoreVertIcon from '@mui/icons-material/MoreVert'

export type RowOverflowMenuItem = {
  id?: string
  label: string
  onClick: () => void
  disabled?: boolean
  destructive?: boolean
  dividerBefore?: boolean
  icon?: ReactNode
}

type RowOverflowMenuProps = {
  items: RowOverflowMenuItem[]
  'aria-label'?: string
  size?: 'small' | 'medium'
}

/** Compact ⋮ menu for secondary / advanced row actions (Autopilot: keep primary path clean). */
export function RowOverflowMenu({
  items,
  'aria-label': ariaLabel = 'More actions',
  size = 'small',
}: RowOverflowMenuProps) {
  const menuId = useId()
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const open = Boolean(anchorEl)

  if (items.length === 0) return null

  const handleOpen = (event: MouseEvent<HTMLElement>) => {
    event.stopPropagation()
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => setAnchorEl(null)

  return (
    <>
      <IconButton
        size={size}
        aria-label={ariaLabel}
        aria-controls={open ? menuId : undefined}
        aria-haspopup="true"
        aria-expanded={open ? 'true' : undefined}
        onClick={handleOpen}
      >
        <MoreVertIcon fontSize="small" />
      </IconButton>
      <Menu
        id={menuId}
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        onClick={(e) => e.stopPropagation()}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        marginThreshold={8}
        disableScrollLock
        // Portal to body so table overflow / sticky cells never clip the menu.
        disablePortal={false}
        slotProps={{
          paper: {
            sx: {
              minWidth: 220,
              maxWidth: 'min(320px, calc(100vw - 16px))',
            },
          },
        }}
      >
        {items.map((item, index) => {
          const key = item.id ?? `${item.label}-${index}`
          return [
            item.dividerBefore ? <Divider key={`${key}-div`} sx={{ my: 0.5 }} /> : null,
            <MenuItem
              key={key}
              disabled={item.disabled}
              onClick={() => {
                handleClose()
                item.onClick()
              }}
              sx={
                item.destructive
                  ? { color: 'error.main' }
                  : undefined
              }
            >
              {item.icon ? <ListItemIcon sx={{ color: 'inherit' }}>{item.icon}</ListItemIcon> : null}
              <ListItemText>{item.label}</ListItemText>
            </MenuItem>,
          ]
        })}
      </Menu>
    </>
  )
}
