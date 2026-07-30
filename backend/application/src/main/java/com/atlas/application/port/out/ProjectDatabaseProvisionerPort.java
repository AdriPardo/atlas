package com.atlas.application.port.out;

/**
 * Provisions a per-project Postgres schema + migrator role (ADR-0015 slice 1).
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

    record ProvisionRequest(String schema, String role, String password) {}

    record ProvisionResult(String schema, String role, String databaseName, String connectionUrl) {}
}
