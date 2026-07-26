import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './auth/AuthContext';
import { AppRoutes } from './routes/AppRoutes';
import { AppThemeProvider } from './theme/ThemeModeContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppThemeProvider>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </AppThemeProvider>
    </QueryClientProvider>
  );
}
