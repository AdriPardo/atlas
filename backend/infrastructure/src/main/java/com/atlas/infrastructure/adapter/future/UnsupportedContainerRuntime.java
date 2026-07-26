package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "false")
public class UnsupportedContainerRuntime implements ContainerRuntimePort {

    @Override
    public void composeUp(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {
        throw new UnsupportedOperationException(
                "Container runtime is disabled (atlas.adapters.real-enabled=false). Compose: " + composeFilePath);
    }

    @Override
    public void composeDown(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {
        throw new UnsupportedOperationException(
                "Container runtime is disabled (atlas.adapters.real-enabled=false). Compose: " + composeFilePath);
    }

    @Override
    public List<ContainerSnapshot> listContainers(Host host, Optional<String> sshPrivateKeyPem) {
        return List.of();
    }

    @Override
    public String containerLogs(Host host, String containerRef, int tailLines, Optional<String> sshPrivateKeyPem) {
        throw new DomainException(
                "Container runtime is disabled (atlas.adapters.real-enabled=false); cannot fetch logs for "
                        + containerRef);
    }

    @Override
    public void restartContainer(
            Host host, String containerRef, Optional<String> sshPrivateKeyPem, Consumer<String> logSink) {
        throw new DomainException(
                "Container runtime is disabled (atlas.adapters.real-enabled=false); cannot restart " + containerRef);
    }
}
