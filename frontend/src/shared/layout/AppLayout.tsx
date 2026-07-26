import { useState } from 'react'
import { Link as RouterLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import AppsOutlinedIcon from '@mui/icons-material/AppsOutlined'
import DnsOutlinedIcon from '@mui/icons-material/DnsOutlined'
import RocketLaunchOutlinedIcon from '@mui/icons-material/RocketLaunchOutlined'
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined'
import { useAuth } from '../../features/auth/AuthContext'

const drawerWidth = 236

const navItems = [
  { to: '/', label: 'Dashboard', icon: <DashboardOutlinedIcon fontSize="small" /> },
  { to: '/applications', label: 'Applications', icon: <AppsOutlinedIcon fontSize="small" /> },
  { to: '/hosts', label: 'Hosts', icon: <DnsOutlinedIcon fontSize="small" /> },
  { to: '/deployments', label: 'Deployments', icon: <RocketLaunchOutlinedIcon fontSize="small" /> },
  { to: '/profile', label: 'Profile', icon: <PersonOutlinedIcon fontSize="small" /> },
]

interface AppLayoutProps {
  mode: 'light' | 'dark'
  onToggleMode: () => void
}

function pageTitle(pathname: string): string {
  if (pathname.startsWith('/applications')) return 'Applications'
  if (pathname.startsWith('/hosts')) return 'Hosts'
  if (pathname.startsWith('/deployments')) return 'Deployments'
  if (pathname.startsWith('/profile')) return 'Profile'
  return 'Dashboard'
}

export function AppLayout({ mode, onToggleMode }: AppLayoutProps) {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('md'))
  const [mobileOpen, setMobileOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { logout, user } = useAuth()

  const drawer = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Toolbar sx={{ px: 2.25, minHeight: 64, gap: 1.25 }}>
        <Box
          sx={{
            width: 28,
            height: 28,
            borderRadius: 1.25,
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            display: 'grid',
            placeItems: 'center',
            fontWeight: 700,
            fontSize: 13,
            letterSpacing: '-0.04em',
            flexShrink: 0,
          }}
        >
          A
        </Box>
        <Box>
          <Typography
            sx={{
              fontWeight: 700,
              letterSpacing: '-0.03em',
              lineHeight: 1.1,
              color: 'text.primary',
            }}
          >
            Atlas
          </Typography>
          <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1 }}>
            Ops console
          </Typography>
        </Box>
      </Toolbar>

      <List sx={{ px: 1.25, pt: 1, flex: 1 }}>
        {navItems.map((item) => {
          const selected =
            item.to === '/'
              ? location.pathname === '/'
              : location.pathname.startsWith(item.to)
          return (
            <ListItemButton
              key={item.to}
              component={RouterLink}
              to={item.to}
              selected={selected}
              onClick={() => setMobileOpen(false)}
              sx={{
                borderRadius: 1.5,
                mb: 0.35,
                py: 0.9,
                position: 'relative',
                '&::before': selected
                  ? {
                      content: '""',
                      position: 'absolute',
                      left: 0,
                      top: 10,
                      bottom: 10,
                      width: 3,
                      borderRadius: 2,
                      bgcolor: 'primary.main',
                    }
                  : undefined,
              }}
            >
              <ListItemIcon sx={{ minWidth: 36, color: selected ? 'inherit' : 'text.secondary' }}>
                {item.icon}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{
                  fontSize: 14,
                  fontWeight: selected ? 650 : 500,
                }}
              />
            </ListItemButton>
          )
        })}
      </List>

      <Box
        sx={{
          mx: 1.5,
          mb: 2,
          px: 1.5,
          py: 1.25,
          borderRadius: 1.5,
          border: (t) => `1px solid ${t.palette.divider}`,
          bgcolor: (t) =>
            t.palette.mode === 'dark' ? 'rgba(255,255,255,0.03)' : 'rgba(255,255,255,0.55)',
        }}
      >
        <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.2 }}>
          {user?.username}
        </Typography>
        <Typography variant="caption" color="text.secondary" className="atlas-mono">
          {user?.role}
        </Typography>
      </Box>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100dvh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar sx={{ minHeight: 64, gap: 1 }}>
          {isMobile && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} aria-label="Open navigation">
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="subtitle1" sx={{ flexGrow: 1, fontWeight: 650, letterSpacing: '-0.02em' }}>
            {pageTitle(location.pathname)}
          </Typography>
          <Tooltip title={mode === 'dark' ? 'Light mode' : 'Dark mode'}>
            <IconButton onClick={onToggleMode} aria-label="Toggle theme">
              {mode === 'dark' ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
            </IconButton>
          </Tooltip>
          <Tooltip title="Sign out">
            <IconButton
              onClick={() => {
                logout()
                navigate('/login')
              }}
              aria-label="Logout"
            >
              <LogoutOutlinedIcon />
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant={isMobile ? 'temporary' : 'permanent'}
          open={isMobile ? mobileOpen : true}
          onClose={() => setMobileOpen(false)}
          ModalProps={{ keepMounted: true }}
          sx={{
            '& .MuiDrawer-paper': {
              width: drawerWidth,
              boxSizing: 'border-box',
            },
          }}
        >
          {drawer}
        </Drawer>
      </Box>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          px: { xs: 2, md: 3.5 },
          py: { xs: 2.5, md: 3.5 },
          mt: 8,
          minWidth: 0,
        }}
      >
        <Outlet />
      </Box>
    </Box>
  )
}
