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

const drawerWidth = 248

const navItems = [
  { to: '/', label: 'Dashboard', icon: <DashboardOutlinedIcon /> },
  { to: '/applications', label: 'Applications', icon: <AppsOutlinedIcon /> },
  { to: '/hosts', label: 'Hosts', icon: <DnsOutlinedIcon /> },
  { to: '/deployments', label: 'Deployments', icon: <RocketLaunchOutlinedIcon /> },
  { to: '/profile', label: 'Profile', icon: <PersonOutlinedIcon /> },
]

interface AppLayoutProps {
  mode: 'light' | 'dark'
  onToggleMode: () => void
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
      <Toolbar sx={{ px: 2.5 }}>
        <Typography variant="h6" color="primary" sx={{ fontWeight: 700 }}>
          Atlas
        </Typography>
      </Toolbar>
      <List sx={{ px: 1.5, flex: 1 }}>
        {navItems.map((item) => (
          <ListItemButton
            key={item.to}
            component={RouterLink}
            to={item.to}
            selected={
              item.to === '/'
                ? location.pathname === '/'
                : location.pathname.startsWith(item.to)
            }
            onClick={() => setMobileOpen(false)}
            sx={{ borderRadius: 2, mb: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Box sx={{ px: 2, pb: 2 }}>
        <Typography variant="caption" color="text.secondary">
          {user?.username} · {user?.role}
        </Typography>
      </Box>
    </Box>
  )

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
        }}
      >
        <Toolbar>
          {isMobile && (
            <IconButton edge="start" onClick={() => setMobileOpen(true)} sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="subtitle1" sx={{ flexGrow: 1, fontWeight: 600 }}>
            Operations console
          </Typography>
          <IconButton onClick={onToggleMode} aria-label="Toggle theme">
            {mode === 'dark' ? <LightModeOutlinedIcon /> : <DarkModeOutlinedIcon />}
          </IconButton>
          <IconButton
            onClick={() => {
              logout()
              navigate('/login')
            }}
            aria-label="Logout"
          >
            <LogoutOutlinedIcon />
          </IconButton>
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
              borderRight: (t) => `1px solid ${t.palette.divider}`,
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
          p: { xs: 2, md: 3 },
          mt: 8,
        }}
      >
        <Outlet />
      </Box>
    </Box>
  )
}
