package com.atlas.api.dto.request;

import com.atlas.domain.host.ConnectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateHostRequest(
        @NotBlank @Size(max = 255) String hostname,
        @NotBlank @Size(max = 64) String ip,
        @NotBlank @Size(max = 150) String operatingSystem,
        @Size(max = 100) String dockerVersion,
        boolean online,
        ConnectionType connectionType,
        @Size(max = 128) String sshUser,
        Integer sshPort,
        UUID sshPrivateKeySecretId) {}
