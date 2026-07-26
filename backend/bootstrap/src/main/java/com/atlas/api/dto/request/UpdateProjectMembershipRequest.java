package com.atlas.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProjectMembershipRequest(@NotBlank String role) {}
