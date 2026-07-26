package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.ConnectionType;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
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

    public ProcessContainerRuntimeAdapter(
            ProcessCommandRunner processCommandRunner,
            SshCommandRunner sshCommandRunner,
            AtlasProperties properties) {
        this.processCommandRunner = processCommandRunner;
        this.sshCommandRunner = sshCommandRunner;
        this.properties = properties;
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

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
