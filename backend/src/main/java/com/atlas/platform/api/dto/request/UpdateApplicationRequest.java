package com.atlas.platform.api.dto.request;

import com.atlas.platform.domain.model.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateApplicationRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 500) String repositoryUrl,
        @NotBlank @Size(max = 120) String branch,
        @NotBlank @Size(max = 255) String composePath,
        @Size(max = 255) String domain,
        @NotNull ApplicationStatus status) {}
