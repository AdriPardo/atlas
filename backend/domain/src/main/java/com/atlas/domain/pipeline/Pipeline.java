package com.atlas.domain.pipeline;

import com.atlas.domain.shared.DomainException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class Pipeline {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UUID id;
    private final UUID projectId;
    private String name;
    private UUID serviceId;
    private UUID hostId;
    private String webhookToken;
    private final Instant createdAt;
    private Instant updatedAt;

    private Pipeline(
            UUID id,
            UUID projectId,
            String name,
            UUID serviceId,
            UUID hostId,
            String webhookToken,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(name, serviceId, hostId, requireToken(webhookToken), updatedAt);
    }

    public static Pipeline create(UUID projectId, String name, UUID serviceId, UUID hostId) {
        Instant now = Instant.now();
        return new Pipeline(
                UUID.randomUUID(), projectId, name, serviceId, hostId, generateWebhookToken(), now, now);
    }

    public static Pipeline rehydrate(
            UUID id,
            UUID projectId,
            String name,
            UUID serviceId,
            UUID hostId,
            String webhookToken,
            Instant createdAt,
            Instant updatedAt) {
        return new Pipeline(id, projectId, name, serviceId, hostId, webhookToken, createdAt, updatedAt);
    }

    public void update(String name, UUID serviceId, UUID hostId) {
        apply(name, serviceId, hostId, this.webhookToken, Instant.now());
    }

    public void rotateWebhookToken() {
        this.webhookToken = generateWebhookToken();
        this.updatedAt = Instant.now();
    }

    private void apply(String name, UUID serviceId, UUID hostId, String webhookToken, Instant updatedAt) {
        this.name = requireText(name, "name");
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId is required");
        // null hostId = Autopilot placement on each run (SHARED default)
        this.hostId = hostId;
        this.webhookToken = webhookToken;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static String generateWebhookToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return "atk_" + HexFormat.of().formatHex(bytes);
    }

    private static String requireToken(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException("webhookToken is required");
        }
        return value.trim();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
        }
        return value.trim();
    }
}
