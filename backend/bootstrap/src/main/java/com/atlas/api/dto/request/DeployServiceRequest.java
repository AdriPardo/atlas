package com.atlas.api.dto.request;

import com.atlas.domain.service.ServiceExposure;
import java.util.UUID;

/**
 * Autopilot deploy request. {@code hostId} is optional — platform places the service when omitted.
 */
public record DeployServiceRequest(UUID hostId, ServiceExposure exposure) {}
