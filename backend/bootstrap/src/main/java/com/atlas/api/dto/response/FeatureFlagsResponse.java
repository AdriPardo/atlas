package com.atlas.api.dto.response;

import java.util.Map;

public record FeatureFlagsResponse(String planCode, Map<String, Boolean> flags) {}
