import { useMemo, useState } from 'react'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from '../features/auth/AuthContext'
import { queryClient } from './queryClient'
import { createAtlasTheme } from './theme'
import { AppRouter } from './AppRouter'

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
