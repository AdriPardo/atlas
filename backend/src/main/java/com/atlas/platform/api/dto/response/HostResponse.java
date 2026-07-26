package com.atlas.platform.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HostResponse(
        UUID id,
        String hostname,
        String ip,
        String operatingSystem,
        String dockerVersion,
        boolean online,
        Instant createdAt) {}
