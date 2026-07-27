package com.atlas.api.dto.request;

import com.atlas.domain.deployment.PlacementMode;
import com.atlas.domain.service.ServiceExposure;
import java.util.UUID;

/**
 * Autopilot deploy request. {@code hostId} is optional — platform places the service when omitted.
 * {@code placementMode} defaults to SHARED (reuse LOCAL); ISOLATED requests Proxmox VM path.
 */
public record DeployServiceRequest(UUID hostId, ServiceExposure exposure, PlacementMode placementMode) {
    public DeployServiceRequest(UUID hostId, ServiceExposure exposure) {
        this(hostId, exposure, null);
    }
}
