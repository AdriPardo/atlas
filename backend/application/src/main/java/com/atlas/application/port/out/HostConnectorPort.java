package com.atlas.application.port.out;

import java.util.UUID;

/**
 * Future port for SSH/remote host connectivity. Not used by MVP use cases.
 */
public interface HostConnectorPort {

    HostConnectionInfo connect(UUID hostId);

    record HostConnectionInfo(UUID hostId, String sessionId) {}
}
