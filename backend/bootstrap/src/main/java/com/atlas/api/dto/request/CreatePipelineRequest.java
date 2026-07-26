package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreatePipelineRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 150) String name,
        @NotNull UUID serviceId,
        @NotNull UUID hostId) {}
