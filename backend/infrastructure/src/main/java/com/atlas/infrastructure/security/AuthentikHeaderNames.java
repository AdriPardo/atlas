package com.atlas.infrastructure.security;

/** Authentik ForwardAuth identity headers (Traefik → nginx → Atlas API). */
public final class AuthentikHeaderNames {

    public static final String USERNAME = "X-authentik-username";
    public static final String GROUPS = "X-authentik-groups";
    public static final String EMAIL = "X-authentik-email";
    public static final String NAME = "X-authentik-name";
    public static final String UID = "X-authentik-uid";

    private AuthentikHeaderNames() {}
}
