package com.atlas.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
