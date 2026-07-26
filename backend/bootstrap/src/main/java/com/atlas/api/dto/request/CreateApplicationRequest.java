package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String repositoryUrl,
        @NotBlank @Size(max = 200) String branch,
        @NotBlank @Size(max = 500) String composePath,
        @Size(max = 255) String domain) {}
