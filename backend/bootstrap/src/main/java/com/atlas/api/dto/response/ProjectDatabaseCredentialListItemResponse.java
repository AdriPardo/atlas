package com.atlas.api.dto.response;

import java.time.Instant;

public record ProjectDatabaseCredentialListItemResponse(
        String role, Instant expiresAt, boolean expired) {}
