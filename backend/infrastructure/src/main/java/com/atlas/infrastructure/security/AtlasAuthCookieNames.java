package com.atlas.infrastructure.security;

/** Session cookie shared by SSO mint and the SPA API client. */
public final class AtlasAuthCookieNames {

    /** Must match {@code frontend/src/shared/api/tokenStorage.ts}. */
    public static final String TOKEN = "atlas.token";

    private AtlasAuthCookieNames() {}
}
