const TOKEN_KEY = 'atlas.token'

function readCookie(name: string): string | null {
  const prefix = `${name}=`
  const match = document.cookie
    .split(';')
    .map((part) => part.trim())
    .find((part) => part.startsWith(prefix))
  if (!match) return null
  return decodeURIComponent(match.slice(prefix.length)).trim()
}

function clearCookie(name: string): void {
  document.cookie = `${name}=; Max-Age=0; path=/; SameSite=Lax`
}

export const tokenStorage = {
  get: () => {
    const fromStorage = localStorage.getItem(TOKEN_KEY)
    if (fromStorage?.trim()) return fromStorage.trim()
    return readCookie(TOKEN_KEY)
  },
  set: (token: string) => {
    const trimmed = token.trim()
    localStorage.setItem(TOKEN_KEY, trimmed)
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY)
    clearCookie(TOKEN_KEY)
  },
}
