import { CLOUDFLARE_API_TOKEN_SECRET, CLOUDFLARE_TOKEN_SCOPES } from '../secrets/knownSecretHints'

/** Matches backend CloudflareApiErrorMessages.INSUFFICIENT_SCOPES phrase. */
const SCOPES_INSUFFICIENT_MARKER = 'token scopes insufficient'

export function isCloudflareScopesFailure(
  mode: string | null | undefined,
  message: string | null | undefined,
): boolean {
  if (mode !== 'FAILED' || !message) return false
  return message.toLowerCase().includes(SCOPES_INSUFFICIENT_MARKER)
}

export function cloudflareScopesFailureHint(): string {
  return `Fix ${CLOUDFLARE_API_TOKEN_SECRET}: ${CLOUDFLARE_TOKEN_SCOPES}. Update the secret under Org secrets or Project secrets, then retry Ensure / Publish.`
}
