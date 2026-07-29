package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GitWebhookEventEvaluatorTest {

    @Test
    void ignoresPing() {
        Optional<String> reason = GitWebhookEventEvaluator.ignoreReason(
                "{}".getBytes(StandardCharsets.UTF_8), "ping", null, "main");
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("ping"));
    }

    @Test
    void ignoresPullRequest() {
        Optional<String> reason = GitWebhookEventEvaluator.ignoreReason(
                "{}".getBytes(StandardCharsets.UTF_8), "pull_request", null, "main");
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("non-push"));
    }

    @Test
    void acceptsPushToTrackedBranch() {
        byte[] body = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        assertTrue(GitWebhookEventEvaluator.ignoreReason(body, "push", null, "main").isEmpty());
    }

    @Test
    void ignoresPushToOtherBranch() {
        byte[] body = "{\"ref\":\"refs/heads/feature\"}".getBytes(StandardCharsets.UTF_8);
        Optional<String> reason = GitWebhookEventEvaluator.ignoreReason(body, "push", null, "main");
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("feature"));
    }

    @Test
    void ignoresTagRef() {
        byte[] body = "{\"ref\":\"refs/tags/v1\"}".getBytes(StandardCharsets.UTF_8);
        Optional<String> reason = GitWebhookEventEvaluator.ignoreReason(body, "push", null, "main");
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("non-branch"));
    }

    @Test
    void ignoresDeletedBranch() {
        byte[] body = "{\"ref\":\"refs/heads/main\",\"deleted\":true}".getBytes(StandardCharsets.UTF_8);
        Optional<String> reason = GitWebhookEventEvaluator.ignoreReason(body, "push", null, "main");
        assertTrue(reason.isPresent());
        assertTrue(reason.get().contains("deleted"));
    }

    @Test
    void allowsCurlWithoutEventHeaderOrRef() {
        assertTrue(GitWebhookEventEvaluator.ignoreReason("{}".getBytes(), null, null, "main").isEmpty());
    }

    @Test
    void giteaEventHeaderRespected() {
        assertEquals(
                Optional.of("ignored ping event"),
                GitWebhookEventEvaluator.ignoreReason("{}".getBytes(), null, "ping", "main"));
    }
}
