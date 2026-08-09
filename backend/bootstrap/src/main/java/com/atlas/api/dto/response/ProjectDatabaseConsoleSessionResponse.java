package com.atlas.api.dto.response;

public record ProjectDatabaseConsoleSessionResponse(
        String consoleUrl,
        String schema,
        String database,
        String server,
        String role,
        String profile,
        java.time.Instant expiresAt,
        int ttlMinutes) {}
