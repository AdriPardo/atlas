package com.atlas.application.port.out;

import java.util.Optional;

/**
 * Managed web SQL console base URL (pgweb behind Authentik). Empty = feature off.
 */
public interface DbConsoleConfigPort {

    /** Normalized public base URL with trailing slash, if configured. */
    Optional<String> publicBaseUrl();

    /**
     * Shared secret for pgweb {@code --connect-token}. Empty = Connect Backend redeem disabled.
     */
    Optional<String> connectToken();
}
