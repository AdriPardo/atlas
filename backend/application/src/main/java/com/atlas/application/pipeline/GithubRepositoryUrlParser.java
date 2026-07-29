package com.atlas.application.pipeline;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts {@code owner/repo} from common GitHub clone URLs. */
public final class GithubRepositoryUrlParser {

    private static final Pattern HTTPS =
            Pattern.compile("(?i)^https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern SSH =
            Pattern.compile("(?i)^git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final Pattern SSH_SLASH =
            Pattern.compile("(?i)^ssh://git@github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");

    private GithubRepositoryUrlParser() {}

    public static Optional<OwnerRepo> parse(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = repositoryUrl.trim();
        Matcher m = HTTPS.matcher(trimmed);
        if (!m.matches()) {
            m = SSH.matcher(trimmed);
        }
        if (!m.matches()) {
            m = SSH_SLASH.matcher(trimmed);
        }
        if (!m.matches()) {
            return Optional.empty();
        }
        String owner = m.group(1);
        String repo = m.group(2);
        if (owner.isBlank() || repo.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new OwnerRepo(owner, repo));
    }

    public static boolean isGithub(String repositoryUrl) {
        if (repositoryUrl == null) {
            return false;
        }
        String lower = repositoryUrl.toLowerCase(Locale.ROOT);
        return lower.contains("github.com");
    }

    public record OwnerRepo(String owner, String repo) {}
}
