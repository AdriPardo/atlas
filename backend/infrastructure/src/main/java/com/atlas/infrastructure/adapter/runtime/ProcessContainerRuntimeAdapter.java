package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "true", matchIfMissing = true)
public class ProcessContainerRuntimeAdapter implements ContainerRuntimePort {

    private final ProcessCommandRunner processCommandRunner;
    private final SshCommandRunner sshCommandRunner;
    private final AtlasProperties properties;
    private final ObjectMapper objectMapper;

    public ProcessContainerRuntimeAdapter(
            ProcessCommandRunner processCommandRunner,
            SshCommandRunner sshCommandRunner,
            AtlasProperties properties,
            ObjectMapper objectMapper) {
        this.processCommandRunner = processCommandRunner;
        this.sshCommandRunner = sshCommandRunner;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void composeUp(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {
        runCompose(host, workingDirectory, composeFilePath, sshPrivateKeyPem, logSink, "up", "-d", "--remove-orphans");
    }

    @Override
    public void composeDown(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {
        runCompose(host, workingDirectory, composeFilePath, sshPrivateKeyPem, logSink, "down");
    }

    @Override
    public List<ContainerSnapshot> listContainers(Host host, Optional<String> sshPrivateKeyPem) {
        String output = runDocker(
                host,
                sshPrivateKeyPem,
                List.of("docker", "ps", "-a", "--format", "{{json .}}"),
                "docker ps -a --format '{{json .}}'");
        return parseContainerSnapshots(output);
    }

    @Override
    public String containerLogs(Host host, String containerRef, int tailLines, Optional<String> sshPrivateKeyPem) {
        return runDocker(
                host,
                sshPrivateKeyPem,
                List.of("docker", "logs", "--tail", String.valueOf(tailLines), containerRef),
                "docker logs --tail " + tailLines + " " + shellQuote(containerRef));
    }

    @Override
    public void restartContainer(
            Host host, String containerRef, Optional<String> sshPrivateKeyPem, Consumer<String> logSink) {
        String output = runDocker(
                host,
                sshPrivateKeyPem,
                List.of("docker", "restart", containerRef),
                "docker restart " + shellQuote(containerRef));
        if (logSink != null) {
            output.lines().forEach(logSink);
            logSink.accept("Restarted container " + containerRef);
        }
    }

    List<ContainerSnapshot> parseContainerSnapshots(String output) {
        List<ContainerSnapshot> containers = new ArrayList<>();
        if (output == null || output.isBlank()) {
            return containers;
        }
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || !trimmed.startsWith("{")) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                containers.add(new ContainerSnapshot(
                        text(node, "ID", "Id"),
                        firstName(text(node, "Names", "Name")),
                        text(node, "Image"),
                        text(node, "State"),
                        text(node, "Status"),
                        text(node, "Ports"),
                        text(node, "Labels")));
            } catch (Exception ignored) {
                // skip malformed lines from docker noise
            }
        }
        return containers;
    }

    private String runDocker(
            Host host, Optional<String> sshPrivateKeyPem, List<String> localCommand, String remoteCommand) {
        if (host.getConnectionType() == ConnectionType.LOCAL) {
            Map<String, String> env = new HashMap<>();
            String dockerHost = properties.getDocker().getHost();
            if (dockerHost != null && !dockerHost.isBlank()) {
                env.put("DOCKER_HOST", dockerHost);
            }
            return processCommandRunner.run(localCommand, null, env, line -> {});
        }

        String key = sshPrivateKeyPem.orElseThrow(
                () -> new DomainException("SSH private key required for remote docker"));
        String user = host.getSshUser() == null ? "root" : host.getSshUser();
        return sshCommandRunner.run(host.getIp(), host.getSshPort(), user, key, remoteCommand, line -> {});
    }

    private void runCompose(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink,
            String... composeArgs) {
        String composePath = composeFilePath == null || composeFilePath.isBlank()
                ? "docker-compose.yml"
                : composeFilePath;

        if (host.getConnectionType() == ConnectionType.LOCAL) {
            List<String> command = new ArrayList<>();
            command.add("docker");
            command.add("compose");
            command.add("-f");
            command.add(composePath);
            command.addAll(List.of(composeArgs));
            Map<String, String> env = new HashMap<>();
            String dockerHost = properties.getDocker().getHost();
            if (dockerHost != null && !dockerHost.isBlank()) {
                env.put("DOCKER_HOST", dockerHost);
            }
            logSink.accept("Running local: " + String.join(" ", command) + " (cwd=" + workingDirectory + ")");
            processCommandRunner.run(command, workingDirectory, env, logSink);
            return;
        }

        String key = sshPrivateKeyPem.orElseThrow(
                () -> new DomainException("SSH private key required for remote compose"));
        String user = host.getSshUser() == null ? "root" : host.getSshUser();
        String remoteDir = "/var/lib/atlas/workspaces/" + workingDirectory.getFileName();
        sshCommandRunner.uploadDirectory(
                host.getIp(), host.getSshPort(), user, key, workingDirectory, remoteDir, logSink);
        String remoteCmd = "cd "
                + shellQuote(remoteDir)
                + " && docker compose -f "
                + shellQuote(composePath)
                + " "
                + String.join(" ", composeArgs);
        logSink.accept("Running remote compose: " + remoteCmd);
        sshCommandRunner.run(host.getIp(), host.getSshPort(), user, key, remoteCmd, logSink);
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText("");
            }
        }
        return "";
    }

    private static String firstName(String names) {
        if (names == null || names.isBlank()) {
            return "";
        }
        String cleaned = names.startsWith("/") ? names.substring(1) : names;
        int comma = cleaned.indexOf(',');
        return comma >= 0 ? cleaned.substring(0, comma) : cleaned;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
