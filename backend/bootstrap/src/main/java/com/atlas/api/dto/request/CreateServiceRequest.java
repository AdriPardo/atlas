package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateServiceRequest(
        String name,
        @NotBlank String repositoryUrl,
        @NotBlank String branch,
        @NotBlank String composePath,
        String domain,
        String environment) {}
