package com.atlas.application.port.out;

/**
 * Provisions per-project SMTP credentials stored as project secrets (ADR-0018).
 *
 * <p>Uses the platform relay configured in {@code atlas.app-smtp.*}. Missing host → not configured.
 */
public interface ProjectSmtpProvisionerPort {

    boolean isConfigured();

    /** Hostname Atlas control-plane uses to reach the relay (docker DNS, e.g. {@code smtp}). */
    String host();

    /**
     * Hostname written into app secrets / {@code SMTP_HOST}. Blank config → same as {@link #host()}.
     * Use a LAN/public name when apps run outside {@code atlas-internal}.
     */
    String hostForApps();

    int port();

    boolean tls();

    boolean auth();

    String fromDomain();

    int dailySendLimitPerProject();

    /** When true, every deploy writes {@code SMTP_*} into workspace {@code .env} (ADR-0018). */
    boolean autoInjectOnDeploy();

    ProvisionResult provision(ProvisionRequest request);

    record ProvisionRequest(String projectSlug, String relayPassword, String apiToken) {}

    record ProvisionResult(
            String host,
            int port,
            String username,
            String password,
            String from,
            boolean tls,
            String apiToken) {}
}
