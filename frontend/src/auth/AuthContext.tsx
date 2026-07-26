import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { login as loginRequest } from '../api/auth';
import type { AuthResponse, LoginRequest } from '../types';
import {
  clearToken,
  getStoredUser,
  getToken,
  setStoredUser,
  setToken,
  type StoredAuthUser,
} from './tokenStorage';

interface AuthContextValue {
  token: string | null;
  user: StoredAuthUser | null;
  isAuthenticated: boolean;
  login: (payload: LoginRequest) => Promise<AuthResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken());
  const [user, setUser] = useState<StoredAuthUser | null>(() => getStoredUser());

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await loginRequest(payload);
    setToken(response.accessToken);
    const stored: StoredAuthUser = {
      userId: response.userId,
      username: response.username,
      role: response.role,
    };
    setStoredUser(stored);
    setTokenState(response.accessToken);
    setUser(stored);
    return response;
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setTokenState(null);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      login,
      logout,
    }),
    [token, user, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
