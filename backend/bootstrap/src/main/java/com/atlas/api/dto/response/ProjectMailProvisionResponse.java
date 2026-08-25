package com.atlas.api.dto.response;

public record ProjectMailProvisionResponse(String from, String host, int port, boolean tls, boolean rotated) {}
