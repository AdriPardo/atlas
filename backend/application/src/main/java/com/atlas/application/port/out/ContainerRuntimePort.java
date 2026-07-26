package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import com.atlas.domain.runtime.ContainerSnapshot;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Runs docker compose and inspects containers against a host (LOCAL process or remote SSH).
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

    List<ContainerSnapshot> listContainers(Host host, Optional<String> sshPrivateKeyPem);

    String containerLogs(Host host, String containerRef, int tailLines, Optional<String> sshPrivateKeyPem);

    void restartContainer(
            Host host, String containerRef, Optional<String> sshPrivateKeyPem, Consumer<String> logSink);
}
