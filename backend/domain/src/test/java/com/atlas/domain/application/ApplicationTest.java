package com.atlas.domain.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atlas.domain.shared.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationTest {

    @Test
    void createSetsRegisteredStatus() {
        Application application = Application.create(
                "billing", "Billing service", "https://git.example/billing.git", "main", "./docker-compose.yml", "billing.local");

        assertEquals(ApplicationStatus.REGISTERED, application.getStatus());
        assertEquals("billing", application.getName());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(
                DomainException.class,
                () -> Application.create(" ", "desc", "https://git.example/a.git", "main", "./compose.yml", ""));
    }

    @Test
    void rehydrateAllowsBlankComposePathForAtlasYmlProjects() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Application application = Application.rehydrate(
                id,
                "reelpath",
                "desc",
                "https://git.example/reelpath.git",
                "main",
                null,
                "",
                ApplicationStatus.REGISTERED,
                now,
                now);

        assertEquals("", application.getComposePath());
        assertFalse(application.hasComposePath());
    }

    @Test
    void createAllowsBlankComposePath() {
        Application application = Application.create(
                "billing", "Billing", "https://git.example/billing.git", "main", "  ", "billing.local");

        assertEquals("", application.getComposePath());
        assertFalse(application.hasComposePath());
    }
}
