package com.atlas.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "domains")
public class DomainJpaEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "service_id")
    private UUID serviceId;

    @Column(nullable = false, length = 253)
    private String hostname;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "verification_token", nullable = false, length = 128)
    private String verificationToken;

    @Column(name = "certificate_issuer", length = 128)
    private String certificateIssuer;

    @Column(name = "certificate_expires_at")
    private Instant certificateExpiresAt;

    @Column(name = "certificate_sans", length = 512)
    private String certificateSans;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "last_error", length = 512)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getCertificateIssuer() {
        return certificateIssuer;
    }

    public void setCertificateIssuer(String certificateIssuer) {
        this.certificateIssuer = certificateIssuer;
    }

    public Instant getCertificateExpiresAt() {
        return certificateExpiresAt;
    }

    public void setCertificateExpiresAt(Instant certificateExpiresAt) {
        this.certificateExpiresAt = certificateExpiresAt;
    }

    public String getCertificateSans() {
        return certificateSans;
    }

    public void setCertificateSans(String certificateSans) {
        this.certificateSans = certificateSans;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
