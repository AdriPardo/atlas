package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
        @NotBlank String name,
        String description,
        @NotBlank String repositoryUrl,
        @NotBlank String branch,
        @NotBlank String composePath,
        String domain) {}
