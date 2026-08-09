package com.atlas.application.database;

import com.atlas.application.port.out.DbConsoleConfigPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.database.DatabaseConsoleUrls;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0015 pragmatic console: mint TTL creds + hand the browser a pgweb deep-link (SSO in front).
 */
@Service
@RequiredArgsConstructor
public class OpenProjectDatabaseConsoleUseCase {

    public record ConsoleSession(
            String consoleUrl,
            String schema,
            String database,
            String server,
            String role,
            String profile,
            String connectionUrl,
            Instant expiresAt,
            int ttlMinutes) {}

    private final IssueProjectDatabaseCredentialsUseCase issueCredentials;
    private final DbConsoleConfigPort dbConsoleConfig;
    private final ProjectRepositoryPort projectRepository;

    @Transactional
    public ConsoleSession open(UUID projectId, String profileWire, Integer ttlMinutes) {
        String consoleBase = dbConsoleConfig
                .publicBaseUrl()
                .orElseThrow(() -> new DomainException(
                        "DB console not configured — set ATLAS_DB_CONSOLE_URL (Authentik-gated pgweb)"));

        IssueProjectDatabaseCredentialsUseCase.IssuedCredential issued =
                issueCredentials.issue(projectId, profileWire, ttlMinutes);

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        String schema = ProjectDatabaseNames.schemaName(project.getSlug());
        String connectionUrl = DatabaseConsoleUrls.withSearchPath(issued.connectionUrl(), schema);

        return new ConsoleSession(
                consoleBase,
                schema,
                DatabaseConsoleUrls.databaseName(connectionUrl),
                DatabaseConsoleUrls.serverHostPort(connectionUrl),
                issued.role(),
                issued.profile(),
                connectionUrl,
                issued.expiresAt(),
                issued.ttlMinutes());
    }

    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return dbConsoleConfig.publicBaseUrl().isPresent();
    }

    @Transactional(readOnly = true)
    public String publicUrlOrEmpty() {
        return dbConsoleConfig.publicBaseUrl().orElse("");
    }
}
