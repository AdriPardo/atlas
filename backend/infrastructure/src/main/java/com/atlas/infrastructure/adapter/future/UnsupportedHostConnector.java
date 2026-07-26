package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.HostConnectorPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedHostConnector implements HostConnectorPort {

    @Override
    public HostConnectionInfo connect(UUID hostId) {
        throw new UnsupportedOperationException(
                "Host connectivity is not implemented in the MVP. Host id: " + hostId);
    }
}
