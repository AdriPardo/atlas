package com.atlas.application.deployment;

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
 * Autopilot host resolution (ADR-0010 / ADR-0012). Prefer shared LOCAL Docker hosts; optionally
 * request an isolated Proxmox VM and register it as a Host when the provisioner returns one.
 */
@Service
@RequiredArgsConstructor
public class AutopilotPlacementService {

    public static final String DEFAULT_LOCAL_HOSTNAME = "atlas-local";

    private final HostRepositoryPort hostRepository;
    private final VmProvisionerPort vmProvisioner;
    private final ResolveSecretValueUseCase resolveSecretValue;

    @Transactional
    public Host resolveHost(UUID explicitHostId) {
        return resolveHost(explicitHostId, PlacementMode.SHARED, null, null).host();
    }

    /**
     * Resolve a target Host for deploy.
     *
     * @param explicitHostId Advanced override; skips Autopilot policy when set
     * @param placementMode SHARED (default) or ISOLATED (Proxmox path with LOCAL fallback)
     * @param projectId used to resolve {@code proxmox.api.token}
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
            Optional<String> token = projectId == null
                    ? resolveSecretValue.byName(VmProvisionerPort.API_TOKEN_SECRET_NAME)
                    : resolveSecretValue.forProject(projectId, VmProvisionerPort.API_TOKEN_SECRET_NAME);

            VmProvisionerPort.ProvisionResult provision = vmProvisioner.provision(
                    new VmProvisionerPort.ProvisionRequest(projectId, nameHint, nameHint), token);

            if ((provision.mode() == VmProvisionerPort.ProvisionMode.CREATED
                            || provision.mode() == VmProvisionerPort.ProvisionMode.REUSED)
                    && provision.vm().isPresent()) {
                Host host = registerProvisionedHost(provision.vm().get());
                return new PlacementResult(host, PlacementMode.ISOLATED, provision.message(), provision.mode());
            }

            Host shared = resolveSharedLocal();
            String reason = "ISOLATED → shared LOCAL fallback: " + provision.message();
            return new PlacementResult(shared, PlacementMode.SHARED, reason, provision.mode());
        }

        return new PlacementResult(resolveSharedLocal(), PlacementMode.SHARED, "SHARED placement", null);
    }

    private Host registerProvisionedHost(VmProvisionerPort.VmDescriptor vm) {
        String hostname = vm.hostname();
        Optional<Host> existing = hostRepository.findByHostnameIgnoreCase(hostname);
        if (existing.isPresent()) {
            return existing.get();
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
                null));
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
