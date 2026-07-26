package com.atlas.platform.application.usecase.application;

public record CreateApplicationCommand(
        String name,
        String description,
        String repositoryUrl,
        String branch,
        String composePath,
        String domain) {}
