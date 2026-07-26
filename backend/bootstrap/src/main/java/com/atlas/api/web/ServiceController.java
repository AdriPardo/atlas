package com.atlas.api.web;

import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.DeployServiceRequest;
import com.atlas.api.dto.request.UpdateServiceRequest;
import com.atlas.api.dto.response.DeployResponse;
import com.atlas.api.dto.response.ServiceResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.deployment.DeployServiceUseCase;
import com.atlas.application.service.DeleteServiceUseCase;
import com.atlas.application.service.GetServiceUseCase;
import com.atlas.application.service.ListServicesUseCase;
import com.atlas.application.service.UpdateServiceUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.service.ServiceStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Services")
public class ServiceController {

    private final GetServiceUseCase getServiceUseCase;
    private final ListServicesUseCase listServicesUseCase;
    private final UpdateServiceUseCase updateServiceUseCase;
    private final DeleteServiceUseCase deleteServiceUseCase;
    private final DeployServiceUseCase deployServiceUseCase;
    private final ApiMapper apiMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(apiMapper.toServiceResponse(getServiceUseCase.execute(id)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ServiceResponse>> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listServicesUseCase.execute(projectId, name, status, new PageQuery(page, size, sort));
        return ResponseEntity.ok(PageResponses.from(result, apiMapper::toServiceResponse));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateServiceRequest request) {
        var service = updateServiceUseCase.execute(
                id,
                new UpdateServiceUseCase.UpdateServiceCommand(
                        request.name(),
                        request.repositoryUrl(),
                        request.branch(),
                        request.composePath(),
                        request.domain(),
                        request.environment(),
                        request.status()));
        return ResponseEntity.ok(apiMapper.toServiceResponse(service));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteServiceUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deploy")
    public ResponseEntity<DeployResponse> deploy(
            @PathVariable UUID id, @Valid @RequestBody DeployServiceRequest request) {
        var result = deployServiceUseCase.execute(id, request.hostId());
        return ResponseEntity.accepted()
                .body(new DeployResponse(
                        result.deployment().getId(),
                        result.job().getId(),
                        result.deployment().getStatus().name()));
    }
}
