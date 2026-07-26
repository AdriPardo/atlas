package com.atlas.platform.domain.port.out.future;

import java.util.UUID;

/**
 * Future port for SSH/host connectivity. Not used by the MVP.
 */
public interface HostConnectorPort {

    boolean ping(UUID hostId);
}
