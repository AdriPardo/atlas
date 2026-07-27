package com.atlas.infrastructure.networking;

import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.domain.networking.Domain;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CloudflareTunnelAdapter implements CloudflareTunnelPort {

    private final AtlasProperties properties;
    private final CloudflareTunnelHttpGateway httpGateway;
    private final ObjectMapper objectMapper;

    public CloudflareTunnelAdapter(
            AtlasProperties properties, CloudflareTunnelHttpGateway httpGateway, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpGateway = httpGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public TunnelIngressSpec describe(Domain domain) {
        return buildSpec(domain.getHostname());
    }

    @Override
    public EnsureResult ensurePublicHostname(Domain domain, Optional<String> apiToken) {
        TunnelIngressSpec ingress = buildSpec(domain.getHostname());
        String hostname = domain.getHostname().toLowerCase(Locale.ROOT);

        if (hostname.endsWith(".atlas.local") || hostname.endsWith(".local")) {
            return new EnsureResult(
                    EnsureMode.SKIPPED,
                    "Hostname looks local-only; skip Cloudflare Tunnel registration",
                    ingress);
        }

        AtlasProperties.Networking net = properties.getNetworking();
        String accountId = blankToNull(net.getCloudflareAccountId());
        String tunnelId = blankToNull(net.getCloudflareTunnelId());
        String token = apiToken.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);

        if (accountId == null || tunnelId == null || token == null) {
            return new EnsureResult(
                    EnsureMode.MANUAL,
                    "Missing Cloudflare account/tunnel id or secret cloudflare.api.token — copy ingress into Zero Trust",
                    ingress);
        }

        try {
            String base = "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId
                    + "/cfd_tunnel/"
                    + tunnelId
                    + "/configurations";
            String getBody = httpGateway.get(base, token);
            JsonNode root = objectMapper.readTree(getBody);
            JsonNode result = root.path("result");
            JsonNode config = result.path("config");
            if (config.isMissingNode() || config.isNull()) {
                config = objectMapper.createObjectNode();
            }

            ArrayNode ingressArr;
            JsonNode existingIngress = config.path("ingress");
            if (existingIngress.isArray()) {
                ingressArr = (ArrayNode) existingIngress.deepCopy();
            } else {
                ingressArr = objectMapper.createArrayNode();
            }

            if (hostnameAlreadyPresent(ingressArr, hostname)) {
                return new EnsureResult(
                        EnsureMode.ALREADY_PRESENT,
                        "Public hostname already present on tunnel",
                        ingress);
            }

            ArrayNode updated = objectMapper.createArrayNode();
            ObjectNode catchAll = null;
            for (JsonNode rule : ingressArr) {
                if (isCatchAll(rule)) {
                    catchAll = (ObjectNode) rule.deepCopy();
                } else {
                    updated.add(rule.deepCopy());
                }
            }

            ObjectNode newRule = objectMapper.createObjectNode();
            newRule.put("hostname", hostname);
            newRule.put("service", ingress.originService());
            ObjectNode originRequest = objectMapper.createObjectNode();
            originRequest.put("noTLSVerify", ingress.noTlsVerify());
            newRule.set("originRequest", originRequest);
            updated.add(newRule);

            if (catchAll == null) {
                catchAll = objectMapper.createObjectNode();
                catchAll.put("service", "http_status:404");
            }
            updated.add(catchAll);

            ObjectNode putConfig = objectMapper.createObjectNode();
            if (config.isObject()) {
                putConfig = ((ObjectNode) config).deepCopy();
            }
            putConfig.set("ingress", updated);

            ObjectNode putBody = objectMapper.createObjectNode();
            putBody.set("config", putConfig);
            httpGateway.put(base, token, objectMapper.writeValueAsString(putBody));

            return new EnsureResult(
                    EnsureMode.APPLIED,
                    "Registered public hostname on Cloudflare Tunnel",
                    ingress);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return new EnsureResult(
                    EnsureMode.FAILED,
                    "Cloudflare Tunnel API failed: " + message + " — use copy ingress as fallback",
                    ingress);
        }
    }

    TunnelIngressSpec buildSpec(String hostnameRaw) {
        AtlasProperties.Networking net = properties.getNetworking();
        String hostname = hostnameRaw == null ? "" : hostnameRaw.trim().toLowerCase(Locale.ROOT);
        String configuredZone = blankToNull(net.getCloudflareZone());
        HostParts parts = splitHostname(hostname, configuredZone);

        String tunnelId = blankToNull(net.getCloudflareTunnelId());
        String originService = blankToNull(net.getCloudflareTunnelOrigin());
        if (originService == null) {
            originService = "https://traefik:443";
        }
        String originUrl = originService.replaceFirst("^https?://", "");
        boolean noTlsVerify = net.isCloudflareTunnelNoTlsVerify();
        String cnameTarget = tunnelId == null ? "<tunnel-id>.cfargotunnel.com" : tunnelId + ".cfargotunnel.com";

        String copyBlock = ""
                + "Subdomain: " + (parts.subdomain().isEmpty() ? "(apex)" : parts.subdomain()) + "\n"
                + "Domain: " + parts.zone() + "\n"
                + "Type: HTTPS\n"
                + "URL: " + originUrl + "\n"
                + "TLS No TLS Verify: " + (noTlsVerify ? "on" : "off") + "\n"
                + "\n"
                + "# Ingress YAML (remote-managed tunnel)\n"
                + "- hostname: " + hostname + "\n"
                + "  service: " + originService + "\n"
                + "  originRequest:\n"
                + "    noTLSVerify: " + noTlsVerify + "\n"
                + "\n"
                + "# DNS CNAME (if not already present)\n"
                + hostname + " → " + cnameTarget + " (proxied)";

        String hint = "Zero Trust → Networks → Tunnels → (atlas) → Public Hostname → Add";

        return new TunnelIngressSpec(
                hostname,
                parts.subdomain(),
                parts.zone(),
                "HTTPS",
                originUrl,
                originService,
                noTlsVerify,
                tunnelId,
                cnameTarget,
                copyBlock,
                hint);
    }

    private static boolean hostnameAlreadyPresent(ArrayNode ingress, String hostname) {
        for (JsonNode rule : ingress) {
            JsonNode hn = rule.get("hostname");
            if (hn != null && hostname.equalsIgnoreCase(hn.asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCatchAll(JsonNode rule) {
        JsonNode hostname = rule.get("hostname");
        return hostname == null || hostname.isNull() || hostname.asText().isBlank();
    }

    static HostParts splitHostname(String hostname, String configuredZone) {
        if (hostname == null || hostname.isBlank()) {
            return new HostParts("", configuredZone == null ? "" : configuredZone);
        }
        if (configuredZone != null && !configuredZone.isBlank()) {
            String zone = configuredZone.toLowerCase(Locale.ROOT);
            if (hostname.equals(zone)) {
                return new HostParts("", zone);
            }
            String suffix = "." + zone;
            if (hostname.endsWith(suffix)) {
                return new HostParts(hostname.substring(0, hostname.length() - suffix.length()), zone);
            }
        }
        int firstDot = hostname.indexOf('.');
        if (firstDot <= 0 || firstDot == hostname.length() - 1) {
            return new HostParts(hostname, configuredZone == null ? "" : configuredZone);
        }
        return new HostParts(hostname.substring(0, firstDot), hostname.substring(firstDot + 1));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    record HostParts(String subdomain, String zone) {}
}
