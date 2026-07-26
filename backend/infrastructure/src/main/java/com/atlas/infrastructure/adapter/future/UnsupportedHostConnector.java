package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.HostConnectorPort;
import com.atlas.domain.host.Host;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "false")
public class UnsupportedHostConnector implements HostConnectorPort {

    @Override
    public HostInspection inspect(Host host) {
        throw new UnsupportedOperationException(
                "Host connectivity is disabled (atlas.adapters.real-enabled=false). Host id: " + host.getId());
    }
}
