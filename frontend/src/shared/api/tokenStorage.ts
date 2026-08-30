const TOKEN_KEY = 'atlas.token'

/** In-memory mirror — synchronous read on every axios request (no localStorage race). */
let memoryToken: string | null = null

function readStored(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

// Hydrate from localStorage on module load.
memoryToken = readStored()

export const tokenStorage = {
  get: (): string | null => memoryToken,
  set: (token: string) => {
    memoryToken = token
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* private mode */
    }
  },
  clear: () => {
    memoryToken = null
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* private mode */
    }
  },
}
