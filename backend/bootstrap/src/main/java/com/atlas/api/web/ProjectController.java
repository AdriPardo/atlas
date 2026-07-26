package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreateProjectRequest;
import com.atlas.api.dto.request.CreateServiceRequest;
import com.atlas.api.dto.request.DeployServiceRequest;
import com.atlas.api.dto.request.UpdateProjectRequest;
import com.atlas.api.dto.response.DeployResponse;
import com.atlas.api.dto.response.ProjectResponse;
import com.atlas.api.dto.response.ServiceResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.deployment.DeployServiceUseCase;
import com.atlas.application.project.CreateProjectUseCase;
import com.atlas.application.project.DeleteProjectUseCase;
import com.atlas.application.project.GetProjectUseCase;
import com.atlas.application.project.ListProjectsUseCase;
import com.atlas.application.project.UpdateProjectUseCase;
import com.atlas.application.service.CreateServiceUseCase;
import com.atlas.application.service.ListServicesUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Tag(name = "Projects")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final ListProjectsUseCase listProjectsUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final CreateServiceUseCase createServiceUseCase;
    private final ListServicesUseCase listServicesUseCase;
    private final DeployServiceUseCase deployServiceUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        var result = createProjectUseCase.execute(new CreateProjectUseCase.CreateProjectCommand(
                request.name(),
                request.description(),
                request.repositoryUrl(),
                request.branch(),
                request.composePath(),
                request.domain()));
        return ResponseEntity.created(URI.create("/api/v1/projects/" + result.project().getId()))
                .body(apiMapper.toProjectResponse(result.project()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toProjectResponse(getProjectUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProjectResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listProjectsUseCase.execute(name, status, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toProjectResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        var project = updateProjectUseCase.execute(
                id,
                new UpdateProjectUseCase.UpdateProjectCommand(
                        request.name(), request.description(), request.status()));
        return ResponseEntity.ok(apiMapper.toProjectResponse(project));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteProjectUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{projectId}/services")
    public ResponseEntity<PageResponse<ServiceResponse>> listServices(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result =
                listServicesUseCase.execute(projectId, name, status, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toServiceResponse));
    }

    @PostMapping("/{projectId}/services")
    public ResponseEntity<ServiceResponse> createService(
            @PathVariable UUID projectId, @Valid @RequestBody CreateServiceRequest request) {
        var service = createServiceUseCase.execute(
                projectId,
                new CreateServiceUseCase.CreateServiceCommand(
                        request.name(),
                        request.repositoryUrl(),
                        request.branch(),
                        request.composePath(),
                        request.domain(),
                        request.environment()));
        return ResponseEntity.created(URI.create("/api/v1/services/" + service.getId()))
                .body(apiMapper.toServiceResponse(service));
    }

    @PostMapping("/{projectId}/deploy")
    public ResponseEntity<DeployResponse> deployDefaultService(
            @PathVariable UUID projectId, @Valid @RequestBody DeployServiceRequest request) {
        var result = deployServiceUseCase.executeForProject(projectId, request.hostId());
        return ResponseEntity.accepted()
                .body(new DeployResponse(
                        result.deployment().getId(),
                        result.job().getId(),
                        result.deployment().getStatus().name()));
    }
}
