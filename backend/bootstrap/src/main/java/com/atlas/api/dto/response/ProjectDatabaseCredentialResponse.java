package com.atlas.api.dto.response;

import java.time.Instant;

public record ProjectDatabaseCredentialResponse(
        String role, String profile, String connectionUrl, Instant expiresAt, int ttlMinutes) {}
