import { createTheme, type PaletteMode, type Theme } from '@mui/material/styles';

export function createAppTheme(mode: PaletteMode): Theme {
  return createTheme({
    palette: {
      mode,
      primary: {
        main: mode === 'light' ? '#1565c0' : '#90caf9',
      },
      secondary: {
        main: mode === 'light' ? '#00838f' : '#4dd0e1',
      },
      background: {
        default: mode === 'light' ? '#f4f6f8' : '#0f1419',
        paper: mode === 'light' ? '#ffffff' : '#1a2332',
      },
    },
    typography: {
      fontFamily: '"IBM Plex Sans", "Segoe UI", sans-serif',
      h4: { fontWeight: 600 },
      h5: { fontWeight: 600 },
      h6: { fontWeight: 600 },
    },
    shape: {
      borderRadius: 10,
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            fontWeight: 600,
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            boxShadow:
              mode === 'light'
                ? '0 1px 3px rgba(16, 24, 40, 0.08)'
                : '0 1px 3px rgba(0, 0, 0, 0.4)',
          },
        },
      },
    },
  });
}
