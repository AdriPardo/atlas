package com.atlas.platform.api.rest;

import com.atlas.platform.api.dto.request.CreateApplicationRequest;
import com.atlas.platform.api.dto.request.UpdateApplicationRequest;
import com.atlas.platform.api.dto.response.ApplicationResponse;
import com.atlas.platform.api.dto.response.PageResponse;
import com.atlas.platform.api.mapper.ApiMapper;
import com.atlas.platform.application.usecase.application.CreateApplicationCommand;
import com.atlas.platform.application.usecase.application.CreateApplicationUseCase;
import com.atlas.platform.application.usecase.application.DeleteApplicationUseCase;
import com.atlas.platform.application.usecase.application.GetApplicationUseCase;
import com.atlas.platform.application.usecase.application.ListApplicationsUseCase;
import com.atlas.platform.application.usecase.application.UpdateApplicationCommand;
import com.atlas.platform.application.usecase.application.UpdateApplicationUseCase;
import com.atlas.platform.domain.model.ApplicationStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final CreateApplicationUseCase createApplicationUseCase;
    private final UpdateApplicationUseCase updateApplicationUseCase;
    private final GetApplicationUseCase getApplicationUseCase;
    private final ListApplicationsUseCase listApplicationsUseCase;
    private final DeleteApplicationUseCase deleteApplicationUseCase;
    private final ApiMapper apiMapper;

    public ApplicationController(
            CreateApplicationUseCase createApplicationUseCase,
            UpdateApplicationUseCase updateApplicationUseCase,
            GetApplicationUseCase getApplicationUseCase,
            ListApplicationsUseCase listApplicationsUseCase,
            DeleteApplicationUseCase deleteApplicationUseCase,
            ApiMapper apiMapper) {
        this.createApplicationUseCase = createApplicationUseCase;
        this.updateApplicationUseCase = updateApplicationUseCase;
        this.getApplicationUseCase = getApplicationUseCase;
        this.listApplicationsUseCase = listApplicationsUseCase;
        this.deleteApplicationUseCase = deleteApplicationUseCase;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        boolean ascending = "asc".equalsIgnoreCase(sortDir);
        return apiMapper.toPage(
                listApplicationsUseCase.execute(name, status, page, size, sortBy, ascending),
                apiMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable UUID id) {
        return apiMapper.toResponse(getApplicationUseCase.execute(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse create(@Valid @RequestBody CreateApplicationRequest request) {
        return apiMapper.toResponse(createApplicationUseCase.execute(new CreateApplicationCommand(
                request.name(),
                request.description(),
                request.repositoryUrl(),
                request.branch(),
                request.composePath(),
                request.domain())));
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable UUID id, @Valid @RequestBody UpdateApplicationRequest request) {
        return apiMapper.toResponse(updateApplicationUseCase.execute(new UpdateApplicationCommand(
                id,
                request.name(),
                request.description(),
                request.repositoryUrl(),
                request.branch(),
                request.composePath(),
                request.domain(),
                request.status())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        deleteApplicationUseCase.execute(id);
    }
}
