package com.atlas.platform.application.usecase.application;

import com.atlas.platform.domain.model.ApplicationStatus;
import java.util.UUID;

public record UpdateApplicationCommand(
        UUID id,
        String name,
        String description,
        String repositoryUrl,
        String branch,
        String composePath,
        String domain,
        ApplicationStatus status) {}
