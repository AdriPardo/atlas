package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.domain.host.Host;
import java.nio.file.Path;
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
}
