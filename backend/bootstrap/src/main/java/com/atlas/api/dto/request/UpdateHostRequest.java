package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHostRequest(
        @NotBlank @Size(max = 255) String hostname,
        @NotBlank @Size(max = 64) String ip,
        @NotBlank @Size(max = 150) String operatingSystem,
        @Size(max = 100) String dockerVersion,
        boolean online) {}
