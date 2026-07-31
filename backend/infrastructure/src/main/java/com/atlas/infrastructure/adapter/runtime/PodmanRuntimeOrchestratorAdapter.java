package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.RuntimeOrchestratorPort;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.shared.DomainException;

/**
 * Podman adapter for {@link RuntimeOrchestratorPort} (ADR-0014).
 * Opt-in via {@code atlas.yml} {@code runtime.kind: podman-compose}.
 * Delegates to {@link ContainerRuntimePort#podmanComposeUp} / {@code podmanComposeDown}.
 * Wired via {@link RoutingRuntimeOrchestratorAdapter}.
 */
public class PodmanRuntimeOrchestratorAdapter implements RuntimeOrchestratorPort {

    private final ContainerRuntimePort containerRuntime;

    public PodmanRuntimeOrchestratorAdapter(ContainerRuntimePort containerRuntime) {
        this.containerRuntime = containerRuntime;
    }

    @Override
    public void apply(RuntimeApplyCommand command) {
        requirePodman(command.capability());
        requireComposeFile(command.composeFilePath());
        command.logSink().accept("Runtime orchestrator: apply via podman");
        containerRuntime.podmanComposeUp(
                command.host(),
                command.workingDirectory(),
                command.composeFilePath(),
                command.sshPrivateKeyPem(),
                command.logSink());
    }

    @Override
    public void teardown(RuntimeTeardownCommand command) {
        requirePodman(command.capability());
        requireComposeFile(command.composeFilePath());
        command.logSink().accept("Runtime orchestrator: teardown via podman");
        containerRuntime.podmanComposeDown(
                command.host(),
                command.workingDirectory(),
                command.composeFilePath(),
                command.sshPrivateKeyPem(),
                command.logSink());
    }

    private static void requirePodman(RuntimeCapability capability) {
        if (capability != RuntimeCapability.PODMAN) {
            throw new DomainException(
                    "Podman runtime orchestrator does not support capability: " + capability.tag());
        }
    }

    private static void requireComposeFile(String composeFilePath) {
        if (composeFilePath == null || composeFilePath.isBlank()) {
            throw new DomainException("composeFilePath is required for podman runtime apply/teardown");
        }
    }
}
