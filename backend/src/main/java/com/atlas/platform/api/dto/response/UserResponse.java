package com.atlas.platform.api.dto.response;

import com.atlas.platform.domain.model.Role;
import java.util.UUID;

public record UserResponse(UUID id, String username, Role role, UUID installationId) {}
