package com.atlas.platform.api.dto.response;

import com.atlas.platform.domain.model.Role;
import java.util.UUID;

public record AuthResponse(String accessToken, String tokenType, UUID userId, String username, Role role) {

    public static AuthResponse bearer(String token, UUID userId, String username, Role role) {
        return new AuthResponse(token, "Bearer", userId, username, role);
    }
}
