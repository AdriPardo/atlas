/** Full-page SSO mint — Traefik ForwardAuth on GET /api/v1/auth/sso (document navigation). */
export const SSO_MINT_PATH = '/api/v1/auth/sso'

export function redirectToSsoMint(): void {
  window.location.replace(SSO_MINT_PATH)
}
