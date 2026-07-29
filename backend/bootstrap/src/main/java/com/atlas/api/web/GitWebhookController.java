package com.atlas.api.web;

import com.atlas.api.dto.response.PipelineRunResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.pipeline.HandleGitWebhookUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks/git")
@RequiredArgsConstructor
public class GitWebhookController {

    private final HandleGitWebhookUseCase handleGitWebhookUseCase;
    private final ApiMapper apiMapper;

    /**
     * Public git webhook. Auth is the path token (and optional HMAC if provider sends a signature).
     * Configure GitHub/Gitea webhook secret to the same value as the Atlas pipeline webhook token.
     * Soft rate limit: ~30 requests / 60s per token (in-memory, single node).
     *
     * <p>Only {@code push} events that target the service's configured branch enqueue a deploy.
     * {@code ping} and other events return 204 No Content.
     */
    @PostMapping("/{token}")
    public ResponseEntity<PipelineRunResponse> receive(
            @PathVariable String token,
            @RequestBody(required = false) byte[] body,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String githubSignature,
            @RequestHeader(value = "X-Gitea-Signature", required = false) String giteaSignature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String githubEvent,
            @RequestHeader(value = "X-Gitea-Event", required = false) String giteaEvent) {
        return handleGitWebhookUseCase
                .execute(token, body, githubSignature, giteaSignature, githubEvent, giteaEvent)
                .map(run -> ResponseEntity.accepted().body(apiMapper.toPipelineRunResponse(run)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
