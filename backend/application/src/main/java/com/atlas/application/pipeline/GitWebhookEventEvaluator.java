package com.atlas.application.pipeline;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * Decides whether a git provider webhook should trigger a deploy.
 *
 * <p>Rules:
 * <ul>
 *   <li>If an event header is present ({@code X-GitHub-Event} / {@code X-Gitea-Event}), only
 *       {@code push} is accepted; {@code ping} and other events are ignored.
 *   <li>If no event header is present (manual curl), the request is treated as a push.
 *   <li>When the body includes {@code ref}, it must match {@code refs/heads/<serviceBranch>}.
 *   <li>Deleted-branch pushes ({@code "deleted": true}) are ignored.
 * </ul>
 */
public final class GitWebhookEventEvaluator {

    private GitWebhookEventEvaluator() {}

    public static Optional<String> ignoreReason(
            byte[] body, String githubEvent, String giteaEvent, String serviceBranch) {
        String event = firstNonBlank(githubEvent, giteaEvent);
        if (event != null) {
            String normalized = event.trim().toLowerCase(Locale.ROOT);
            if ("ping".equals(normalized)) {
                return Optional.of("ignored ping event");
            }
            if (!"push".equals(normalized)) {
                return Optional.of("ignored non-push event: " + normalized);
            }
        }

        String json = body == null ? "" : new String(body, StandardCharsets.UTF_8);
        if (isDeletedPush(json)) {
            return Optional.of("ignored deleted-branch push");
        }

        Optional<String> ref = extractRef(json);
        if (ref.isEmpty()) {
            return Optional.empty();
        }

        String refValue = ref.get();
        String headsPrefix = "refs/heads/";
        if (!refValue.startsWith(headsPrefix)) {
            return Optional.of("ignored non-branch ref: " + refValue);
        }

        String pushedBranch = refValue.substring(headsPrefix.length());
        if (pushedBranch.isBlank()) {
            return Optional.empty();
        }

        String expected = serviceBranch == null ? "" : serviceBranch.trim();
        if (expected.isEmpty()) {
            return Optional.empty();
        }
        if (!expected.equals(pushedBranch)) {
            return Optional.of(
                    "ignored push to branch '" + pushedBranch + "' (service tracks '" + expected + "')");
        }
        return Optional.empty();
    }

    static Optional<String> extractRef(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        String marker = "\"ref\"";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return Optional.empty();
        }
        int colon = json.indexOf(':', idx + marker.length());
        if (colon < 0) {
            return Optional.empty();
        }
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return Optional.empty();
        }
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return Optional.empty();
        }
        String ref = json.substring(firstQuote + 1, secondQuote);
        return ref.isBlank() ? Optional.empty() : Optional.of(ref);
    }

    private static boolean isDeletedPush(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        return json.matches("(?s).*\"deleted\"\\s*:\\s*true.*");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
