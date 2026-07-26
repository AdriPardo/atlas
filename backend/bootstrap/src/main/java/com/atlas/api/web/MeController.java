package com.atlas.api.web;

import com.atlas.api.dto.response.DashboardStatsResponse;
import com.atlas.api.dto.response.UserResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.shared.GetDashboardStatsUseCase;
import com.atlas.application.user.GetCurrentUserUseCase;
import com.atlas.infrastructure.security.AtlasUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final GetDashboardStatsUseCase getDashboardStatsUseCase;
    private final ApiMapper apiMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AtlasUserPrincipal principal) {
        return ResponseEntity.ok(apiMapper.toUserResponse(getCurrentUserUseCase.execute(principal.getId())));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> dashboardStats() {
        var stats = getDashboardStatsUseCase.execute();
        return ResponseEntity.ok(
                new DashboardStatsResponse(stats.applications(), stats.hosts(), stats.deployments()));
    }
}
