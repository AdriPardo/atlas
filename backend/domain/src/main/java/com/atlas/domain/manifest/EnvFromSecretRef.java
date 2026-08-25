package com.atlas.domain.manifest;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Manifest {@code envFrom} entry that references a logical project secret (ADR-0014 / ADR-0015).
 * Deploy materializes the value into the workspace {@code .env} for Compose interpolation.
 */
public final class EnvFromSecretRef {

    private final String secretRef;
    private final String envKey;

    public EnvFromSecretRef(String secretRef, String envKey) {
        this.secretRef = requireText(secretRef, "secretRef");
        this.envKey = blankToNull(envKey);
    }

    public static EnvFromSecretRef of(String secretRef) {
        return new EnvFromSecretRef(secretRef, null);
    }

    public String getSecretRef() {
        return secretRef;
    }

    /** Explicit Compose/env key; empty → platform mapping (e.g. {@code db.url} → {@code DATABASE_URL}). */
    public Optional<String> getEnvKey() {
        return Optional.ofNullable(envKey);
    }

    /**
     * Target key written to {@code .env}. Explicit {@code env:} wins; otherwise known mappings /
     * SCREAMING_SNAKE from the logical secret name.
     */
    public String resolveEnvKey() {
        if (envKey != null) {
            return envKey;
        }
        String normalized = secretRef.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "db.url" -> "DATABASE_URL";
            case "db.schema" -> "DB_SCHEMA";
            case "db.password" -> "DB_PASSWORD";
            // Known app-secret mappings (user-owned in Atlas → env via envFrom)
            case "ai.openai" -> "OPENAI_API_KEY";
            case "ai.openai.base_url" -> "OPENAI_BASE_URL";
            case "ai.elevenlabs" -> "ELEVENLABS_API_KEY";
            case "ai.deepseek" -> "DEEPSEEK_API_KEY";
            case "ai.provider" -> "AI_PROVIDER";
            case "ai.api_key" -> "AI_API_KEY";
            case "ai.base_url" -> "AI_BASE_URL";
            case "smtp.host" -> "SMTP_HOST";
            case "smtp.port" -> "SMTP_PORT";
            case "smtp.user" -> "SMTP_USER";
            case "smtp.password" -> "SMTP_PASSWORD";
            case "smtp.from" -> "SMTP_FROM";
            case "smtp.tls" -> "SMTP_TLS";
            case "mail.api_token" -> "MAIL_API_TOKEN";
            default -> secretRef.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvFromSecretRef that)) {
            return false;
        }
        return secretRef.equals(that.secretRef) && Objects.equals(envKey, that.envKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretRef, envKey);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
