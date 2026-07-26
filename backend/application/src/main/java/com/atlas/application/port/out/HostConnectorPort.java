package com.atlas.application.port.out;

import com.atlas.domain.host.Host;

/**
 * Inspects a registered host (LOCAL Docker engine or SSH).
 */
public interface HostConnectorPort {

    HostInspection inspect(Host host);

    record HostInspection(
            String reportedHostname, String operatingSystem, String dockerVersion, boolean reachable) {}
}
