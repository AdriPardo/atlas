package com.atlas.platform.application.security;

import com.atlas.platform.domain.model.Role;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String username, Role role, UUID installationId) {}
