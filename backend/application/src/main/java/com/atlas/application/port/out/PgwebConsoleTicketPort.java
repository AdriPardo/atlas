package com.atlas.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * One-time tickets that pgweb redeems via Connect Backend (no password in browser query string).
 */
public interface PgwebConsoleTicketPort {

    /** Stores {@code connectionUrl} until {@code redeemBy} or first successful redeem. */
    String issue(String connectionUrl, Instant redeemBy);

    /**
     * Consumes ticket exactly once. Empty if unknown, expired, or already used.
     */
    Optional<String> redeem(String resourceId);
}
