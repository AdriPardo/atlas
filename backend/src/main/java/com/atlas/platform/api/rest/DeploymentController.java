package com.atlas.platform.api.rest;

import com.atlas.platform.api.dto.response.DeploymentResponse;
import com.atlas.platform.api.dto.response.PageResponse;
import com.atlas.platform.api.mapper.ApiMapper;
import com.atlas.platform.application.usecase.deployment.GetDeploymentUseCase;
import com.atlas.platform.application.usecase.deployment.ListDeploymentsUseCase;
import com.atlas.platform.domain.model.DeploymentStatus;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deployments")
public class DeploymentController {

    private final ListDeploymentsUseCase listDeploymentsUseCase;
    private final GetDeploymentUseCase getDeploymentUseCase;
    private final ApiMapper apiMapper;

    public DeploymentController(
            ListDeploymentsUseCase listDeploymentsUseCase,
            GetDeploymentUseCase getDeploymentUseCase,
            ApiMapper apiMapper) {
        this.listDeploymentsUseCase = listDeploymentsUseCase;
        this.getDeploymentUseCase = getDeploymentUseCase;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public PageResponse<DeploymentResponse> list(
            @RequestParam(required = false) UUID applicationId,
            @RequestParam(required = false) UUID hostId,
            @RequestParam(required = false) DeploymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        boolean ascending = "asc".equalsIgnoreCase(sortDir);
        return apiMapper.toPage(
                listDeploymentsUseCase.execute(
                        applicationId, hostId, status, page, size, sortBy, ascending),
                apiMapper::toResponse);
    }

    @GetMapping("/{id}")
    public DeploymentResponse get(@PathVariable UUID id) {
        return apiMapper.toResponse(getDeploymentUseCase.execute(id));
    }
}
