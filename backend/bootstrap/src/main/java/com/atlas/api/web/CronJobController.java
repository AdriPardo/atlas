package com.atlas.api.web;

import com.atlas.api.dto.request.CreateCronJobRequest;
import com.atlas.api.dto.request.UpdateCronJobRequest;
import com.atlas.api.dto.response.CronJobResponse;
import com.atlas.application.cron.ManageCronJobUseCase;
import com.atlas.domain.cron.CronJob;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cron-jobs")
@RequiredArgsConstructor
public class CronJobController {

    private final ManageCronJobUseCase manageCronJobUseCase;

    @GetMapping
    public ResponseEntity<List<CronJobResponse>> list() {
        return ResponseEntity.ok(manageCronJobUseCase.list().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{cronJobId}")
    public ResponseEntity<CronJobResponse> get(@PathVariable UUID cronJobId) {
        return ResponseEntity.ok(toResponse(manageCronJobUseCase.get(cronJobId)));
    }

    @PostMapping
    public ResponseEntity<CronJobResponse> create(@Valid @RequestBody CreateCronJobRequest request) {
        CronJob cronJob = manageCronJobUseCase.create(
                request.name(), request.cronExpression(), request.targetType(), request.targetId());
        return ResponseEntity.created(URI.create("/api/v1/cron-jobs/" + cronJob.getId()))
                .body(toResponse(cronJob));
    }

    @PutMapping("/{cronJobId}")
    public ResponseEntity<CronJobResponse> update(
            @PathVariable UUID cronJobId, @Valid @RequestBody UpdateCronJobRequest request) {
        return ResponseEntity.ok(toResponse(manageCronJobUseCase.update(
                cronJobId,
                request.name(),
                request.cronExpression(),
                request.targetType(),
                request.targetId(),
                request.enabled())));
    }

    @DeleteMapping("/{cronJobId}")
    public ResponseEntity<Void> delete(@PathVariable UUID cronJobId) {
        manageCronJobUseCase.delete(cronJobId);
        return ResponseEntity.noContent().build();
    }

    private CronJobResponse toResponse(CronJob cronJob) {
        return new CronJobResponse(
                cronJob.getId(),
                cronJob.getName(),
                cronJob.getCronExpression(),
                cronJob.getTargetType(),
                cronJob.getTargetId(),
                cronJob.isEnabled(),
                cronJob.getLastFiredAt(),
                cronJob.getLastError(),
                cronJob.getCreatedAt(),
                cronJob.getUpdatedAt());
    }
}
