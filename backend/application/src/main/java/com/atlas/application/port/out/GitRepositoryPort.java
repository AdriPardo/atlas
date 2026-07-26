package com.atlas.application.port.out;

/**
 * Future port for Git clone/pull operations. Not used by MVP use cases.
 */
public interface GitRepositoryPort {

    void clone(String repositoryUrl, String branch, String targetDirectory);

    void pull(String workingDirectory, String branch);
}
