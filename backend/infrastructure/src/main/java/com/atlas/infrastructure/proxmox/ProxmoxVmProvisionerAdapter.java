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
import org.springframework.stereotype.Component;

/**
 * Proxmox VE adapter for Autopilot ISOLATED placement.
 *
 * <p>Without URL/node/template or {@code proxmox.api.token} → {@link ProvisionMode#STUBBED}. With
 * credentials, probes {@code GET /api2/json/version}. Clone runs only when {@code
 * atlas.proxmox.clone-enabled=true}; otherwise returns STUBBED after a successful probe so the next
 * increment can enable clone without re-architecting.
 */
@Component
public class ProxmoxVmProvisionerAdapter implements VmProvisionerPort {

    private final AtlasProperties properties;
    private final ProxmoxHttpGateway httpGateway;
    private final ObjectMapper objectMapper;

    public ProxmoxVmProvisionerAdapter(
            AtlasProperties properties, ProxmoxHttpGateway httpGateway, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpGateway = httpGateway;
        this.objectMapper = objectMapper;
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
            httpGateway.postForm(cloneUrl, token, form, insecure);

            // Guest IP resolution (qemu-guest-agent) is a follow-on; register Host once IP is known.
            String ip = blankToNull(px.getDefaultGuestIp()) == null ? "" : px.getDefaultGuestIp();
            if (ip.isBlank()) {
                return ProvisionResult.of(
                        ProvisionMode.STUBBED,
                        "Proxmox clone accepted (vmid="
                                + newVmid
                                + ") but guest IP unknown — set ATLAS_PROXMOX_DEFAULT_GUEST_IP or wait for agent slice; using shared LOCAL");
            }

            VmDescriptor vm = new VmDescriptor(
                    String.valueOf(newVmid),
                    hostname,
                    ip,
                    blankToNull(px.getTargetNode()) == null ? node : px.getTargetNode(),
                    blankToNull(px.getSshUser()) == null ? "atlas" : px.getSshUser(),
                    px.getSshPort() <= 0 ? 22 : px.getSshPort());
            return ProvisionResult.of(
                    ProvisionMode.CREATED, "Proxmox VM cloned as " + hostname + " (vmid=" + newVmid + ")", vm);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProvisionResult.of(ProvisionMode.UNAVAILABLE, "Proxmox API interrupted: " + e.getMessage());
        } catch (Exception e) {
            return ProvisionResult.of(
                    ProvisionMode.UNAVAILABLE, "Proxmox API error: " + e.getMessage() + " — using shared LOCAL");
        }
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
}
