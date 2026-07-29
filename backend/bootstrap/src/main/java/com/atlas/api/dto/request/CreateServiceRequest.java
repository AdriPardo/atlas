package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceRequest(
        String name,
        @NotBlank String repositoryUrl,
        @NotBlank String branch,
        @Size(max = 500) String composePath,
        String domain,
        String environment) {}
