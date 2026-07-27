package com.atlas.domain.networking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainTest {

    @Test
    void createStartsPendingWithChallenge() {
        Domain domain = Domain.create(UUID.randomUUID(), "App.Example.com", null);
        assertEquals("app.example.com", domain.getHostname());
        assertEquals(DomainStatus.PENDING_DNS, domain.getStatus());
        assertTrue(domain.getVerificationToken().startsWith("atlas-verify-"));
        assertEquals("_atlas-challenge.app.example.com", domain.dnsTxtName());
    }

    @Test
    void markVerifiedSeedsCertificateMetadata() {
        Domain domain = Domain.create(UUID.randomUUID(), "api.atlas.local", null);
        domain.markVerified();
        assertEquals(DomainStatus.ACTIVE, domain.getStatus());
        assertNotNull(domain.getVerifiedAt());
        assertEquals("letsencrypt-stub", domain.getCertificateIssuer());
        assertEquals("api.atlas.local", domain.getCertificateSans());
        assertNotNull(domain.getCertificateExpiresAt());
    }

    @Test
    void rejectsInvalidHostname() {
        assertThrows(DomainException.class, () -> Domain.create(UUID.randomUUID(), "-bad", null));
    }
}
