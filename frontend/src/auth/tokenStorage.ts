const TOKEN_KEY = 'atlas_access_token';
const USER_KEY = 'atlas_auth_user';

export interface StoredAuthUser {
  userId: string;
  username: string;
  role: string;
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser(): StoredAuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredAuthUser;
  } catch {
    return null;
  }
}

export function setStoredUser(user: StoredAuthUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}
