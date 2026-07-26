package com.atlas.infrastructure.adapter.future;

import com.atlas.application.port.out.GitRepositoryPort;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "false")
public class UnsupportedGitRepository implements GitRepositoryPort {

    @Override
    public void cloneOrUpdate(
            String repositoryUrl,
            String branch,
            Path targetDirectory,
            Optional<String> accessToken,
            Consumer<String> logSink) {
        throw new UnsupportedOperationException(
                "Git clone is disabled (atlas.adapters.real-enabled=false). Repository: " + repositoryUrl);
    }
}
