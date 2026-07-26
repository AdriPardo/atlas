import { createTheme, type PaletteMode } from '@mui/material'
import { alpha } from '@mui/material/styles'

const easeOut = 'cubic-bezier(0.23, 1, 0.32, 1)'

export function createAtlasTheme(mode: PaletteMode) {
  const isDark = mode === 'dark'
  const border = isDark ? 'rgba(148,163,184,0.14)' : 'rgba(15,23,42,0.09)'
  const ink = isDark ? '#E8EEF5' : '#0F172A'
  const muted = isDark ? '#94A3B8' : '#64748B'
  const surface = isDark ? '#0B1220' : '#F1F5F4'
  const paper = isDark ? '#121A27' : '#FFFFFF'
  const teal = isDark ? '#2DD4BF' : '#0F766E'
  const tealSoft = isDark ? '#14B8A6' : '#0D9488'

  return createTheme({
    cssVariables: true,
    palette: {
      mode,
      primary: {
        main: teal,
        light: isDark ? '#5EEAD4' : '#14B8A6',
        dark: isDark ? '#0F766E' : '#115E59',
        contrastText: isDark ? '#042F2E' : '#ECFDF5',
      },
      secondary: {
        main: isDark ? '#94A3B8' : '#334155',
      },
      success: {
        main: isDark ? '#34D399' : '#059669',
      },
      error: {
        main: isDark ? '#F87171' : '#DC2626',
      },
      warning: {
        main: isDark ? '#FBBF24' : '#D97706',
      },
      info: {
        main: isDark ? '#38BDF8' : '#0284C7',
      },
      text: {
        primary: ink,
        secondary: muted,
      },
      background: {
        default: surface,
        paper,
      },
      divider: border,
    },
    typography: {
      fontFamily: '"IBM Plex Sans", "Segoe UI", sans-serif',
      fontSize: 14,
      h3: { fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1.15 },
      h4: { fontWeight: 700, letterSpacing: '-0.025em', lineHeight: 1.2, fontSize: '1.75rem' },
      h5: { fontWeight: 650, letterSpacing: '-0.015em', lineHeight: 1.25, fontSize: '1.25rem' },
      h6: { fontWeight: 650, letterSpacing: '-0.01em', fontSize: '1.05rem' },
      subtitle1: { fontWeight: 600, letterSpacing: '-0.01em' },
      subtitle2: { fontWeight: 600, letterSpacing: '-0.005em' },
      body1: { lineHeight: 1.55 },
      body2: { lineHeight: 1.5, fontSize: '0.875rem' },
      overline: {
        fontWeight: 600,
        letterSpacing: '0.08em',
        fontSize: '0.7rem',
        lineHeight: 1.4,
      },
      button: { textTransform: 'none', fontWeight: 600, letterSpacing: '-0.01em' },
      caption: { letterSpacing: '0.01em' },
    },
    shape: { borderRadius: 8 },
    transitions: {
      easing: {
        easeOut,
        sharp: easeOut,
      },
      duration: {
        shortest: 120,
        shorter: 160,
        short: 200,
        standard: 220,
        complex: 280,
        enteringScreen: 220,
        leavingScreen: 180,
      },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          ':root': {
            '--atlas-border': border,
            '--atlas-surface': surface,
            '--atlas-paper': paper,
            '--atlas-ink': ink,
            '--atlas-muted': muted,
            '--atlas-teal': teal,
            '--atlas-teal-soft': tealSoft,
            '--atlas-ease-out': easeOut,
            '--atlas-rail': isDark ? '#0A101A' : '#E8EEEC',
            '--atlas-glow': isDark ? 'rgba(45,212,191,0.14)' : 'rgba(15,118,110,0.12)',
          },
          body: {
            backgroundImage: isDark
              ? `radial-gradient(900px 420px at 0% -5%, rgba(45,212,191,0.08), transparent 55%),
                 radial-gradient(700px 360px at 100% 0%, rgba(56,189,248,0.05), transparent 50%),
                 linear-gradient(180deg, #0B1220 0%, #0B1220 100%)`
              : `radial-gradient(900px 420px at 0% -5%, rgba(15,118,110,0.07), transparent 55%),
                 radial-gradient(700px 360px at 100% 0%, rgba(14,165,233,0.04), transparent 50%),
                 linear-gradient(180deg, #F1F5F4 0%, #F1F5F4 100%)`,
            backgroundAttachment: 'fixed',
          },
          '@media (prefers-reduced-motion: reduce)': {
            '*, *::before, *::after': {
              animationDuration: '0.01ms !important',
              animationIterationCount: '1 !important',
              transitionDuration: '0.01ms !important',
            },
          },
        },
      },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: {
            borderRadius: 8,
            paddingInline: 16,
            transition: `transform 140ms ${easeOut}, background-color 160ms ${easeOut}, box-shadow 160ms ${easeOut}, border-color 160ms ${easeOut}`,
            '&:active': { transform: 'scale(0.98)' },
          },
          containedPrimary: {
            boxShadow: isDark
              ? `0 1px 0 ${alpha('#FFF', 0.08)} inset, 0 8px 20px ${alpha(teal, 0.18)}`
              : `0 1px 0 ${alpha('#FFF', 0.35)} inset, 0 6px 16px ${alpha(teal, 0.16)}`,
            '&:hover': {
              boxShadow: isDark
                ? `0 1px 0 ${alpha('#FFF', 0.1)} inset, 0 10px 24px ${alpha(teal, 0.24)}`
                : `0 1px 0 ${alpha('#FFF', 0.4)} inset, 0 8px 20px ${alpha(teal, 0.22)}`,
            },
          },
          sizeLarge: {
            minHeight: 44,
            fontSize: '0.95rem',
          },
        },
      },
      MuiIconButton: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            transition: `background-color 160ms ${easeOut}, transform 140ms ${easeOut}`,
            '&:active': { transform: 'scale(0.96)' },
          },
        },
      },
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            border: `1px solid ${border}`,
            backgroundColor: paper,
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            backgroundColor: isDark ? alpha('#121A27', 0.88) : alpha('#FFFFFF', 0.88),
            backdropFilter: 'blur(12px)',
            borderBottom: `1px solid ${border}`,
            boxShadow: 'none',
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundImage: 'none',
            backgroundColor: isDark ? '#0A101A' : '#E8EEEC',
            borderRight: `1px solid ${border}`,
          },
        },
      },
      MuiListItemButton: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            transition: `background-color 160ms ${easeOut}, color 160ms ${easeOut}`,
            '&.Mui-selected': {
              backgroundColor: isDark ? alpha(teal, 0.14) : alpha(teal, 0.1),
              color: teal,
              '& .MuiListItemIcon-root': { color: teal },
              '&:hover': {
                backgroundColor: isDark ? alpha(teal, 0.2) : alpha(teal, 0.14),
              },
            },
          },
        },
      },
      MuiTextField: {
        defaultProps: { size: 'small' },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            backgroundColor: isDark ? alpha('#FFF', 0.02) : alpha('#FFF', 0.7),
            transition: `box-shadow 160ms ${easeOut}, border-color 160ms ${easeOut}`,
            '&.Mui-focused': {
              boxShadow: `0 0 0 3px ${alpha(teal, isDark ? 0.22 : 0.16)}`,
            },
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          head: {
            fontWeight: 600,
            color: muted,
            fontSize: '0.75rem',
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
            borderBottom: `1px solid ${border}`,
            backgroundColor: isDark ? alpha('#FFF', 0.02) : alpha('#0F172A', 0.02),
          },
          root: {
            borderBottom: `1px solid ${border}`,
          },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          root: {
            transition: `background-color 140ms ${easeOut}`,
            '&:last-child td': { borderBottom: 0 },
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: 6,
            fontWeight: 600,
            letterSpacing: '0.01em',
          },
          sizeSmall: {
            height: 24,
            fontSize: '0.72rem',
          },
        },
      },
      MuiAlert: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            border: `1px solid ${border}`,
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            borderRadius: 12,
          },
        },
      },
      MuiLink: {
        styleOverrides: {
          root: {
            fontWeight: 560,
            transition: `color 140ms ${easeOut}`,
          },
        },
      },
    },
  })
}
