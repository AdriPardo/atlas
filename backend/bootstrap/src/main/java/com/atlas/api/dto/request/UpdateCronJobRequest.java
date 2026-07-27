package com.atlas.api.dto.request;

import com.atlas.domain.cron.CronTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateCronJobRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 128) String cronExpression,
        @NotNull CronTargetType targetType,
        UUID targetId,
        Boolean enabled) {}
