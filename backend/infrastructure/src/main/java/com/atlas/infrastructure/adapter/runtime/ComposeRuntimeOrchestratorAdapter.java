package com.atlas.infrastructure.adapter.runtime;

import com.atlas.application.port.out.ContainerRuntimePort;
import com.atlas.application.port.out.RuntimeOrchestratorPort;
import com.atlas.domain.runtime.RuntimeCapability;
import com.atlas.domain.shared.DomainException;
import org.springframework.stereotype.Component;

/**
 * Compose adapter for {@link RuntimeOrchestratorPort} (ADR-0014 phase D).
 * Delegates to {@link ContainerRuntimePort#composeUp} / {@code composeDown}.
 */
@Component
public class ComposeRuntimeOrchestratorAdapter implements RuntimeOrchestratorPort {

    private final ContainerRuntimePort containerRuntime;

    public ComposeRuntimeOrchestratorAdapter(ContainerRuntimePort containerRuntime) {
        this.containerRuntime = containerRuntime;
    }

    @Override
    public void apply(RuntimeApplyCommand command) {
        requireCompose(command.capability());
        requireComposeFile(command.composeFilePath());
        command.logSink().accept("Runtime orchestrator: apply via compose");
        containerRuntime.composeUp(
                command.host(),
                command.workingDirectory(),
                command.composeFilePath(),
                command.sshPrivateKeyPem(),
                command.logSink());
    }

    @Override
    public void teardown(RuntimeTeardownCommand command) {
        requireCompose(command.capability());
        requireComposeFile(command.composeFilePath());
        command.logSink().accept("Runtime orchestrator: teardown via compose");
        containerRuntime.composeDown(
                command.host(),
                command.workingDirectory(),
                command.composeFilePath(),
                command.sshPrivateKeyPem(),
                command.logSink());
    }

    private static void requireCompose(RuntimeCapability capability) {
        if (capability != RuntimeCapability.COMPOSE) {
            throw new DomainException(
                    "Compose runtime orchestrator does not support capability: " + capability.tag());
        }
    }

    private static void requireComposeFile(String composeFilePath) {
        if (composeFilePath == null || composeFilePath.isBlank()) {
            throw new DomainException("composeFilePath is required for compose runtime apply/teardown");
        }
    }
}
