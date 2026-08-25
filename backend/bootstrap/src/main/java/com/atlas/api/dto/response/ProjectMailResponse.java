package com.atlas.api.dto.response;

public record ProjectMailResponse(
        boolean provisionerConfigured,
        boolean provisioned,
        String from,
        String host,
        Integer port,
        boolean tls,
        String message,
        int remainingSendsToday,
        int dailySendLimit) {}
