package com.atlas.platform.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String repositoryUrl,
        @Size(max = 120) String branch,
        @Size(max = 255) String composePath,
        @Size(max = 255) String domain) {}
