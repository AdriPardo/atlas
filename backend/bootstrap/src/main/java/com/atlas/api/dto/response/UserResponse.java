package com.atlas.api.dto.response;

import com.atlas.domain.user.Role;
import java.util.UUID;

public record UserResponse(UUID id, String username, Role role) {}
