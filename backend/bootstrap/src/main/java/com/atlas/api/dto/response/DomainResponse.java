package com.atlas.api.dto.response;

import com.atlas.domain.networking.DomainStatus;
import java.time.Instant;
import java.util.UUID;

public record DomainResponse(
        UUID id,
        UUID projectId,
        UUID serviceId,
        String hostname,
        DomainStatus status,
        String verificationToken,
        String dnsTxtName,
        String dnsTxtValue,
        String certificateIssuer,
        Instant certificateExpiresAt,
        String certificateSans,
        Instant verifiedAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt) {}
