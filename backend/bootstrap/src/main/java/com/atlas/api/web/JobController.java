package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.response.JobResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.job.GetJobUseCase;
import com.atlas.application.job.ListJobsUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.job.JobStatus;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final GetJobUseCase getJobUseCase;
    private final ListJobsUseCase listJobsUseCase;
    private final ApiMapper apiMapper;

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toJobResponse(getJobUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<JobResponse>> list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listJobsUseCase.execute(status, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toJobResponse));
    }
}
