package com.atlas.api.dto.request;

import com.atlas.domain.project.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectRequest(
        @NotBlank String name, String description, @NotNull ProjectStatus status) {}
