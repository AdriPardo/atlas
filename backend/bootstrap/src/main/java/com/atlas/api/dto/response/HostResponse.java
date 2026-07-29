package com.atlas.api.dto.response;

import com.atlas.domain.host.ConnectionType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HostResponse(
        UUID id,
        String hostname,
        String ip,
        String operatingSystem,
        String dockerVersion,
        boolean online,
        ConnectionType connectionType,
        String sshUser,
        int sshPort,
        UUID sshPrivateKeySecretId,
        List<String> runtimeCapabilities,
        Instant createdAt,
        Instant updatedAt) {}
