package com.atlas.api.dto.response;

import java.util.UUID;

public record ApiKeyCreatedResponse(UUID id, String name, String prefix, String key) {}
