/** Logical secret names resolved by deploy / Autopilot (binding → owned → org). */
export const GIT_TOKEN_SECRET = 'git.token'
export const CLOUDFLARE_API_TOKEN_SECRET = 'cloudflare.api.token'

/** Minimum Cloudflare API token permissions for Tunnel + DNS Autopilot. */
export const CLOUDFLARE_TOKEN_SCOPES =
  'Zone → DNS → Edit (CNAME) and Account → Cloudflare Tunnel / Cloudflare One → Edit (Public Hostname)'

export function isCloudflareApiTokenName(name: string): boolean {
  return name.trim().toLowerCase() === CLOUDFLARE_API_TOKEN_SECRET
}

/** Helper under the Name field when creating/linking a secret. */
export function secretNameHelperText(name: string, fallback = 'e.g. git.token'): string {
  if (isCloudflareApiTokenName(name)) {
    return `API token scopes: ${CLOUDFLARE_TOKEN_SCOPES}`
  }
  const trimmed = name.trim().toLowerCase()
  if (trimmed === GIT_TOKEN_SECRET) {
    return 'GitHub PAT with repo scope (private clone + optional webhook register)'
  }
  return fallback
}
