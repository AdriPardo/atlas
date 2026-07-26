package com.atlas.application.port.out;

/**
 * Future port for Docker Compose / container runtime operations. Not used by MVP use cases.
 */
public interface ContainerRuntimePort {

    void up(String composeFilePath, String workingDirectory);

    void down(String composeFilePath, String workingDirectory);

    String logs(String composeFilePath, String workingDirectory);
}
