package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.ContainerRuntimePort;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedContainerRuntime implements ContainerRuntimePort {

    @Override
    public void up(String composeFilePath, String workingDirectory) {
        throw new UnsupportedOperationException(
                "Container runtime up is not implemented in the MVP. Compose: " + composeFilePath);
    }

    @Override
    public void down(String composeFilePath, String workingDirectory) {
        throw new UnsupportedOperationException(
                "Container runtime down is not implemented in the MVP. Compose: " + composeFilePath);
    }

    @Override
    public String logs(String composeFilePath, String workingDirectory) {
        throw new UnsupportedOperationException(
                "Container runtime logs are not implemented in the MVP. Compose: " + composeFilePath);
    }
}
