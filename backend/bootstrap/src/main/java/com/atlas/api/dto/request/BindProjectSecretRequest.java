package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record BindProjectSecretRequest(
        @NotNull UUID secretId, @Size(max = 255) String alias) {}
