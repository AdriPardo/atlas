package com.atlas.api.dto.request;

import com.atlas.domain.observability.AlertEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateAlertRuleRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull AlertEventType eventType,
        UUID projectId,
        @NotNull UUID channelId) {}
