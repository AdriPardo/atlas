package com.atlas.api.dto.response;

public record MailSettingsResponse(
        boolean configured,
        String host,
        int port,
        String fromDomain,
        boolean tls,
        boolean auth,
        int dailySendLimitPerProject) {}
