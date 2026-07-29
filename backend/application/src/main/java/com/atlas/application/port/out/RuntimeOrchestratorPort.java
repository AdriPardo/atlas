package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.RuntimeCapability;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Generic stack orchestration (ADR-0014 phase D). Deploy use cases talk here;
 * Compose remains the default adapter via {@link #apply} / {@link #teardown}.
 *
 * <p>{@link ContainerRuntimePort} stays for container inspect/logs/restart.
 */
public interface RuntimeOrchestratorPort {

    void apply(RuntimeApplyCommand command);

    void teardown(RuntimeTeardownCommand command);

    record RuntimeApplyCommand(
            Host host,
            Path workingDirectory,
            RuntimeCapability capability,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {}

    record RuntimeTeardownCommand(
            Host host,
            Path workingDirectory,
            RuntimeCapability capability,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {}
}
