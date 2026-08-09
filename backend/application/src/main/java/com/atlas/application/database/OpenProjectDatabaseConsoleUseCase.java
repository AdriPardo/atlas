package com.atlas.application.database;

import com.atlas.application.port.out.DbConsoleConfigPort;
import com.atlas.application.port.out.PgwebConsoleTicketPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.database.DatabaseConsoleUrls;
import com.atlas.domain.database.ProjectDatabaseNames;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0015 pragmatic console: mint TTL creds + one-time pgweb Connect Backend ticket (SSO in front).
 */
@Service
public class OpenProjectDatabaseConsoleUseCase {

    /** Short window for browser to hit /connect/{id} after Open; Postgres role TTL is separate. */
    private static final int TICKET_REDEEM_MINUTES = 2;

    public record ConsoleSession(
            String consoleUrl,
            String schema,
            String database,
            String server,
            String role,
            String profile,
            Instant expiresAt,
            int ttlMinutes) {}

    private final IssueProjectDatabaseCredentialsUseCase issueCredentials;
    private final DbConsoleConfigPort dbConsoleConfig;
    private final PgwebConsoleTicketPort tickets;
    private final ProjectRepositoryPort projectRepository;
    private final Clock clock;

    @Autowired
    public OpenProjectDatabaseConsoleUseCase(
            IssueProjectDatabaseCredentialsUseCase issueCredentials,
            DbConsoleConfigPort dbConsoleConfig,
            PgwebConsoleTicketPort tickets,
            ProjectRepositoryPort projectRepository) {
        this(issueCredentials, dbConsoleConfig, tickets, projectRepository, Clock.systemUTC());
    }

    OpenProjectDatabaseConsoleUseCase(
            IssueProjectDatabaseCredentialsUseCase issueCredentials,
            DbConsoleConfigPort dbConsoleConfig,
            PgwebConsoleTicketPort tickets,
            ProjectRepositoryPort projectRepository,
            Clock clock) {
        this.issueCredentials = issueCredentials;
        this.dbConsoleConfig = dbConsoleConfig;
        this.tickets = tickets;
        this.projectRepository = projectRepository;
        this.clock = clock;
    }

    @Transactional
    public ConsoleSession open(UUID projectId, String profileWire, Integer ttlMinutes) {
        String consoleBase = dbConsoleConfig
                .publicBaseUrl()
                .orElseThrow(() -> new DomainException(
                        "DB console not configured — set ATLAS_DB_CONSOLE_URL (Authentik-gated pgweb)"));
        if (dbConsoleConfig.connectToken().isEmpty()) {
            throw new DomainException(
                    "DB console connect token not configured — set ATLAS_DB_CONSOLE_CONNECT_TOKEN");
        }

        IssueProjectDatabaseCredentialsUseCase.IssuedCredential issued =
                issueCredentials.issue(projectId, profileWire, ttlMinutes);

        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        String schema = ProjectDatabaseNames.schemaName(project.getSlug());
        String connectionUrl = DatabaseConsoleUrls.withSearchPath(issued.connectionUrl(), schema);

        Instant redeemBy = clock.instant().plus(TICKET_REDEEM_MINUTES, ChronoUnit.MINUTES);
        String resourceId = tickets.issue(connectionUrl, redeemBy);
        String launchUrl = consoleBase + "connect/" + resourceId;

        return new ConsoleSession(
                launchUrl,
                schema,
                DatabaseConsoleUrls.databaseName(connectionUrl),
                DatabaseConsoleUrls.serverHostPort(connectionUrl),
                issued.role(),
                issued.profile(),
                issued.expiresAt(),
                issued.ttlMinutes());
    }

    @Transactional(readOnly = true)
    public boolean isConfigured() {
        return dbConsoleConfig.publicBaseUrl().isPresent() && dbConsoleConfig.connectToken().isPresent();
    }

    @Transactional(readOnly = true)
    public String publicUrlOrEmpty() {
        return dbConsoleConfig.publicBaseUrl().orElse("");
    }
}
