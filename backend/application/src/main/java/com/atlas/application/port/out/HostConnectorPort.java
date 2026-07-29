package com.atlas.application.port.out;

import com.atlas.domain.host.Host;
import java.util.Set;

/**
 * Inspects a registered host (LOCAL Docker engine or SSH).
 */
public interface HostConnectorPort {

    HostInspection inspect(Host host);

    /**
     * @param detectedRuntimeCapabilities wire tags ({@code compose}, {@code podman}, …). Empty means
     *     "unknown / do not overwrite" (unreachable or probe inconclusive).
     */
    record HostInspection(
            String reportedHostname,
            String operatingSystem,
            String dockerVersion,
            boolean reachable,
            Set<String> detectedRuntimeCapabilities) {

        public HostInspection {
            detectedRuntimeCapabilities =
                    detectedRuntimeCapabilities == null ? Set.of() : Set.copyOf(detectedRuntimeCapabilities);
        }
    }
}
