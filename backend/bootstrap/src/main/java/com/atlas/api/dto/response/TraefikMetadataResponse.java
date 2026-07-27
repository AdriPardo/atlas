package com.atlas.api.dto.response;

import java.util.Map;

public record TraefikMetadataResponse(
        String routerName,
        String rule,
        String entryPoints,
        boolean tls,
        String certResolver,
        Map<String, String> labels) {}
