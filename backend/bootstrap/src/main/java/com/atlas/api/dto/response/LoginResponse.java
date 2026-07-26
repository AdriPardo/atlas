package com.atlas.api.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}
