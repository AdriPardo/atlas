package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSecretRequest(
        @NotBlank @Size(max = 255) String name, @NotBlank String value) {}
