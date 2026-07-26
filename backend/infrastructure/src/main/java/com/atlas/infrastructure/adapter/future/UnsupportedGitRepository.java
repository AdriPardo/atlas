package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.GitRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedGitRepository implements GitRepositoryPort {

    @Override
    public void clone(String repositoryUrl, String branch, String targetDirectory) {
        throw new UnsupportedOperationException(
                "Git clone is not implemented in the MVP. Repository: " + repositoryUrl);
    }

    @Override
    public void pull(String workingDirectory, String branch) {
        throw new UnsupportedOperationException(
                "Git pull is not implemented in the MVP. Directory: " + workingDirectory);
    }
}
