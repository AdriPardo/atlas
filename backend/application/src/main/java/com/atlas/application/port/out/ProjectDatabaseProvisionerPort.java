package com.atlas.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Provisions per-project Postgres schema + roles and issues TTL credentials (ADR-0015).
 *
 * <p>Never targets the control-plane {@code atlas} database. Missing admin config → not configured.
 */
public interface ProjectDatabaseProvisionerPort {

    boolean isConfigured();

    /** Target database name for customer schemas (never {@code atlas}). */
    String databaseName();

    /** Host:port used when building app connection URLs. */
    String hostPort();

    ProvisionResult provision(ProvisionRequest request);

    /**
     * Creates an ephemeral LOGIN role with {@code VALID UNTIL}, grants matching {@code profile},
     * and returns a one-shot connection URL. Does not mutate migrator password / {@code db.url}.
     */
    CredentialResult issueCredential(CredentialRequest request);

    /** Drops an ephemeral credential role (idempotent). */
    void revokeCredential(String role);

    /** Lists ephemeral credential roles whose name starts with {@code rolePrefix}. */
    List<CredentialInfo> listCredentials(String rolePrefix);

    record ProvisionRequest(String schema, String role, String readRole, String password) {}

    record ProvisionResult(
            String schema, String role, String readRole, String databaseName, String connectionUrl) {}

    record CredentialRequest(
            String schema,
            String migratorRole,
            String readRole,
            String temporaryRole,
            String password,
            Instant validUntil,
            String profile) {}

    record CredentialResult(String role, String profile, String connectionUrl, Instant expiresAt) {}

    record CredentialInfo(String role, Instant expiresAt, boolean expired) {}
}
