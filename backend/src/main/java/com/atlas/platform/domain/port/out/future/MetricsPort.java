package com.atlas.platform.domain.port.out.future;

import java.util.UUID;

/**
 * Future port for Prometheus/metrics integrations. Not used by the MVP.
 */
public interface MetricsPort {

    boolean isHealthy(UUID applicationId);
}
