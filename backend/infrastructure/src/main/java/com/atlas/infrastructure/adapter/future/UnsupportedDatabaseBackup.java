package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.DatabaseBackupPort;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "false")
public class UnsupportedDatabaseBackup implements DatabaseBackupPort {

    @Override
    public Path dumpTo(Path targetDir) {
        throw new UnsupportedOperationException(
                "Database backup is disabled (atlas.adapters.real-enabled=false). Target: " + targetDir);
    }
}
