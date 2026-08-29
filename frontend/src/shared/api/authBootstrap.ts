/**
 * Single-flight auth bootstrap gate. Protected API calls wait here until SSO mints a JWT
 * (or bootstrap fails). Prevents 403 races when React Query fires before /auth/sso completes.
 */
export type AuthBootstrapPhase = 'pending' | 'ready' | 'failed'

let phase: AuthBootstrapPhase = 'pending'
let resolveBootstrap: ((success: boolean) => void) | null = null

let bootstrapPromise: Promise<boolean> = createBootstrapPromise()

function createBootstrapPromise(): Promise<boolean> {
  phase = 'pending'
  return new Promise<boolean>((resolve) => {
    resolveBootstrap = resolve
  })
}

/** Start a fresh bootstrap cycle (logout, full re-probe). */
export function resetAuthBootstrap(): void {
  bootstrapPromise = createBootstrapPromise()
}

/** Mark bootstrap complete — success requires a stored JWT on the caller side. */
export function resolveAuthBootstrap(success: boolean): void {
  if (phase !== 'pending') return
  phase = success ? 'ready' : 'failed'
  resolveBootstrap?.(success)
  resolveBootstrap = null
}

export function getAuthBootstrapPhase(): AuthBootstrapPhase {
  return phase
}

export function isAuthBootstrapReady(): boolean {
  return phase === 'ready'
}

/** Resolves when bootstrap finishes; false if SSO/login could not establish a session. */
export function waitForAuthBootstrap(): Promise<boolean> {
  if (phase === 'ready') return Promise.resolve(true)
  if (phase === 'failed') return Promise.resolve(false)
  return bootstrapPromise
}
