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

    @Test
    void persistsExplicitCapabilities() {
        Host host = Host.create(
                "k8s-node",
                "10.0.0.5",
                "linux",
                "",
                true,
                ConnectionType.SSH,
                "root",
                22,
                null,
                Set.of(RuntimeCapability.K8S, RuntimeCapability.COMPOSE));

        assertTrue(host.supportsRuntime(RuntimeCapability.K8S));
        assertTrue(host.supportsRuntime(RuntimeCapability.COMPOSE));
        assertEquals(Set.of("k8s", "compose"), host.runtimeCapabilityTags());
    }

    @Test
    void replaceRuntimeCapabilitiesUpdatesTags() {
        Host host = Host.create("local", "127.0.0.1", "linux", "26", true, ConnectionType.LOCAL, null, 22, null);
        host.replaceRuntimeCapabilities(Set.of(RuntimeCapability.PODMAN));

        assertEquals(Set.of(RuntimeCapability.PODMAN), host.runtimeCapabilities());
        assertFalse(host.supportsRuntime(RuntimeCapability.COMPOSE));
    }
}
