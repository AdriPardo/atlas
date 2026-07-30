package com.atlas.api.web;

import com.atlas.api.dto.response.ProjectDatabaseProvisionResponse;
import com.atlas.api.dto.response.ProjectDatabaseResponse;
import com.atlas.application.database.ProvisionProjectDatabaseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/database")
@RequiredArgsConstructor
@Tag(name = "Project database")
public class ProjectDatabaseController {

    private final ProvisionProjectDatabaseUseCase provisionProjectDatabaseUseCase;

    @GetMapping
    @Operation(summary = "Project DB status (schema metadata; never returns credentials)")
    public ResponseEntity<ProjectDatabaseResponse> status(@PathVariable UUID projectId) {
        var status = provisionProjectDatabaseUseCase.status(projectId);
        return ResponseEntity.ok(new ProjectDatabaseResponse(
                status.provisionerConfigured(),
                status.provisioned(),
                status.schema(),
                status.role(),
                status.databaseName(),
                status.profile(),
                status.message()));
    }

    @PostMapping("/provision")
    @Operation(summary = "Provision schema + migrator role; store db.url / db.schema secrets")
    public ResponseEntity<ProjectDatabaseProvisionResponse> provision(@PathVariable UUID projectId) {
        var outcome = provisionProjectDatabaseUseCase.provision(projectId);
        return ResponseEntity.ok(new ProjectDatabaseProvisionResponse(
                outcome.schema(),
                outcome.role(),
                outcome.databaseName(),
                outcome.profile(),
                outcome.rotated()));
    }
}
