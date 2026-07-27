package com.atlas.api.dto.response;

public record DnsCnameResponse(
        String hostname,
        String zone,
        String recordName,
        String cnameTarget,
        boolean proxied,
        String copyBlock,
        String mode,
        String message) {}
