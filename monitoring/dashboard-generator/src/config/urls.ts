/**
 * External / internal URLs for the Quick Links panel.
 *
 * Only values with verified evidence in the repo are hard-coded.
 * Everything else must be filled before provisioning if you want working links.
 *
 * Evidence status (2026-07-26):
 * - GitHub Atlas repo: VERIFIED (https://github.com/AdriPardo/atlas)
 * - Homepage, Prometheus, Alertmanager, Grafana, Loki, Traefik, Proxmox:
 *   NOT FOUND in repository configs — left empty on purpose.
 */
export interface QuickLink {
  label: string;
  url: string;
  /** When false, the link is rendered as pending configuration. */
  configured: boolean;
}

const PENDING = "";

export const QUICK_LINKS: QuickLink[] = [
  { label: "Homepage", url: PENDING, configured: false },
  { label: "Prometheus", url: PENDING, configured: false },
  { label: "Alertmanager", url: PENDING, configured: false },
  { label: "Grafana", url: PENDING, configured: false },
  { label: "Loki", url: PENDING, configured: false },
  { label: "Traefik", url: PENDING, configured: false },
  {
    label: "GitHub",
    url: "https://github.com/AdriPardo/atlas",
    configured: true,
  },
  { label: "Proxmox", url: PENDING, configured: false },
];

/**
 * Optional deep-links from the Service Status table (job name → dashboard UID).
 * Only include UIDs discovered under monitoring/dashboards/official.
 * Currently empty: official/ has no dashboard JSON in this repository.
 */
export const SERVICE_DASHBOARD_UIDS: Record<string, string> = {
  // Example once official dashboards exist:
  // "node-exporter": "node-exporter-full",
};
