package com.atlas.domain.observability;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class NotificationChannel {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UUID id;
    private String name;
    private NotificationChannelType type;
    private String target;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    private NotificationChannel(
            UUID id,
            String name,
            NotificationChannelType type,
            String target,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, type, target, enabled, updatedAt);
    }

    public static NotificationChannel create(String name, NotificationChannelType type, String target) {
        Instant now = Instant.now();
        return new NotificationChannel(UUID.randomUUID(), name, type, target, true, now, now);
    }

    public static NotificationChannel rehydrate(
            UUID id,
            String name,
            NotificationChannelType type,
            String target,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
        return new NotificationChannel(id, name, type, target, enabled, createdAt, updatedAt);
    }

    public void update(String name, NotificationChannelType type, String target, Boolean enabled) {
        apply(
                name,
                type,
                target,
                enabled == null ? this.enabled : enabled,
                Instant.now());
    }

    private void apply(
            String name,
            NotificationChannelType type,
            String target,
            boolean enabled,
            Instant updatedAt) {
        this.name = requireText(name, "name");
        if (this.name.length() > 128) {
            throw new DomainException("name must be <= 128 characters");
        }
        this.type = Objects.requireNonNull(type, "type is required");
        this.target = normalizeTarget(type, target);
        this.enabled = enabled;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String normalizeTarget(NotificationChannelType type, String target) {
        String value = requireText(target, "target");
        if (value.length() > 512) {
            throw new DomainException("target must be <= 512 characters");
        }
        return switch (type) {
            case WEBHOOK -> normalizeWebhook(value);
            case EMAIL -> normalizeEmail(value);
        };
    }

    private static String normalizeWebhook(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("stub://"))) {
            throw new DomainException("webhook target must be an http(s) or stub:// URL");
        }
        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new DomainException("email target is invalid");
        }
        return email;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
