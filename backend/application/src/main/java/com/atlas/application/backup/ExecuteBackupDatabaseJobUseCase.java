package com.atlas.application.backup;

import com.atlas.application.port.out.BackupPolicyPort;
import com.atlas.application.port.out.DatabaseBackupPort;
import com.atlas.domain.shared.DomainException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecuteBackupDatabaseJobUseCase {

    private final DatabaseBackupPort databaseBackupPort;
    private final BackupPolicyPort backupPolicy;

    public Path execute() {
        Path dir = Path.of(backupPolicy.directory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new DomainException("Unable to create backup directory: " + dir + " — " + ex.getMessage());
        }

        Path dump = databaseBackupPort.dumpTo(dir);
        pruneOldDumps(dir, backupPolicy.keepCount());
        return dump;
    }

    private void pruneOldDumps(Path dir, int keepCount) {
        int keep = Math.max(1, keepCount);
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> dumps = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith("atlas-") && name.endsWith(".sql.gz");
                    })
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .toList();
            for (int i = keep; i < dumps.size(); i++) {
                Files.deleteIfExists(dumps.get(i));
            }
        } catch (IOException ex) {
            throw new DomainException("Unable to prune backups in " + dir + " — " + ex.getMessage());
        }
    }
}
