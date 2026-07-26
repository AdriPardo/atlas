package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Runs docker compose against a host (LOCAL process or remote SSH).
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
}
