package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateDeploymentRequest(@NotNull UUID applicationId, @NotNull UUID hostId) {}
