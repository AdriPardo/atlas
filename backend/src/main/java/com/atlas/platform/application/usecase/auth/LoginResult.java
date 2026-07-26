package com.atlas.platform.application.usecase.auth;

import com.atlas.platform.domain.model.Role;
import java.util.UUID;

public record LoginResult(String accessToken, UUID userId, String username, Role role) {}
