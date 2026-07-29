package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.HostCommandPort;
import com.atlas.domain.shared.DomainException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "false")
public class UnsupportedHostCommand implements HostCommandPort {

    @Override
    public void run(HostCommand command) {
        throw new DomainException(
                "Host commands are disabled (atlas.adapters.real-enabled=false). Command: "
                        + command.shellCommand());
    }
}
