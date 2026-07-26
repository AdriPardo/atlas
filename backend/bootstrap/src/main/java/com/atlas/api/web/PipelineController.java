package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreatePipelineRequest;
import com.atlas.api.dto.request.UpdatePipelineRequest;
import com.atlas.api.dto.response.PipelineResponse;
import com.atlas.api.dto.response.PipelineRunResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.pipeline.CreatePipelineUseCase;
import com.atlas.application.pipeline.DeletePipelineUseCase;
import com.atlas.application.pipeline.GetPipelineUseCase;
import com.atlas.application.pipeline.ListPipelineRunsUseCase;
import com.atlas.application.pipeline.ListPipelinesUseCase;
import com.atlas.application.pipeline.RunPipelineUseCase;
import com.atlas.application.pipeline.UpdatePipelineUseCase;
import com.atlas.application.shared.PageQuery;
import jakarta.validation.Valid;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final CreatePipelineUseCase createPipelineUseCase;
    private final GetPipelineUseCase getPipelineUseCase;
    private final ListPipelinesUseCase listPipelinesUseCase;
    private final UpdatePipelineUseCase updatePipelineUseCase;
    private final DeletePipelineUseCase deletePipelineUseCase;
    private final RunPipelineUseCase runPipelineUseCase;
    private final ListPipelineRunsUseCase listPipelineRunsUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    public ResponseEntity<PipelineResponse> create(@Valid @RequestBody CreatePipelineRequest request) {
        var pipeline = createPipelineUseCase.execute(new CreatePipelineUseCase.CreatePipelineCommand(
                request.projectId(), request.name(), request.serviceId(), request.hostId()));
        return ResponseEntity.created(URI.create("/api/v1/pipelines/" + pipeline.getId()))
                .body(apiMapper.toPipelineResponse(pipeline));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PipelineResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toPipelineResponse(getPipelineUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<PipelineResponse>> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listPipelinesUseCase.execute(projectId, name, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toPipelineResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PipelineResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePipelineRequest request) {
        var pipeline = updatePipelineUseCase.execute(
                id, new UpdatePipelineUseCase.UpdatePipelineCommand(request.name(), request.serviceId(), request.hostId()));
        return ResponseEntity.ok(apiMapper.toPipelineResponse(pipeline));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deletePipelineUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/runs")
    public ResponseEntity<PipelineRunResponse> run(@PathVariable UUID id) {
        var run = runPipelineUseCase.execute(id, "manual");
        return ResponseEntity.accepted().body(apiMapper.toPipelineRunResponse(run));
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<PageResponse<PipelineRunResponse>> listRuns(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        getPipelineUseCase.execute(id);
        var result = listPipelineRunsUseCase.execute(id, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toPipelineRunResponse));
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<PipelineRunResponse> getRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(apiMapper.toPipelineRunResponse(listPipelineRunsUseCase.get(runId)));
    }
}
