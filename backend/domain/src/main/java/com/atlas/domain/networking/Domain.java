package com.atlas.domain.networking;

import com.atlas.domain.shared.DomainException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Getter;

@Getter
public class Domain {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern HOSTNAME = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))*$");

    private final UUID id;
    private final UUID projectId;
    private UUID serviceId;
    private String hostname;
    private DomainStatus status;
    private String verificationToken;
    private String certificateIssuer;
    private Instant certificateExpiresAt;
    private String certificateSans;
    private Instant verifiedAt;
    private String lastError;
    private final Instant createdAt;
    private Instant updatedAt;

    private Domain(
            UUID id,
            UUID projectId,
            UUID serviceId,
            String hostname,
            DomainStatus status,
            String verificationToken,
            String certificateIssuer,
            Instant certificateExpiresAt,
            String certificateSans,
            Instant verifiedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.projectId = Objects.requireNonNull(projectId, "projectId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(
                serviceId,
                hostname,
                status,
                verificationToken,
                certificateIssuer,
                certificateExpiresAt,
                certificateSans,
                verifiedAt,
                lastError,
                updatedAt);
    }

    public static Domain create(UUID projectId, String hostname, UUID serviceId) {
        Instant now = Instant.now();
        return new Domain(
                UUID.randomUUID(),
                projectId,
                serviceId,
                hostname,
                DomainStatus.PENDING_DNS,
                generateVerificationToken(),
                null,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    public static Domain rehydrate(
            UUID id,
            UUID projectId,
            UUID serviceId,
            String hostname,
            DomainStatus status,
            String verificationToken,
            String certificateIssuer,
            Instant certificateExpiresAt,
            String certificateSans,
            Instant verifiedAt,
            String lastError,
            Instant createdAt,
            Instant updatedAt) {
        return new Domain(
                id,
                projectId,
                serviceId,
                hostname,
                status,
                verificationToken,
                certificateIssuer,
                certificateExpiresAt,
                certificateSans,
                verifiedAt,
                lastError,
                createdAt,
                updatedAt);
    }

    public void update(String hostname, UUID serviceId) {
        apply(
                serviceId,
                hostname,
                this.status,
                this.verificationToken,
                this.certificateIssuer,
                this.certificateExpiresAt,
                this.certificateSans,
                this.verifiedAt,
                this.lastError,
                Instant.now());
    }

    /**
     * Control-plane verify stub: marks DNS ownership accepted and seeds certificate metadata.
     * Real TXT/CNAME checks land in a later DNS provider adapter.
     */
    public void markVerified() {
        Instant now = Instant.now();
        this.status = DomainStatus.ACTIVE;
        this.verifiedAt = now;
        this.lastError = null;
        this.certificateIssuer = "letsencrypt-stub";
        this.certificateExpiresAt = now.plus(90, ChronoUnit.DAYS);
        this.certificateSans = this.hostname;
        this.updatedAt = now;
    }

    public void markError(String message) {
        this.status = DomainStatus.ERROR;
        this.lastError = requireText(message, "lastError");
        this.updatedAt = Instant.now();
    }

    public String dnsTxtName() {
        return "_atlas-challenge." + hostname;
    }

    public String dnsTxtValue() {
        return verificationToken;
    }

    private void apply(
            UUID serviceId,
            String hostname,
            DomainStatus status,
            String verificationToken,
            String certificateIssuer,
            Instant certificateExpiresAt,
            String certificateSans,
            Instant verifiedAt,
            String lastError,
            Instant updatedAt) {
        this.serviceId = serviceId;
        this.hostname = normalizeHostname(hostname);
        this.status = Objects.requireNonNull(status, "status is required");
        this.verificationToken = requireText(verificationToken, "verificationToken");
        this.certificateIssuer = blankToNull(certificateIssuer);
        this.certificateExpiresAt = certificateExpiresAt;
        this.certificateSans = blankToNull(certificateSans);
        this.verifiedAt = verifiedAt;
        this.lastError = blankToNull(lastError);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String normalizeHostname(String hostname) {
        String value = requireText(hostname, "hostname").toLowerCase(Locale.ROOT);
        if (!HOSTNAME.matcher(value).matches()) {
            throw new DomainException("hostname is invalid");
        }
        return value;
    }

    private static String generateVerificationToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return "atlas-verify-" + HexFormat.of().formatHex(bytes);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainException(field + " is required");
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
