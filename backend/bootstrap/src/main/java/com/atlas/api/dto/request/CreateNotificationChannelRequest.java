package com.atlas.api.dto.request;

import com.atlas.domain.observability.NotificationChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationChannelRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull NotificationChannelType type,
        @NotBlank @Size(max = 512) String target) {}
