package com.atlas.application.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.runtime.RuntimeCapability;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuntimeCapabilityDetectorTest {

    @Test
    void dockerOnlyYieldsCompose() {
        assertEquals(Set.of(RuntimeCapability.COMPOSE), RuntimeCapabilityDetector.fromProbe("27.0.3", ""));
        assertEquals(Set.of("compose"), RuntimeCapabilityDetector.tagsFromProbe("27.0.3", null));
    }

    @Test
    void podmanOnlyYieldsPodman() {
        assertEquals(Set.of(RuntimeCapability.PODMAN), RuntimeCapabilityDetector.fromProbe("", "5.2.0"));
    }

    @Test
    void bothYieldComposeAndPodman() {
        assertEquals(
                Set.of(RuntimeCapability.COMPOSE, RuntimeCapability.PODMAN),
                RuntimeCapabilityDetector.fromProbe("27.0.3", "5.2.0"));
    }

    @Test
    void blankOrErrorYieldsEmpty() {
        assertTrue(RuntimeCapabilityDetector.fromProbe("", "").isEmpty());
        assertTrue(RuntimeCapabilityDetector.fromProbe("error: cannot connect", "not found").isEmpty());
        assertTrue(RuntimeCapabilityDetector.fromProbe(null, null).isEmpty());
    }
}
