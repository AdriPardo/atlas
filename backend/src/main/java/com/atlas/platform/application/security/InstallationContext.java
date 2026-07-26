package com.atlas.platform.application.security;

import java.util.UUID;

public final class InstallationContext {

    public static final UUID DEFAULT_INSTALLATION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private InstallationContext() {}

    public static UUID currentInstallationId() {
        return DEFAULT_INSTALLATION_ID;
    }
}
