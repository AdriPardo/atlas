package com.atlas.api.dto.response;

import java.util.UUID;

public record DeployResponse(UUID deploymentId, UUID jobId, String status) {}
