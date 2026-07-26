package com.atlas.application.auth;

public record AuthenticationResult(String accessToken, String tokenType, long expiresIn) {}
