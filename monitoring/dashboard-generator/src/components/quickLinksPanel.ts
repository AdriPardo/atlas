import { TextMode, PanelBuilder as TextBuilder } from "@grafana/grafana-foundation-sdk/text";
import { QUICK_LINKS } from "../config/urls.js";

/** Markdown quick links. Unconfigured URLs are listed explicitly as pending. */
export function createQuickLinksPanel(span = 24, height = 5): TextBuilder {
  const lines = QUICK_LINKS.map((link) => {
    if (link.configured && link.url) {
      return `- [${link.label}](${link.url})`;
    }
    return `- **${link.label}** — _URL not configured_ (edit \`src/config/urls.ts\`)`;
  });

  const content = [
    "### Quick Links",
    "",
    ...lines,
    "",
    "_Only GitHub is hard-coded from repository evidence. Fill the remaining URLs in `src/config/urls.ts`, then regenerate._",
  ].join("\n");

  return new TextBuilder()
    .title("Quick Links")
    .description(
      "Operational entry points for Atlas. Unconfigured destinations are intentional until hostnames are confirmed in repo or provisioning.",
    )
    .span(span)
    .height(height)
    .mode(TextMode.Markdown)
    .content(content);
}
