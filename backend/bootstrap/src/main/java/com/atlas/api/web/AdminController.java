package com.atlas.api.web;

import com.atlas.application.retention.PurgeRetentionUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final PurgeRetentionUseCase purgeRetentionUseCase;

    @PostMapping("/purge")
    public ResponseEntity<Map<String, Object>> purge() {
        var result = purgeRetentionUseCase.executeAsAdmin();
        return ResponseEntity.ok(Map.of(
                "deletedJobs", result.deletedJobs(),
                "deletedPipelineRuns", result.deletedPipelineRuns(),
                "ran", result.ran()));
    }
}
