package com.atlas.api.dto.response;

public record ProjectDatabaseProvisionResponse(
        String schema, String role, String databaseName, String profile, boolean rotated) {}
