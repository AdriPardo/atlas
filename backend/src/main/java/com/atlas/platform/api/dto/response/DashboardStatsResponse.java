package com.atlas.platform.api.dto.response;

public record DashboardStatsResponse(
        long applications, long runningApplications, long hosts, long deployments) {}
