package com.atlas.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record IssueProjectDatabaseCredentialRequest(
        @Size(max = 32) String profile,
        @Min(5) @Max(1440) Integer ttlMinutes) {}
