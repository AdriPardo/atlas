package com.atlas.application.deployment;

import com.atlas.application.host.SyncHostUseCase;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.VmProvisionerPort;
import com.atlas.application.secret.ResolveSecretValueUseCase;
import com.atlas.domain.deployment.PlacementMode;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autopilot host resolution (ADR-0010 / ADR-0012 / REUSED). Prefer shared LOCAL Docker hosts;
 * for ISOLATED, reuse an existing SSH Host by hostname, else ask Proxmox (reuse VM by name/tag or
 * clone), register Host + Sync, and let Deploy reuse {@code DEPLOY_SERVICE}.
 */
@Service
@RequiredArgsConstructor
public class AutopilotPlacementService {

    public static final String DEFAULT_LOCAL_HOSTNAME = "atlas-local";

    private final HostRepositoryPort hostRepository;
    private final VmProvisionerPort vmProvisioner;
    private final ResolveSecretValueUseCase resolveSecretValue;
    private final SyncHostUseCase syncHostUseCase;

    @Transactional
    public Host resolveHost(UUID explicitHostId) {
        return resolveHost(explicitHostId, PlacementMode.SHARED, null, null).host();
    }

    /**
     * Resolve a target Host for deploy.
     *
     * @param explicitHostId Advanced override; skips Autopilot policy when set
     * @param placementMode SHARED (default) or ISOLATED (Proxmox path with LOCAL fallback)
     * @param projectId used to resolve {@code proxmox.api.token} / {@code proxmox.ssh.private_key}
     * @param nameHint service/project name for VM hostname
     */
    @Transactional
    public PlacementResult resolveHost(
            UUID explicitHostId, PlacementMode placementMode, UUID projectId, String nameHint) {
        if (explicitHostId != null) {
            Host host = hostRepository
                    .findById(explicitHostId)
                    .orElseThrow(() -> new NotFoundException("Host not found: " + explicitHostId));
            return new PlacementResult(host, PlacementMode.SHARED, "explicit hostId override", null);
        }

        PlacementMode mode = placementMode == null ? PlacementMode.SHARED : placementMode;
        if (mode == PlacementMode.ISOLATED) {
            String hostname = VmProvisionerPort.sanitizeHostname(nameHint, nameHint);
            Optional<Host> reusableHost = findReusableIsolatedHost(hostname);
            if (reusableHost.isPresent()) {
                Host host = reusableHost.get();
                syncHostUseCase.execute(host.getId());
                return new PlacementResult(
                        host,
                        PlacementMode.ISOLATED,
                        "Reused existing Host " + hostname + " (no Proxmox clone)",
                        VmProvisionerPort.ProvisionMode.REUSED);
            }

            Optional<String> token = projectId == null
                    ? resolveSecretValue.byName(VmProvisionerPort.API_TOKEN_SECRET_NAME)
                    : resolveSecretValue.forProject(projectId, VmProvisionerPort.API_TOKEN_SECRET_NAME);

            VmProvisionerPort.ProvisionResult provision = vmProvisioner.provision(
                    new VmProvisionerPort.ProvisionRequest(projectId, nameHint, nameHint), token);

            if ((provision.mode() == VmProvisionerPort.ProvisionMode.CREATED
                            || provision.mode() == VmProvisionerPort.ProvisionMode.REUSED)
                    && provision.vm().isPresent()) {
                Optional<UUID> sshKeySecretId =
                        resolveSecretValue.idForProject(projectId, VmProvisionerPort.SSH_PRIVATE_KEY_SECRET_NAME);
                if (sshKeySecretId.isEmpty()) {
                    Host shared = resolveSharedLocal();
                    String reason = "ISOLATED → shared LOCAL fallback: missing secret "
                            + VmProvisionerPort.SSH_PRIVATE_KEY_SECRET_NAME
                            + " (link PEM used by the Proxmox cloud-init template)";
                    return new PlacementResult(
                            shared, PlacementMode.SHARED, reason, provision.mode());
                }
                Host host = registerProvisionedHost(provision.vm().get(), sshKeySecretId.get());
                syncHostUseCase.execute(host.getId());
                return new PlacementResult(host, PlacementMode.ISOLATED, provision.message(), provision.mode());
            }

            Host shared = resolveSharedLocal();
            String reason = "ISOLATED → shared LOCAL fallback: " + provision.message();
            return new PlacementResult(shared, PlacementMode.SHARED, reason, provision.mode());
        }

        return new PlacementResult(resolveSharedLocal(), PlacementMode.SHARED, "SHARED placement", null);
    }

    /**
     * Prefer an already-registered SSH Host for the Autopilot hostname (dogfood: redeploy without
     * cloning another Proxmox VM).
     */
    private Optional<Host> findReusableIsolatedHost(String hostname) {
        return hostRepository.findByHostnameIgnoreCase(hostname).filter(host -> {
            if (host.getConnectionType() != ConnectionType.SSH) {
                return false;
            }
            if (host.getSshPrivateKeySecretId() == null) {
                return false;
            }
            String ip = host.getIp();
            return ip != null && !ip.isBlank() && !"0.0.0.0".equals(ip.trim());
        });
    }

    private Host registerProvisionedHost(VmProvisionerPort.VmDescriptor vm, UUID sshPrivateKeySecretId) {
        String hostname = vm.hostname();
        Optional<Host> existing = hostRepository.findByHostnameIgnoreCase(hostname);
        if (existing.isPresent()) {
            Host host = existing.get();
            host.update(
                    host.getHostname(),
                    vm.ip(),
                    host.getOperatingSystem(),
                    host.getDockerVersion(),
                    host.isOnline(),
                    ConnectionType.SSH,
                    vm.sshUser(),
                    vm.sshPort(),
                    sshPrivateKeySecretId);
            return hostRepository.save(host);
        }
        String ip = vm.ip() == null || vm.ip().isBlank() ? "0.0.0.0" : vm.ip();
        return hostRepository.save(Host.create(
                hostname,
                ip,
                "linux",
                "",
                false,
                ConnectionType.SSH,
                vm.sshUser(),
                vm.sshPort(),
                sshPrivateKeySecretId));
    }

    private Host resolveSharedLocal() {
        List<Host> hosts = hostRepository.listForPlacement();
        if (hosts.isEmpty()) {
            return ensureDefaultLocalHost();
        }
        return hosts.stream()
                .max(Comparator.comparingInt(AutopilotPlacementService::score))
                .orElseGet(this::ensureDefaultLocalHost);
    }

    private Host ensureDefaultLocalHost() {
        return hostRepository
                .findByHostnameIgnoreCase(DEFAULT_LOCAL_HOSTNAME)
                .orElseGet(() -> hostRepository.save(Host.create(
                        DEFAULT_LOCAL_HOSTNAME,
                        "127.0.0.1",
                        "linux",
                        "",
                        true,
                        ConnectionType.LOCAL,
                        null,
                        22,
                        null)));
    }

    static int score(Host host) {
        int score = 0;
        if (host.getConnectionType() == ConnectionType.LOCAL) {
            score += 100;
        }
        if (host.isOnline()) {
            score += 50;
        }
        String hostname = host.getHostname() == null ? "" : host.getHostname().toLowerCase(Locale.ROOT);
        if (DEFAULT_LOCAL_HOSTNAME.equals(hostname) || "default".equals(hostname)) {
            score += 25;
        }
        return score;
    }

    public record PlacementResult(
            Host host,
            PlacementMode effectiveMode,
            String reason,
            VmProvisionerPort.ProvisionMode provisionMode) {}
}
