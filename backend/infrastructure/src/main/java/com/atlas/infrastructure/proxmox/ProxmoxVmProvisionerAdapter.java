package com.atlas.infrastructure.proxmox;

import com.atlas.application.port.out.VmProvisionerPort;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Proxmox VE adapter for Autopilot ISOLATED placement (ADR-0012 / slice 3b).
 *
 * <p>Without URL/node/template or {@code proxmox.api.token} → {@link ProvisionMode#STUBBED}. With
 * credentials, probes {@code GET /api2/json/version}. Clone runs only when {@code
 * atlas.proxmox.clone-enabled=true}: clone → wait task → start → poll qemu-guest-agent for IPv4
 * (fallback {@code ATLAS_PROXMOX_DEFAULT_GUEST_IP}).
 */
@Component
public class ProxmoxVmProvisionerAdapter implements VmProvisionerPort {

    private final AtlasProperties properties;
    private final ProxmoxHttpGateway httpGateway;
    private final ObjectMapper objectMapper;
    private final ProxmoxSleeper sleeper;

    @Autowired
    public ProxmoxVmProvisionerAdapter(
            AtlasProperties properties, ProxmoxHttpGateway httpGateway, ObjectMapper objectMapper) {
        this(properties, httpGateway, objectMapper, ms -> {
            if (ms > 0) {
                Thread.sleep(ms);
            }
        });
    }

    ProxmoxVmProvisionerAdapter(
            AtlasProperties properties,
            ProxmoxHttpGateway httpGateway,
            ObjectMapper objectMapper,
            ProxmoxSleeper sleeper) {
        this.properties = properties;
        this.httpGateway = httpGateway;
        this.objectMapper = objectMapper;
        this.sleeper = sleeper;
    }

    @Override
    public boolean isConfigured() {
        AtlasProperties.Proxmox px = properties.getProxmox();
        return blankToNull(px.getApiUrl()) != null
                && blankToNull(px.getNode()) != null
                && blankToNull(px.getTemplateVmid()) != null;
    }

    @Override
    public ProvisionResult provision(ProvisionRequest request, Optional<String> apiToken) {
        AtlasProperties.Proxmox px = properties.getProxmox();
        String apiUrl = blankToNull(px.getApiUrl());
        String node = blankToNull(px.getNode());
        String templateVmid = blankToNull(px.getTemplateVmid());
        String token = apiToken.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);

        if (apiUrl == null || node == null || templateVmid == null) {
            return ProvisionResult.of(
                    ProvisionMode.STUBBED,
                    "Proxmox not configured (need ATLAS_PROXMOX_API_URL, NODE, TEMPLATE_VMID) — using shared LOCAL");
        }
        if (token == null) {
            return ProvisionResult.of(
                    ProvisionMode.STUBBED,
                    "Missing secret proxmox.api.token — using shared LOCAL until token is set");
        }

        String base = trimTrailingSlash(apiUrl);
        boolean insecure = px.isInsecureTls();
        String targetNode = blankToNull(px.getTargetNode()) == null ? node : px.getTargetNode();
        try {
            String versionBody = httpGateway.get(base + "/api2/json/version", token, insecure);
            JsonNode version = objectMapper.readTree(versionBody).path("data").path("version");
            String versionLabel = version.isMissingNode() || version.asText().isBlank()
                    ? "unknown"
                    : version.asText();

            if (!px.isCloneEnabled()) {
                return ProvisionResult.of(
                        ProvisionMode.STUBBED,
                        "Proxmox reachable (v"
                                + versionLabel
                                + "); clone disabled (ATLAS_PROXMOX_CLONE_ENABLED=false) — using shared LOCAL");
            }

            String hostname = sanitizeHostname(request.nameHint(), request.serviceName());
            int newVmid = ThreadLocalRandom.current().nextInt(200, 899_999);
            Map<String, String> form = new LinkedHashMap<>();
            form.put("newid", String.valueOf(newVmid));
            form.put("name", hostname);
            form.put("full", "1");
            if (blankToNull(px.getStorage()) != null) {
                form.put("storage", px.getStorage());
            }
            if (blankToNull(px.getTargetNode()) != null) {
                form.put("target", px.getTargetNode());
            }

            String cloneUrl = base + "/api2/json/nodes/" + node + "/qemu/" + templateVmid + "/clone";
            String cloneBody = httpGateway.postForm(cloneUrl, token, form, insecure);
            waitForTaskIfPresent(base, node, token, insecure, cloneBody, px);

            String startUrl =
                    base + "/api2/json/nodes/" + targetNode + "/qemu/" + newVmid + "/status/start";
            String startBody = httpGateway.postForm(startUrl, token, Map.of(), insecure);
            waitForTaskIfPresent(base, targetNode, token, insecure, startBody, px);

            String ip = waitForGuestIp(base, targetNode, String.valueOf(newVmid), token, insecure, px);
            if (ip == null || ip.isBlank()) {
                return ProvisionResult.of(
                        ProvisionMode.STUBBED,
                        "Proxmox VM "
                                + newVmid
                                + " started but guest IP unknown (agent timeout; set ATLAS_PROXMOX_DEFAULT_GUEST_IP or enable qemu-guest-agent) — using shared LOCAL");
            }

            VmDescriptor vm = new VmDescriptor(
                    String.valueOf(newVmid),
                    hostname,
                    ip,
                    targetNode,
                    blankToNull(px.getSshUser()) == null ? "atlas" : px.getSshUser(),
                    px.getSshPort() <= 0 ? 22 : px.getSshPort());
            return ProvisionResult.of(
                    ProvisionMode.CREATED,
                    "Proxmox VM ready as " + hostname + " (vmid=" + newVmid + ", ip=" + ip + ")",
                    vm);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProvisionResult.of(ProvisionMode.UNAVAILABLE, "Proxmox API interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ProvisionResult.of(
                    ProvisionMode.UNAVAILABLE, "Proxmox API error: " + e.getMessage() + " — using shared LOCAL");
        }
    }

    private void waitForTaskIfPresent(
            String base,
            String node,
            String token,
            boolean insecure,
            String responseBody,
            AtlasProperties.Proxmox px)
            throws Exception {
        String upid = extractUpid(responseBody);
        if (upid == null) {
            return;
        }
        long deadline = System.currentTimeMillis() + timeoutMillis(px);
        int pollMs = pollIntervalMs(px);
        while (System.currentTimeMillis() < deadline) {
            String encoded = urlPathSegment(upid);
            String statusBody =
                    httpGateway.get(base + "/api2/json/nodes/" + node + "/tasks/" + encoded + "/status", token, insecure);
            JsonNode data = objectMapper.readTree(statusBody).path("data");
            String status = data.path("status").asText("");
            if ("stopped".equalsIgnoreCase(status)) {
                String exit = data.path("exitstatus").asText("OK");
                if (!"OK".equalsIgnoreCase(exit)) {
                    throw new IllegalStateException("Proxmox task failed: " + exit + " (upid=" + upid + ")");
                }
                return;
            }
            sleeper.sleepMillis(pollMs);
        }
        throw new IllegalStateException("Proxmox task timed out: " + upid);
    }

    private String waitForGuestIp(
            String base,
            String node,
            String vmid,
            String token,
            boolean insecure,
            AtlasProperties.Proxmox px)
            throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis(px);
        int pollMs = pollIntervalMs(px);
        String agentUrl = base + "/api2/json/nodes/" + node + "/qemu/" + vmid + "/agent/network-get-interfaces";
        while (true) {
            try {
                String body = httpGateway.get(agentUrl, token, insecure);
                Optional<String> ip = extractGuestIpv4(objectMapper.readTree(body));
                if (ip.isPresent()) {
                    return ip.get();
                }
            } catch (Exception ignored) {
                // Guest agent not ready yet — keep polling until timeout.
            }
            if (System.currentTimeMillis() >= deadline) {
                break;
            }
            sleeper.sleepMillis(pollMs);
        }
        return blankToNull(px.getDefaultGuestIp());
    }

    static Optional<String> extractGuestIpv4(JsonNode root) {
        JsonNode result = root.path("data").path("result");
        if (!result.isArray()) {
            result = root.path("data");
        }
        if (!result.isArray()) {
            return Optional.empty();
        }
        for (JsonNode iface : result) {
            String name = iface.path("name").asText("");
            if ("lo".equals(name) || name.startsWith("lo.")) {
                continue;
            }
            JsonNode addresses = iface.path("ip-addresses");
            if (!addresses.isArray()) {
                continue;
            }
            for (JsonNode addr : addresses) {
                if (!"ipv4".equalsIgnoreCase(addr.path("ip-address-type").asText())) {
                    continue;
                }
                String ip = addr.path("ip-address").asText("").trim();
                if (ip.isEmpty() || ip.startsWith("127.") || ip.startsWith("169.254.")) {
                    continue;
                }
                return Optional.of(ip);
            }
        }
        return Optional.empty();
    }

    static String extractUpid(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            JsonNode data = new ObjectMapper().readTree(responseBody).path("data");
            if (data.isTextual()) {
                String text = data.asText();
                return text.startsWith("UPID:") ? text : null;
            }
        } catch (Exception ignored) {
            // fall through
        }
        String trimmed = responseBody.trim();
        if (trimmed.startsWith("UPID:")) {
            return trimmed;
        }
        return null;
    }

    static String sanitizeHostname(String nameHint, String serviceName) {
        String raw = nameHint != null && !nameHint.isBlank()
                ? nameHint
                : (serviceName != null && !serviceName.isBlank() ? serviceName : "atlas-vm");
        String label = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
        label = label.replaceAll("(^-|-$)", "");
        if (label.isBlank()) {
            label = "atlas-vm";
        }
        if (label.length() > 63) {
            label = label.substring(0, 63).replaceAll("-$", "");
        }
        if (!label.startsWith("atlas-")) {
            label = "atlas-" + label;
            if (label.length() > 63) {
                label = label.substring(0, 63).replaceAll("-$", "");
            }
        }
        return label;
    }

    private static long timeoutMillis(AtlasProperties.Proxmox px) {
        int seconds = px.getGuestReadyTimeoutSeconds();
        // Negative → default 120s; 0 allowed for tests (immediate give-up → defaultGuestIp / STUBBED).
        if (seconds < 0) {
            seconds = 120;
        }
        return seconds * 1000L;
    }

    private static int pollIntervalMs(AtlasProperties.Proxmox px) {
        int ms = px.getGuestReadyPollIntervalMs();
        return ms <= 0 ? 3000 : ms;
    }

    private static String urlPathSegment(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    @FunctionalInterface
    interface ProxmoxSleeper {
        void sleepMillis(long ms) throws InterruptedException;
    }
}
