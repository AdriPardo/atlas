package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Update pipeline. {@code hostId} optional — null clears pin (Autopilot on run). */
public record UpdatePipelineRequest(
        @NotBlank @Size(max = 150) String name, @NotNull UUID serviceId, UUID hostId) {}
