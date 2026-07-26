package com.atlas.application.port.out;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Git clone/fetch into a local workspace directory.
 */
public interface GitRepositoryPort {

    void cloneOrUpdate(
            String repositoryUrl,
            String branch,
            Path targetDirectory,
            Optional<String> accessToken,
            Consumer<String> logSink);
}
