package com.atlas.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServiceUnitExposureTest {

    @Test
    void defaultsToPublicAndAcceptsInternal() {
        ServiceUnit service = ServiceUnit.createDefault(
                java.util.UUID.randomUUID(),
                "https://git.example/app.git",
                "main",
                "./docker-compose.yml",
                "");
        assertEquals(ServiceExposure.PUBLIC, service.getExposure());
        service.updateExposure(ServiceExposure.INTERNAL);
        assertEquals(ServiceExposure.INTERNAL, service.getExposure());
    }

    @Test
    void allowsBlankComposePathForManifestDrivenRepos() {
        ServiceUnit service = ServiceUnit.createDefault(
                java.util.UUID.randomUUID(),
                "https://git.example/app.git",
                "main",
                null,
                "app.example.com");
        assertEquals("", service.getComposePath());
        assertFalse(service.hasComposePath());

        service.update(
                service.getName(),
                service.getRepositoryUrl(),
                service.getBranch(),
                "  ",
                service.getDomain(),
                service.getEnvironment(),
                service.getStatus());
        assertEquals("", service.getComposePath());
        assertFalse(service.hasComposePath());

        service.update(
                service.getName(),
                service.getRepositoryUrl(),
                service.getBranch(),
                "./compose.yml",
                service.getDomain(),
                service.getEnvironment(),
                service.getStatus());
        assertTrue(service.hasComposePath());
        assertEquals("./compose.yml", service.getComposePath());
    }
}
