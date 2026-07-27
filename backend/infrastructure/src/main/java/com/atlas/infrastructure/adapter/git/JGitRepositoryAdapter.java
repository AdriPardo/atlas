package com.atlas.infrastructure.adapter.git;

import com.atlas.application.port.out.GitRepositoryPort;
import com.atlas.domain.shared.DomainException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "atlas.adapters.real-enabled", havingValue = "true", matchIfMissing = true)
public class JGitRepositoryAdapter implements GitRepositoryPort {

    @Override
    public void cloneOrUpdate(
            String repositoryUrl,
            String branch,
            Path targetDirectory,
            Optional<String> accessToken,
            Consumer<String> logSink) {
        try {
            Files.createDirectories(targetDirectory.getParent() == null ? targetDirectory : targetDirectory.getParent());
            if (Files.exists(targetDirectory.resolve(".git"))) {
                logSink.accept("Updating existing git workspace at " + targetDirectory);
                try (Git git = Git.open(targetDirectory.toFile())) {
                    var fetch = git.fetch();
                    accessToken.ifPresent(token -> fetch.setCredentialsProvider(credentials(token)));
                    fetch.call();
                    git.checkout().setName(branch).call();
                    var pull = git.pull().setRemoteBranchName(branch);
                    accessToken.ifPresent(token -> pull.setCredentialsProvider(credentials(token)));
                    pull.call();
                }
            } else {
                if (Files.exists(targetDirectory)) {
                    deleteRecursively(targetDirectory);
                }
                logSink.accept("Cloning " + repositoryUrl + " (branch " + branch + ")");
                var clone = Git.cloneRepository()
                        .setURI(repositoryUrl)
                        .setDirectory(targetDirectory.toFile())
                        .setBranch(branch)
                        .setCloneAllBranches(false);
                accessToken.ifPresent(token -> clone.setCredentialsProvider(credentials(token)));
                try (Git git = clone.call()) {
                    Ref head = git.getRepository().exactRef("HEAD");
                    logSink.accept("Clone complete" + (head == null ? "" : " at " + head.getObjectId().abbreviate(7).name()));
                }
            }
        } catch (GitAPIException | java.io.IOException ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            if (accessToken.isEmpty() && looksLikeAuthFailure(message)) {
                throw new DomainException(
                        "Git authentication failed for "
                                + repositoryUrl
                                + ". Provide git.token via project secret, project binding alias,"
                                + " or organization secret (GitHub PAT with repo scope), then retry. Cause: "
                                + message);
            }
            throw new DomainException("Git operation failed: " + message);
        }
    }

    private static boolean looksLikeAuthFailure(String message) {
        String lower = message.toLowerCase();
        return lower.contains("auth")
                || lower.contains("not authorized")
                || lower.contains("authentication")
                || lower.contains("credentials")
                || lower.contains("unable to access")
                || lower.contains("could not read username")
                || lower.contains("401")
                || lower.contains("403");
    }

    private static UsernamePasswordCredentialsProvider credentials(String token) {
        return new UsernamePasswordCredentialsProvider("oauth2", token);
    }

    private static void deleteRecursively(Path path) throws java.io.IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (java.io.IOException ignored) {
                    // best-effort cleanup before clone
                }
            });
        }
    }
}
