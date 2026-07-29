package com.atlas.infrastructure.networking;

import com.atlas.application.port.out.DnsProviderPort;
import com.atlas.domain.networking.Domain;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Cloudflare zone DNS adapter: challenge TXT stays instructional; CNAME upsert for Autopilot PUBLIC.
 */
@Component
public class CloudflareDnsAdapter implements DnsProviderPort {

    private final AtlasProperties properties;
    private final CloudflareTunnelHttpGateway httpGateway;
    private final ObjectMapper objectMapper;

    public CloudflareDnsAdapter(
            AtlasProperties properties, CloudflareTunnelHttpGateway httpGateway, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpGateway = httpGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public DnsSyncResult syncChallenge(Domain domain) {
        return new DnsSyncResult(
                false,
                "DNS challenge: create TXT "
                        + domain.dnsTxtName()
                        + " = "
                        + domain.dnsTxtValue()
                        + " (optional; PUBLIC Autopilot uses CNAME ensure)");
    }

    @Override
    public CnameSpec describeCname(Domain domain, String cnameTarget) {
        return buildSpec(domain.getHostname(), cnameTarget);
    }

    @Override
    public CnameEnsureResult ensureCname(Domain domain, String cnameTarget, Optional<String> apiToken) {
        CnameSpec spec = buildSpec(domain.getHostname(), cnameTarget);
        String hostname = domain.getHostname().toLowerCase(Locale.ROOT);

        if (hostname.endsWith(".atlas.local") || hostname.endsWith(".local")) {
            return new CnameEnsureResult(
                    CnameEnsureMode.SKIPPED,
                    "Hostname looks local-only; skip Cloudflare DNS CNAME",
                    spec);
        }

        String zoneName = blankToNull(properties.getNetworking().getCloudflareZone());
        String configuredZoneId = blankToNull(properties.getNetworking().getCloudflareZoneId());
        String target = blankToNull(cnameTarget);
        String token = apiToken.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);

        if (zoneName == null || target == null || target.contains("<tunnel-id>") || token == null) {
            return new CnameEnsureResult(
                    CnameEnsureMode.MANUAL,
                    "Missing Cloudflare zone/tunnel id or secret cloudflare.api.token — create CNAME manually",
                    spec);
        }

        try {
            String zoneId = configuredZoneId != null ? configuredZoneId : resolveZoneId(zoneName, token);
            if (zoneId == null) {
                return new CnameEnsureResult(
                        CnameEnsureMode.FAILED,
                        "Cloudflare zone not found for " + zoneName,
                        spec);
            }

            Optional<ExistingRecord> existing = findCnameRecord(zoneId, hostname, token);
            if (existing.isPresent()) {
                ExistingRecord record = existing.get();
                boolean sameTarget = target.equalsIgnoreCase(stripTrailingDot(record.content()));
                boolean sameProxied = record.proxied() == spec.proxied();
                if (sameTarget && sameProxied) {
                    return new CnameEnsureResult(
                            CnameEnsureMode.ALREADY_PRESENT,
                            "CNAME already points to " + target + " (proxied)",
                            spec);
                }
                patchRecord(zoneId, record.id(), hostname, target, spec.proxied(), token);
                return new CnameEnsureResult(
                        CnameEnsureMode.UPDATED,
                        "Updated CNAME " + hostname + " → " + target,
                        spec);
            }

            createRecord(zoneId, hostname, target, spec.proxied(), token);
            return new CnameEnsureResult(
                    CnameEnsureMode.APPLIED,
                    "Created CNAME " + hostname + " → " + target + " (proxied)",
                    spec);
        } catch (Exception ex) {
            return new CnameEnsureResult(
                    CnameEnsureMode.FAILED,
                    CloudflareApiErrorMessages.failedEnsureMessage("Cloudflare DNS API", ex),
                    spec);
        }
    }

    CnameSpec buildSpec(String hostnameRaw, String cnameTargetRaw) {
        AtlasProperties.Networking net = properties.getNetworking();
        String hostname = hostnameRaw == null ? "" : hostnameRaw.trim().toLowerCase(Locale.ROOT);
        String configuredZone = blankToNull(net.getCloudflareZone());
        String zone = resolveZoneLabel(hostname, configuredZone);
        String target = blankToNull(cnameTargetRaw);
        if (target == null) {
            String tunnelId = blankToNull(net.getCloudflareTunnelId());
            target = tunnelId == null ? "<tunnel-id>.cfargotunnel.com" : tunnelId + ".cfargotunnel.com";
        }
        boolean proxied = true;
        String copyBlock = ""
                + "Type: CNAME\n"
                + "Name: " + hostname + "\n"
                + "Target: " + target + "\n"
                + "Proxy: proxied (orange cloud)\n"
                + "Zone: " + zone + "\n"
                + "\n"
                + "# Cloudflare DNS\n"
                + hostname + " → " + target + " (proxied)";
        return new CnameSpec(hostname, zone, hostname, target, proxied, copyBlock);
    }

    private String resolveZoneId(String zoneName, String token) throws Exception {
        String url = "https://api.cloudflare.com/client/v4/zones?name="
                + URLEncoder.encode(zoneName, StandardCharsets.UTF_8)
                + "&status=active&page=1&per_page=5";
        String body = httpGateway.get(url, token);
        JsonNode result = objectMapper.readTree(body).path("result");
        if (!result.isArray() || result.isEmpty()) {
            return null;
        }
        return result.get(0).path("id").asText(null);
    }

    private Optional<ExistingRecord> findCnameRecord(String zoneId, String hostname, String token)
            throws Exception {
        String url = "https://api.cloudflare.com/client/v4/zones/"
                + zoneId
                + "/dns_records?type=CNAME&name="
                + URLEncoder.encode(hostname, StandardCharsets.UTF_8)
                + "&page=1&per_page=5";
        String body = httpGateway.get(url, token);
        JsonNode result = objectMapper.readTree(body).path("result");
        if (!result.isArray() || result.isEmpty()) {
            return Optional.empty();
        }
        JsonNode first = result.get(0);
        return Optional.of(new ExistingRecord(
                first.path("id").asText(),
                first.path("content").asText(""),
                first.path("proxied").asBoolean(false)));
    }

    private void createRecord(String zoneId, String hostname, String target, boolean proxied, String token)
            throws Exception {
        String url = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records";
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CNAME");
        payload.put("name", hostname);
        payload.put("content", target);
        payload.put("proxied", proxied);
        payload.put("ttl", 1);
        httpGateway.post(url, token, objectMapper.writeValueAsString(payload));
    }

    private void patchRecord(
            String zoneId, String recordId, String hostname, String target, boolean proxied, String token)
            throws Exception {
        String url = "https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records/" + recordId;
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "CNAME");
        payload.put("name", hostname);
        payload.put("content", target);
        payload.put("proxied", proxied);
        payload.put("ttl", 1);
        httpGateway.patch(url, token, objectMapper.writeValueAsString(payload));
    }

    static String resolveZoneLabel(String hostname, String configuredZone) {
        if (configuredZone != null && !configuredZone.isBlank()) {
            return configuredZone.toLowerCase(Locale.ROOT);
        }
        int firstDot = hostname.indexOf('.');
        if (firstDot <= 0 || firstDot == hostname.length() - 1) {
            return "";
        }
        return hostname.substring(firstDot + 1);
    }

    private static String stripTrailingDot(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.endsWith(".")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record ExistingRecord(String id, String content, boolean proxied) {}
}
