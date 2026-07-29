package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Runs an arbitrary shell command on a host workspace (LOCAL or SSH).
 * Used for optional {@code runtime.migrateCommand} after stack apply — Atlas does not interpret ORM tools.
 */
public interface HostCommandPort {

    void run(HostCommand command);

    record HostCommand(
            Host host,
            Path workingDirectory,
            String shellCommand,
            Optional<String> sshPrivateKeyPem,
            Consumer<String> logSink) {}
}
