/** Logical secret names resolved by deploy / Autopilot (binding → owned → org). */
export const GIT_TOKEN_SECRET = 'git.token'
export const CLOUDFLARE_API_TOKEN_SECRET = 'cloudflare.api.token'
export const DB_URL_SECRET = 'db.url'
export const DB_SCHEMA_SECRET = 'db.schema'
export const DB_PASSWORD_SECRET = 'db.password'
/** Common app secret names (user-owned in Atlas → envFrom → runtime). */
export const AI_OPENAI_SECRET = 'ai.openai'
export const AI_OPENAI_BASE_URL_SECRET = 'ai.openai.base_url'
export const AI_ELEVENLABS_SECRET = 'ai.elevenlabs'
export const AI_DEEPSEEK_SECRET = 'ai.deepseek'
export const AI_PROVIDER_SECRET = 'ai.provider'
export const AI_API_KEY_SECRET = 'ai.api_key'
export const AI_BASE_URL_SECRET = 'ai.base_url'

/** Minimum Cloudflare API token permissions for Tunnel + DNS Autopilot. */
export const CLOUDFLARE_TOKEN_SCOPES =
  'Zone → DNS → Edit (CNAME) and Account → Cloudflare Tunnel / Cloudflare One → Edit (Public Hostname)'

export function isCloudflareApiTokenName(name: string): boolean {
  return name.trim().toLowerCase() === CLOUDFLARE_API_TOKEN_SECRET
}

/** Helper under the Name field when creating/linking a secret. */
export function secretNameHelperText(name: string, fallback = 'e.g. git.token, openai.api_key, stripe.secret'): string {
  if (isCloudflareApiTokenName(name)) {
    return `API token scopes: ${CLOUDFLARE_TOKEN_SCOPES}`
  }
  const trimmed = name.trim().toLowerCase()
  if (trimmed === GIT_TOKEN_SECRET) {
    return 'GitHub PAT with repo scope (private clone + optional webhook register)'
  }
  if (trimmed === DB_URL_SECRET) {
    return 'Full DB connection string for this project (maps to DATABASE_URL). Prefer schema app_<project_slug>.'
  }
  if (trimmed === DB_SCHEMA_SECRET) {
    return 'Canonical schema name (convention: app_<project_slug>). Pair with db.url.'
  }
  if (trimmed === DB_PASSWORD_SECRET) {
    return 'DB password only if the app does not use a full URL; prefer db.url.'
  }
  if (trimmed === AI_OPENAI_SECRET) {
    return 'Your OpenAI (or compatible) API key → OPENAI_API_KEY via envFrom in atlas.yml'
  }
  if (trimmed === AI_OPENAI_BASE_URL_SECRET) {
    return 'OpenAI-compatible base URL → OPENAI_BASE_URL. Pair with ai.openai.'
  }
  if (trimmed === AI_ELEVENLABS_SECRET) {
    return 'Your ElevenLabs API key → ELEVENLABS_API_KEY via envFrom'
  }
  if (trimmed === AI_DEEPSEEK_SECRET) {
    return 'Your DeepSeek API key → DEEPSEEK_API_KEY via envFrom'
  }
  if (trimmed === AI_PROVIDER_SECRET) {
    return 'Logical AI provider id → AI_PROVIDER (openai | deepseek | local | …)'
  }
  if (trimmed === AI_API_KEY_SECRET) {
    return 'Generic AI API key → AI_API_KEY when the app uses a single client'
  }
  if (trimmed === AI_BASE_URL_SECRET) {
    return 'Generic AI base URL → AI_BASE_URL. Pair with ai.api_key / ai.provider.'
  }
  return fallback
}
