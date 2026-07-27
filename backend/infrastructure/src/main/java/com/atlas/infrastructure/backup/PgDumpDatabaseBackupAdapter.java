package com.atlas.infrastructure.backup;

import com.atlas.application.port.out.DatabaseBackupPort;
import com.atlas.domain.shared.DomainException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Logical dump via {@code pg_dump} piped to gzip. Requires {@code postgresql-client} on the host/image.
 */
@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "true", matchIfMissing = true)
public class PgDumpDatabaseBackupAdapter implements DatabaseBackupPort {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String pgDumpBinary;

    public PgDumpDatabaseBackupAdapter(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${atlas.backup.pg-dump-binary:pg_dump}") String pgDumpBinary) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.pgDumpBinary = pgDumpBinary;
    }

    @Override
    public Path dumpTo(Path targetDir) {
        JdbcPostgresTarget target = JdbcPostgresTarget.parse(jdbcUrl);
        Path out = targetDir.resolve("atlas-" + STAMP.format(Instant.now()) + ".sql.gz");

        List<String> command = new ArrayList<>();
        command.add(pgDumpBinary);
        command.add("--dbname=" + target.database());
        command.add("--host=" + target.host());
        command.add("--port=" + target.port());
        command.add("--username=" + username);
        command.add("--no-owner");
        command.add("--no-acl");
        command.add("--clean");
        command.add("--if-exists");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("PGPASSWORD", password == null ? "" : password);
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();
            try (InputStream stdout = process.getInputStream();
                    OutputStream fileOut = Files.newOutputStream(out);
                    GZIPOutputStream gzip = new GZIPOutputStream(fileOut)) {
                stdout.transferTo(gzip);
            }
            String stderr = new String(process.getErrorStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                Files.deleteIfExists(out);
                throw new DomainException("pg_dump timed out after 30 minutes");
            }
            if (process.exitValue() != 0) {
                Files.deleteIfExists(out);
                throw new DomainException("pg_dump failed (exit " + process.exitValue() + "): " + stderr.trim());
            }
            if (!Files.isRegularFile(out) || Files.size(out) == 0) {
                Files.deleteIfExists(out);
                throw new DomainException("pg_dump produced an empty dump file");
            }
            return out;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            try {
                Files.deleteIfExists(out);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
            throw new DomainException("pg_dump failed: " + ex.getMessage());
        }
    }

    record JdbcPostgresTarget(String host, int port, String database) {

        static JdbcPostgresTarget parse(String jdbcUrl) {
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new DomainException("spring.datasource.url is required for database backups");
            }
            String normalized = jdbcUrl.trim();
            String prefix = "jdbc:postgresql://";
            if (!normalized.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                throw new DomainException("Unsupported JDBC URL for backups (expected jdbc:postgresql://…): "
                        + jdbcUrl);
            }
            String rest = normalized.substring(prefix.length());
            int slash = rest.indexOf('/');
            if (slash < 0) {
                throw new DomainException("JDBC URL missing database name: " + jdbcUrl);
            }
            String hostPort = rest.substring(0, slash);
            String dbAndParams = rest.substring(slash + 1);
            String database = dbAndParams;
            int q = dbAndParams.indexOf('?');
            if (q >= 0) {
                database = dbAndParams.substring(0, q);
            }
            if (database.isBlank()) {
                throw new DomainException("JDBC URL missing database name: " + jdbcUrl);
            }

            String host = hostPort;
            int port = 5432;
            if (hostPort.startsWith("[")) {
                int end = hostPort.indexOf(']');
                if (end < 0) {
                    throw new DomainException("Invalid IPv6 host in JDBC URL: " + jdbcUrl);
                }
                host = hostPort.substring(1, end);
                if (end + 1 < hostPort.length() && hostPort.charAt(end + 1) == ':') {
                    port = Integer.parseInt(hostPort.substring(end + 2));
                }
            } else {
                int colon = hostPort.lastIndexOf(':');
                if (colon >= 0) {
                    host = hostPort.substring(0, colon);
                    port = Integer.parseInt(hostPort.substring(colon + 1));
                }
            }
            if (host.isBlank()) {
                throw new DomainException("JDBC URL missing host: " + jdbcUrl);
            }
            return new JdbcPostgresTarget(host, port, database);
        }
    }
}
