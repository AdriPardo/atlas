package com.atlas.platform.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "atlas.security.jwt")
public record JwtProperties(String secret, long expirationMinutes) {}
