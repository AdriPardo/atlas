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
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import DnsOutlinedIcon from '@mui/icons-material/DnsOutlined'
import RocketLaunchOutlinedIcon from '@mui/icons-material/RocketLaunchOutlined'
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import PolicyOutlinedIcon from '@mui/icons-material/PolicyOutlined'
import NotificationsOutlinedIcon from '@mui/icons-material/NotificationsOutlined'
import VpnKeyOutlinedIcon from '@mui/icons-material/VpnKeyOutlined'
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined'
import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined'
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined'
import LogoutOutlinedIcon from '@mui/icons-material/LogoutOutlined'
import { useAuth } from '../../features/auth/AuthContext'

const drawerWidth = 244

const navGroups = [
  {
    label: 'Operate',
    items: [
      { to: '/', label: 'Dashboard', icon: <DashboardOutlinedIcon fontSize="small" /> },
      { to: '/projects', label: 'Projects', icon: <FolderOutlinedIcon fontSize="small" /> },
      { to: '/hosts', label: 'Hosts · adv.', icon: <DnsOutlinedIcon fontSize="small" /> },
      { to: '/deployments', label: 'Deployments', icon: <RocketLaunchOutlinedIcon fontSize="small" /> },
      { to: '/pipelines', label: 'Pipelines', icon: <AccountTreeOutlinedIcon fontSize="small" /> },
      { to: '/audit', label: 'Audit', icon: <PolicyOutlinedIcon fontSize="small" /> },
      { to: '/alerts', label: 'Alerts', icon: <NotificationsOutlinedIcon fontSize="small" /> },
      { to: '/secrets', label: 'Secrets', icon: <VpnKeyOutlinedIcon fontSize="small" /> },
    ],
  },
  {
    label: 'Account',
    items: [{ to: '/profile', label: 'Profile', icon: <PersonOutlinedIcon fontSize="small" /> }],
  },
]

interface AppLayoutProps {
  mode: 'light' | 'dark'
  onToggleMode: () => void
}

function pageTitle(pathname: string): string {
  if (pathname.startsWith('/projects') || pathname.startsWith('/applications')) return 'Projects'
  if (pathname.startsWith('/hosts')) return 'Hosts'
  if (pathname.startsWith('/deployments')) return 'Deployments'
  if (pathname.startsWith('/pipelines')) return 'Pipelines'
  if (pathname.startsWith('/audit')) return 'Audit'
  if (pathname.startsWith('/alerts')) return 'Alerts'
  if (pathname.startsWith('/secrets')) return 'Secrets'
  if (pathname.startsWith('/profile')) return 'Profile'
  return 'Dashboard'
}

function pageSubtitle(pathname: string): string {
  if (pathname === '/') return 'Inventory & activity'
  if (pathname === '/projects/new') return 'Create'
  if (pathname.match(/^\/projects\/[^/]+\/edit$/)) return 'Edit'
  if (pathname.match(/^\/projects\/[^/]+$/)) return 'Detail'
  if (pathname.startsWith('/projects')) return 'Services & repos'
  if (pathname === '/hosts/new') return 'Create'
  if (pathname.match(/^\/hosts\/[^/]+\/edit$/)) return 'Edit'
  if (pathname.match(/^\/hosts\/[^/]+$/)) return 'Detail'
  if (pathname.startsWith('/hosts')) return 'Servers'
  if (pathname === '/deployments/new') return 'Create'
  if (pathname.match(/^\/deployments\/[^/]+$/)) return 'Detail'
  if (pathname.startsWith('/deployments')) return 'Release history'
  if (pathname === '/pipelines/new') return 'Create'
  if (pathname.match(/^\/pipelines\/[^/]+$/)) return 'Detail'
  if (pathname.startsWith('/pipelines')) return 'Deploy automation'
  if (pathname.startsWith('/audit')) return 'Security trail'
  if (pathname.startsWith('/alerts')) return 'Rules & channels'
  if (pathname.startsWith('/secrets')) return 'Encrypted credentials'
  if (pathname.startsWith('/profile')) return 'Signed-in user'
  return ''
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

      <Box sx={{ px: 1.25, pt: 0.5, flex: 1, overflowY: 'auto' }}>
        {navGroups.map((group) => (
          <Box key={group.label} sx={{ mb: 1.75 }}>
            <Typography
              variant="overline"
              color="text.secondary"
              sx={{ px: 1.5, display: 'block', mb: 0.5, opacity: 0.85 }}
            >
              {group.label}
            </Typography>
            <List disablePadding>
              {group.items.map((item) => {
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
          </Box>
        ))}
      </Box>

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
        <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.2 }} noWrap>
          {user?.username}
        </Typography>
        <Typography variant="caption" color="text.secondary" className="atlas-mono">
          {user?.role}
        </Typography>
      </Box>
    </Box>
  )

  const subtitle = pageSubtitle(location.pathname)

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
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            <Typography
              variant="subtitle1"
              sx={{ fontWeight: 650, letterSpacing: '-0.02em', lineHeight: 1.2 }}
              noWrap
            >
              {pageTitle(location.pathname)}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.2 }}>
                {subtitle}
              </Typography>
            )}
          </Box>
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
