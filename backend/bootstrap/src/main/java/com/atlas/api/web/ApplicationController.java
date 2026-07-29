package com.atlas.api.web;

import com.atlas.api.config.OpenApiConfig;
import com.atlas.api.dto.common.PageResponse;
import com.atlas.api.dto.common.PageResponses;
import com.atlas.api.dto.request.CreateApplicationRequest;
import com.atlas.api.dto.request.DeployApplicationRequest;
import com.atlas.api.dto.request.UpdateApplicationRequest;
import com.atlas.api.dto.response.ApplicationResponse;
import com.atlas.api.dto.response.DeployResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.application.CreateApplicationUseCase;
import com.atlas.application.application.DeleteApplicationUseCase;
import com.atlas.application.application.GetApplicationUseCase;
import com.atlas.application.application.ListApplicationsUseCase;
import com.atlas.application.application.UpdateApplicationUseCase;
import com.atlas.application.deployment.DeployApplicationUseCase;
import com.atlas.application.shared.PageQuery;
import com.atlas.domain.application.ApplicationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

/**
 * @deprecated Prefer {@link ProjectController} / {@link ServiceController}. Maps 1:1 to Project +
 *     default Service (ADR-0004).
 */
@Deprecated
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(
        name = "Applications (deprecated)",
        description =
                "Deprecated alias for Projects + default Service. Prefer /api/v1/projects and "
                        + "/api/v1/services. Sunset "
                        + OpenApiConfig.APPLICATIONS_SUNSET
                        + ". See docs/api/deprecations.md.")
public class ApplicationController {

    private static final String SUNSET = OpenApiConfig.APPLICATIONS_SUNSET;
    private static final String DEPRECATION_LINK =
            "</api/v1/projects>; rel=\"successor-version\", </api/v1/services>; rel=\"alternate\"";

    private final CreateApplicationUseCase createApplicationUseCase;
    private final GetApplicationUseCase getApplicationUseCase;
    private final ListApplicationsUseCase listApplicationsUseCase;
    private final UpdateApplicationUseCase updateApplicationUseCase;
    private final DeleteApplicationUseCase deleteApplicationUseCase;
    private final DeployApplicationUseCase deployApplicationUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    @Operation(deprecated = true)
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        var application = createApplicationUseCase.execute(new CreateApplicationUseCase.CreateApplicationCommand(
                request.name(),
                request.description(),
                request.repositoryUrl(),
                request.branch(),
                request.composePath(),
                request.domain()));
        return deprecated(ResponseEntity.created(URI.create("/api/v1/applications/" + application.getId()))
                .body(apiMapper.toApplicationResponse(application)));
    }

    @GetMapping("/{id}")
    @Operation(deprecated = true)
    public ResponseEntity<ApplicationResponse> getById(@PathVariable UUID id) {
        return deprecated(ResponseEntity.ok(apiMapper.toApplicationResponse(getApplicationUseCase.execute(id))));
    }

    @GetMapping
    @Operation(deprecated = true)
    public ResponseEntity<PageResponse<ApplicationResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var result = listApplicationsUseCase.execute(name, status, new PageQuery(page, size, sort));
        return deprecated(ResponseEntity.ok(PageResponses.from(result, apiMapper::toApplicationResponse)));
    }

    @PostMapping("/{id}/deploy")
    @Operation(deprecated = true)
    public ResponseEntity<DeployResponse> deploy(
            @PathVariable UUID id, @Valid @RequestBody DeployApplicationRequest request) {
        var result = deployApplicationUseCase.execute(id, request.hostId());
        return deprecated(ResponseEntity.accepted()
                .body(new DeployResponse(
                        result.deployment().getId(),
                        result.job().getId(),
                        result.deployment().getStatus().name())));
    }

    @PutMapping("/{id}")
    @Operation(deprecated = true)
    public ResponseEntity<ApplicationResponse> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateApplicationRequest request) {
        var application = updateApplicationUseCase.execute(
                id,
                new UpdateApplicationUseCase.UpdateApplicationCommand(
                        request.name(),
                        request.description(),
                        request.repositoryUrl(),
                        request.branch(),
                        request.composePath(),
                        request.domain(),
                        request.status()));
        return deprecated(ResponseEntity.ok(apiMapper.toApplicationResponse(application)));
    }

    @DeleteMapping("/{id}")
    @Operation(deprecated = true)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteApplicationUseCase.execute(id);
        return deprecated(ResponseEntity.noContent().build());
    }

    private static <T> ResponseEntity<T> deprecated(ResponseEntity<T> response) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(response.getHeaders());
        headers.add("Deprecation", "true");
        headers.add("Sunset", SUNSET);
        headers.add("Link", DEPRECATION_LINK);
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }
}
