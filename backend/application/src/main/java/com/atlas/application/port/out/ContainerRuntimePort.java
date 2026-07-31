package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Low-level Docker/Podman/container ops against a host (LOCAL process or remote SSH).
 *
 * <p>Stack apply/teardown for deploys goes through {@link RuntimeOrchestratorPort}
 * (ADR-0014 phase D). This port remains for inspect / logs / restart and as the
 * Compose / Podman adapters' delegate.
 */
public interface ContainerRuntimePort {

    void composeUp(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink);

    void composeDown(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink);

    /**
     * Apply a Compose-compatible stack with {@code podman compose} (opt-in Podman adapter).
     */
    void podmanComposeUp(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink);

    /**
     * Teardown a Compose-compatible stack with {@code podman compose}.
     */
    void podmanComposeDown(
            Host host,
            Path workingDirectory,
            String composeFilePath,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink);

    List<ContainerSnapshot> listContainers(Host host, Optional<String> sshPrivateKeyPem);

    String containerLogs(Host host, String containerRef, int tailLines, Optional<String> sshPrivateKeyPem);

    void restartContainer(
            Host host, String containerRef, Optional<String> sshPrivateKeyPem, Consumer<String> logSink);
}
