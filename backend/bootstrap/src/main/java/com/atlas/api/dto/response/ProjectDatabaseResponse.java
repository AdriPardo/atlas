package com.atlas.api.dto.response;

public record ProjectDatabaseResponse(
        boolean provisionerConfigured,
        boolean provisioned,
        String schema,
        String role,
        String databaseName,
        String profile,
        String message,
        boolean consoleConfigured,
        String consoleUrl) {}
