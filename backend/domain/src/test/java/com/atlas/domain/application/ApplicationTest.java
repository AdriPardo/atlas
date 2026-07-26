package com.atlas.domain.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.atlas.domain.shared.DomainException;
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
}
