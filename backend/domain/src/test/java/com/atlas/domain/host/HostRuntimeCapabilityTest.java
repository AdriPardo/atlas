package com.atlas.domain.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.runtime.RuntimeCapability;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HostRuntimeCapabilityTest {

    @Test
    void defaultsToComposeCapability() {
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);

        assertEquals(Set.of(RuntimeCapability.COMPOSE), host.runtimeCapabilities());
        assertTrue(host.supportsRuntime(RuntimeCapability.COMPOSE));
        assertFalse(host.supportsRuntime(RuntimeCapability.K8S));
        assertEquals(Set.of("compose"), host.runtimeCapabilityTags());
    }
}
