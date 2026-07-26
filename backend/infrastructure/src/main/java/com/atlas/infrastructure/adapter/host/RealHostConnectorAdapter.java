package com.atlas.infrastructure.adapter.host;

import com.atlas.application.port.out.HostConnectorPort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.adapter.runtime.ProcessCommandRunner;
import com.atlas.infrastructure.adapter.runtime.SshCommandRunner;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "true", matchIfMissing = true)
public class RealHostConnectorAdapter implements HostConnectorPort {

    private final ProcessCommandRunner processCommandRunner;
    private final SshCommandRunner sshCommandRunner;
    private final com.atlas.application.secret.ResolveSecretValueUseCase resolveSecretValue;

    public RealHostConnectorAdapter(
            ProcessCommandRunner processCommandRunner,
            SshCommandRunner sshCommandRunner,
            com.atlas.application.secret.ResolveSecretValueUseCase resolveSecretValue) {
        this.processCommandRunner = processCommandRunner;
        this.sshCommandRunner = sshCommandRunner;
        this.resolveSecretValue = resolveSecretValue;
    }

    @Override
    public HostInspection inspect(Host host) {
        try {
            if (host.getConnectionType() == ConnectionType.LOCAL) {
                return inspectLocal();
            }
            return inspectSsh(host);
        } catch (Exception ex) {
            return new HostInspection(host.getHostname(), host.getOperatingSystem(), host.getDockerVersion(), false);
        }
    }

    private HostInspection inspectLocal() {
        String hostname = firstLine(processCommandRunner.run(List.of("hostname"), null, line -> {}));
        String os = firstLine(processCommandRunner.run(List.of("uname", "-s"), null, line -> {}));
        String docker = extractDockerVersion(
                processCommandRunner.run(List.of("docker", "version", "--format", "{{.Server.Version}}"), null, line -> {}));
        return new HostInspection(
                blank(hostname, "localhost"),
                blank(os, System.getProperty("os.name", "unknown")),
                docker,
                !docker.isBlank());
    }

    private HostInspection inspectSsh(Host host) {
        if (host.getSshPrivateKeySecretId() == null) {
            throw new DomainException("SSH host requires sshPrivateKeySecretId for sync");
        }
        String privateKey = resolveSecretValue.byId(host.getSshPrivateKeySecretId());
        String user = host.getSshUser() == null ? "root" : host.getSshUser();
        String hostname = firstLine(sshCommandRunner.run(
                host.getIp(), host.getSshPort(), user, privateKey, "hostname", line -> {}));
        String os = firstLine(sshCommandRunner.run(
                host.getIp(), host.getSshPort(), user, privateKey, "uname -s", line -> {}));
        String docker = extractDockerVersion(sshCommandRunner.run(
                host.getIp(),
                host.getSshPort(),
                user,
                privateKey,
                "docker version --format '{{.Server.Version}}'",
                line -> {}));
        return new HostInspection(blank(hostname, host.getHostname()), blank(os, "linux"), docker, !docker.isBlank());
    }

    private static String extractDockerVersion(String output) {
        if (output == null) {
            return "";
        }
        String trimmed = output.trim();
        if (trimmed.toLowerCase(Locale.ROOT).contains("error") || trimmed.isBlank()) {
            return "";
        }
        return firstLine(trimmed);
    }

    private static String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.lines().map(String::trim).filter(s -> !s.isBlank()).findFirst().orElse("");
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
