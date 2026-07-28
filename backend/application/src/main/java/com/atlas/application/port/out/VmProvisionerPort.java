package com.atlas.application.port.out;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Proxmox (or future) VM provisioner for Autopilot ISOLATED placement (ADR-0012 / slice 3b / REUSED).
 *
 * <p>Always safe to call: missing config/token → {@link ProvisionMode#STUBBED}. Placement falls back
 * to shared LOCAL until {@link ProvisionMode#CREATED} or {@link ProvisionMode#REUSED} returns a
 * usable {@link VmDescriptor} with a real guest IP. Before cloning, adapters should match an
 * existing VM by hostname or Proxmox tag and return {@link ProvisionMode#REUSED}.
 */
public interface VmProvisionerPort {

    /** Org/global secret: Proxmox API token ({@code USER@REALM!TOKENID=UUID}). */
    String API_TOKEN_SECRET_NAME = "proxmox.api.token";

    /**
     * Org/global (or project-bound) secret: SSH private key PEM for VMs cloned from the Proxmox
     * template (cloud-init authorized_keys). Linked as {@code Host.sshPrivateKeySecretId}.
     */
    String SSH_PRIVATE_KEY_SECRET_NAME = "proxmox.ssh.private_key";

    boolean isConfigured();

    ProvisionResult provision(ProvisionRequest request, Optional<String> apiToken);

    /**
     * Canonical Atlas VM hostname used for Proxmox name / Host lookup ({@code atlas-…}).
     * Shared by placement (Host reuse) and the Proxmox adapter (VM reuse / clone name).
     */
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

    enum ProvisionMode {
        /** New VM cloned/started via Proxmox API. */
        CREATED,
        /** Existing VM matched and reused. */
        REUSED,
        /** Config incomplete or clone not enabled — no Host from provisioner. */
        STUBBED,
        /** Config present but API unreachable / rejected. */
        UNAVAILABLE,
        /** Clone/start attempted and failed. */
        FAILED
    }

    record ProvisionRequest(UUID projectId, String nameHint, String serviceName) {}

    /**
     * Enough data to register an Atlas {@code Host} SSH target once the VM is ready.
     *
     * @param vmid Proxmox VM id
     * @param hostname preferred Atlas hostname
     * @param ip guest IP from qemu-guest-agent (or configured fallback)
     * @param node Proxmox node name
     * @param sshUser SSH user for Host connector
     * @param sshPort SSH port
     */
    record VmDescriptor(String vmid, String hostname, String ip, String node, String sshUser, int sshPort) {}

    record ProvisionResult(ProvisionMode mode, String message, Optional<VmDescriptor> vm) {
        public static ProvisionResult of(ProvisionMode mode, String message) {
            return new ProvisionResult(mode, message, Optional.empty());
        }

        public static ProvisionResult of(ProvisionMode mode, String message, VmDescriptor vm) {
            return new ProvisionResult(mode, message, Optional.ofNullable(vm));
        }
    }
}
