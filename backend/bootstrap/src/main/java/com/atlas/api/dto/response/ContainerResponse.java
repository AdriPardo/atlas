package com.atlas.api.dto.response;

public record ContainerResponse(
        String id,
        String name,
        String image,
        String state,
        String status,
        String ports,
        String labels,
        String grafanaLogsUrl) {}
