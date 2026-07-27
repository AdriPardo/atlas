package com.atlas.api.web;

import com.atlas.api.dto.response.JobResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.backup.EnqueueBackupDatabaseUseCase;
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
    private final EnqueueBackupDatabaseUseCase enqueueBackupDatabaseUseCase;
    private final ApiMapper apiMapper;

    @PostMapping("/purge")
    public ResponseEntity<Map<String, Object>> purge() {
        var result = purgeRetentionUseCase.executeAsAdmin();
        return ResponseEntity.ok(Map.of(
                "deletedJobs", result.deletedJobs(),
                "deletedPipelineRuns", result.deletedPipelineRuns(),
                "ran", result.ran()));
    }

    @PostMapping("/backup")
    public ResponseEntity<JobResponse> backup() {
        var job = enqueueBackupDatabaseUseCase.executeAsAdmin();
        return ResponseEntity.accepted().body(apiMapper.toJobResponse(job));
    }
}
