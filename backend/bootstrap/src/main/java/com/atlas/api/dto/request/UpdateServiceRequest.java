package com.atlas.api.dto.request;

import com.atlas.domain.service.ServiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateServiceRequest(
        @NotBlank String name,
        @NotBlank String repositoryUrl,
        @NotBlank String branch,
        @Size(max = 500) String composePath,
        String domain,
        String environment,
        @NotNull ServiceStatus status) {}
