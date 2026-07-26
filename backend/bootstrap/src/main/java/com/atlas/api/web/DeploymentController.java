package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreateDeploymentRequest;
import com.atlas.api.dto.request.UpdateDeploymentRequest;
import com.atlas.api.dto.response.DeploymentResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.deployment.CreateDeploymentUseCase;
import com.atlas.application.deployment.DeleteDeploymentUseCase;
import com.atlas.application.deployment.GetDeploymentUseCase;
import com.atlas.application.deployment.ListDeploymentsUseCase;
import com.atlas.application.deployment.UpdateDeploymentUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.deployment.DeploymentStatus;
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
@RequestMapping("/api/v1/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final CreateDeploymentUseCase createDeploymentUseCase;
    private final GetDeploymentUseCase getDeploymentUseCase;
    private final ListDeploymentsUseCase listDeploymentsUseCase;
    private final UpdateDeploymentUseCase updateDeploymentUseCase;
    private final DeleteDeploymentUseCase deleteDeploymentUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    public ResponseEntity<DeploymentResponse> create(@Valid @RequestBody CreateDeploymentRequest request) {
        var deployment = createDeploymentUseCase.execute(new CreateDeploymentUseCase.CreateDeploymentCommand(
                request.serviceId(), request.applicationId(), request.hostId()));
        return ResponseEntity.created(URI.create("/api/v1/deployments/" + deployment.getId()))
                .body(apiMapper.toDeploymentResponse(deployment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeploymentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toDeploymentResponse(getDeploymentUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<DeploymentResponse>> list(
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) DeploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listDeploymentsUseCase.execute(
                serviceId, applicationId, hostId, status, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toDeploymentResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeploymentResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateDeploymentRequest request) {
        var deployment = updateDeploymentUseCase.execute(
                id,
                new UpdateDeploymentUseCase.UpdateDeploymentCommand(
                        request.status(), request.startedAt(), request.finishedAt(), request.logs()));
        return ResponseEntity.ok(apiMapper.toDeploymentResponse(deployment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteDeploymentUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}
