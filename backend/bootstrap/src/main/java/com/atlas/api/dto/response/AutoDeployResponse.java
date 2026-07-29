package com.atlas.api.dto.response;

public record AutoDeployResponse(
        PipelineResponse pipeline,
        boolean created,
        String webhookUrl,
        String trackedBranch,
        boolean githubWebhookRegistered,
        String githubWebhookMessage,
        String githubHookId,
        String setupInstructions) {}
