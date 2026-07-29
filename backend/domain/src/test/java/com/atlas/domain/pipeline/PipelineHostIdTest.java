package com.atlas.domain.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PipelineHostIdTest {

    @Test
    void createAllowsNullHostId() {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Pipeline pipeline = Pipeline.create(projectId, "auto-deploy", serviceId, null);
        assertNull(pipeline.getHostId());
        assertEquals(serviceId, pipeline.getServiceId());
    }

    @Test
    void updateCanClearHostPin() {
        UUID projectId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Pipeline pipeline = Pipeline.create(projectId, "deploy", serviceId, hostId);
        assertEquals(hostId, pipeline.getHostId());

        pipeline.update("deploy", serviceId, null);
        assertNull(pipeline.getHostId());
    }
}
