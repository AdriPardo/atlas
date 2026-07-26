package com.atlas.platform.api.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp, int status, String code, String message, String path, List<String> details) {}
