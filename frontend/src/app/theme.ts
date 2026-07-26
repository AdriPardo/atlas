import { createTheme, type PaletteMode } from '@mui/material'

export function createAtlasTheme(mode: PaletteMode) {
  const isDark = mode === 'dark'
  return createTheme({
    palette: {
      mode,
      primary: {
        main: isDark ? '#2DD4BF' : '#0F766E',
        contrastText: isDark ? '#042F2E' : '#ECFDF5',
      },
      secondary: {
        main: isDark ? '#94A3B8' : '#334155',
      },
      background: {
        default: isDark ? '#0B1220' : '#F4F7F6',
        paper: isDark ? '#121A2A' : '#FFFFFF',
      },
      divider: isDark ? 'rgba(148,163,184,0.16)' : 'rgba(15,23,42,0.08)',
    },
    typography: {
      fontFamily: '"IBM Plex Sans", "Segoe UI", sans-serif',
      h4: { fontWeight: 700, letterSpacing: '-0.02em' },
      h5: { fontWeight: 650, letterSpacing: '-0.01em' },
      h6: { fontWeight: 650 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    shape: { borderRadius: 10 },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
      },
      MuiPaper: {
        defaultProps: { elevation: 0 },
        styleOverrides: {
          root: {
            border: `1px solid ${isDark ? 'rgba(148,163,184,0.16)' : 'rgba(15,23,42,0.08)'}`,
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            borderBottom: `1px solid ${isDark ? 'rgba(148,163,184,0.16)' : 'rgba(15,23,42,0.08)'}`,
          },
        },
      },
    },
  })
}
