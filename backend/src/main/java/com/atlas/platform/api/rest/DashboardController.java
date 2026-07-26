package com.atlas.platform.api.rest;

import com.atlas.platform.api.dto.response.DashboardStatsResponse;
import com.atlas.platform.application.usecase.application.DashboardStatsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardStatsUseCase dashboardStatsUseCase;

    public DashboardController(DashboardStatsUseCase dashboardStatsUseCase) {
        this.dashboardStatsUseCase = dashboardStatsUseCase;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse stats() {
        var stats = dashboardStatsUseCase.execute();
        return new DashboardStatsResponse(
                stats.applications(),
                stats.runningApplications(),
                stats.hosts(),
                stats.deployments());
    }
}
