package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.RuntimeOrchestratorPort;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.shared.DomainException;
import org.springframework.stereotype.Component;

/**
 * Routes {@link RuntimeOrchestratorPort} calls by {@link RuntimeCapability}.
 * Compose remains the default path; Podman is opt-in ({@code runtime.kind: podman-compose}).
 */
@Component
public class RoutingRuntimeOrchestratorAdapter implements RuntimeOrchestratorPort {

    private final ComposeRuntimeOrchestratorAdapter compose;
    private final PodmanRuntimeOrchestratorAdapter podman;

    public RoutingRuntimeOrchestratorAdapter(ContainerRuntimePort containerRuntime) {
        this.compose = new ComposeRuntimeOrchestratorAdapter(containerRuntime);
        this.podman = new PodmanRuntimeOrchestratorAdapter(containerRuntime);
    }

    RoutingRuntimeOrchestratorAdapter(
            ComposeRuntimeOrchestratorAdapter compose, PodmanRuntimeOrchestratorAdapter podman) {
        this.compose = compose;
        this.podman = podman;
    }

    @Override
    public void apply(RuntimeApplyCommand command) {
        delegate(command.capability()).apply(command);
    }

    @Override
    public void teardown(RuntimeTeardownCommand command) {
        delegate(command.capability()).teardown(command);
    }

    private RuntimeOrchestratorPort delegate(RuntimeCapability capability) {
        if (capability == RuntimeCapability.COMPOSE) {
            return compose;
        }
        if (capability == RuntimeCapability.PODMAN) {
            return podman;
        }
        throw new DomainException("No runtime orchestrator for capability: " + capability.tag());
    }
}
