package com.atlas.application.backup;

import com.atlas.application.port.out.BackupPolicyPort;
import com.atlas.application.port.out.BillingMeterPort;
import com.atlas.application.port.out.DatabaseBackupPort;
import com.atlas.domain.billing.UsageMeters;
import com.atlas.domain.shared.DomainException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecuteBackupDatabaseJobUseCase {

    private static final BigDecimal BYTES_PER_GIB = BigDecimal.valueOf(1024L * 1024L * 1024L);

    private final DatabaseBackupPort databaseBackupPort;
    private final BackupPolicyPort backupPolicy;
    private final BillingMeterPort billingMeter;

    public Path execute() {
        Path dir = Path.of(backupPolicy.directory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new DomainException("Unable to create backup directory: " + dir + " — " + ex.getMessage());
        }

        Path dump = databaseBackupPort.dumpTo(dir);
        recordBackupGb(dump);
        pruneOldDumps(dir, backupPolicy.keepCount());
        return dump;
    }

    private void recordBackupGb(Path dump) {
        try {
            long bytes = Files.size(dump);
            BigDecimal gib =
                    BigDecimal.valueOf(bytes).divide(BYTES_PER_GIB, 6, RoundingMode.HALF_UP);
            Map<String, String> dimensions = new LinkedHashMap<>();
            dimensions.put("path", dump.getFileName().toString());
            dimensions.put("bytes", Long.toString(bytes));
            billingMeter.record(UsageMeters.BACKUP_GB, gib, dimensions);
        } catch (IOException | RuntimeException ex) {
            // Soft metering — backup already succeeded; size probe must not fail the job.
        }
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
