package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnableAutoDeployRequest(
        @NotNull UUID serviceId, UUID hostId, String publicBaseUrl) {}
