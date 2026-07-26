package com.atlas.domain.runtime;

/**
 * Read-mostly projection of a Docker container discovered on a host.
 */
public record ContainerSnapshot(
        String id,
        String name,
        String image,
        String state,
        String status,
        String ports,
        String labels) {}
