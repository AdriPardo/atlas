package com.atlas.infrastructure.adapter.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.runtime.ContainerSnapshot;
import com.atlas.infrastructure.config.AtlasProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessContainerRuntimeAdapterParseTest {

    private final ProcessContainerRuntimeAdapter adapter = new ProcessContainerRuntimeAdapter(
            new ProcessCommandRunner(), null, new AtlasProperties(), new ObjectMapper());

    @Test
    void parsesDockerPsJsonLines() {
        String output =
                """
                {"ID":"abc123","Names":"nginx","Image":"nginx:latest","State":"running","Status":"Up 2 hours","Ports":"0.0.0.0:80->80/tcp","Labels":"com.docker.compose.service=web"}
                {"ID":"def456","Names":"/redis","Image":"redis:7","State":"exited","Status":"Exited (0) 1 day ago","Ports":"","Labels":""}
                noise line
                """;

        List<ContainerSnapshot> containers = adapter.parseContainerSnapshots(output);

        assertEquals(2, containers.size());
        assertEquals("abc123", containers.get(0).id());
        assertEquals("nginx", containers.get(0).name());
        assertEquals("running", containers.get(0).state());
        assertEquals("def456", containers.get(1).id());
        assertEquals("redis", containers.get(1).name());
        assertEquals("exited", containers.get(1).state());
    }

    @Test
    void emptyOutputYieldsEmptyList() {
        assertTrue(adapter.parseContainerSnapshots("").isEmpty());
        assertTrue(adapter.parseContainerSnapshots(null).isEmpty());
    }
}
