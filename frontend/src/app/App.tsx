import { useMemo, useState } from 'react'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../features/auth/AuthContext'
import { isTransientApiError } from '../shared/api/queryErrors'
import { createAtlasTheme } from './theme'
import { AppRouter } from './AppRouter'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Edge/proxy 502s and brief backend restarts — same class of flake SSO already retries.
      retry: (failureCount, error) => failureCount < 3 && isTransientApiError(error),
      retryDelay: (attempt) => Math.min(500 * 2 ** attempt, 4000),
      refetchOnWindowFocus: false,
    },
  },
})

const THEME_KEY = 'atlas.theme'

export function App() {
  const [mode, setMode] = useState<'light' | 'dark'>(() => {
    const stored = localStorage.getItem(THEME_KEY)
    return stored === 'light' || stored === 'dark' ? stored : 'dark'
  })

  const theme = useMemo(() => createAtlasTheme(mode), [mode])

  const toggleMode = () => {
    setMode((current) => {
      const next = current === 'dark' ? 'light' : 'dark'
      localStorage.setItem(THEME_KEY, next)
      return next
    })
  }

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <AuthProvider>
          <AppRouter mode={mode} onToggleMode={toggleMode} />
        </AuthProvider>
      </ThemeProvider>
    </QueryClientProvider>
  )
}
