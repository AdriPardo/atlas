package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateDomainRequest(
        @NotBlank @Size(max = 253) String hostname, UUID serviceId) {}
