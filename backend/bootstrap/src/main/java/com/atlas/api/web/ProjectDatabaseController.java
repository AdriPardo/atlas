package com.atlas.api.web;

import com.atlas.api.dto.request.IssueProjectDatabaseCredentialRequest;
import com.atlas.api.dto.response.ProjectDatabaseConsoleSessionResponse;
import com.atlas.api.dto.response.ProjectDatabaseCredentialListItemResponse;
import com.atlas.api.dto.response.ProjectDatabaseCredentialResponse;
import com.atlas.api.dto.response.ProjectDatabaseProvisionResponse;
import com.atlas.api.dto.response.ProjectDatabaseResponse;
import com.atlas.application.database.IssueProjectDatabaseCredentialsUseCase;
import com.atlas.application.database.OpenProjectDatabaseConsoleUseCase;
import com.atlas.application.database.ProvisionProjectDatabaseUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/database")
@RequiredArgsConstructor
@Tag(name = "Project database")
public class ProjectDatabaseController {

    private final ProvisionProjectDatabaseUseCase provisionProjectDatabaseUseCase;
    private final IssueProjectDatabaseCredentialsUseCase issueProjectDatabaseCredentialsUseCase;
    private final OpenProjectDatabaseConsoleUseCase openProjectDatabaseConsoleUseCase;

    @GetMapping
    @Operation(summary = "Project DB status (schema metadata; never returns credentials)")
    public ResponseEntity<ProjectDatabaseResponse> status(@PathVariable UUID projectId) {
        var status = provisionProjectDatabaseUseCase.status(projectId);
        String consoleUrl = openProjectDatabaseConsoleUseCase.publicUrlOrEmpty();
        return ResponseEntity.ok(new ProjectDatabaseResponse(
                status.provisionerConfigured(),
                status.provisioned(),
                status.schema(),
                status.role(),
                status.databaseName(),
                status.profile(),
                status.message(),
                openProjectDatabaseConsoleUseCase.isConfigured(),
                consoleUrl.isBlank() ? null : consoleUrl));
    }

    @PostMapping("/provision")
    @Operation(summary = "Provision schema + migrator/read roles; store db.url / db.schema secrets")
    public ResponseEntity<ProjectDatabaseProvisionResponse> provision(@PathVariable UUID projectId) {
        var outcome = provisionProjectDatabaseUseCase.provision(projectId);
        return ResponseEntity.ok(new ProjectDatabaseProvisionResponse(
                outcome.schema(),
                outcome.role(),
                outcome.databaseName(),
                outcome.profile(),
                outcome.rotated()));
    }

    @GetMapping("/credentials")
    @Operation(summary = "List active TTL credential roles (no passwords)")
    public ResponseEntity<List<ProjectDatabaseCredentialListItemResponse>> listCredentials(
            @PathVariable UUID projectId) {
        List<ProjectDatabaseCredentialListItemResponse> items =
                issueProjectDatabaseCredentialsUseCase.list(projectId).stream()
                        .map(c -> new ProjectDatabaseCredentialListItemResponse(
                                c.role(), c.expiresAt(), c.expired()))
                        .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/credentials")
    @Operation(summary = "Issue TTL connection URL (default profile db.read)")
    public ResponseEntity<ProjectDatabaseCredentialResponse> issueCredential(
            @PathVariable UUID projectId, @Valid @RequestBody(required = false) IssueProjectDatabaseCredentialRequest body) {
        IssueProjectDatabaseCredentialRequest req =
                body == null ? new IssueProjectDatabaseCredentialRequest(null, null) : body;
        var issued = issueProjectDatabaseCredentialsUseCase.issue(projectId, req.profile(), req.ttlMinutes());
        return ResponseEntity.ok(new ProjectDatabaseCredentialResponse(
                issued.role(),
                issued.profile(),
                issued.connectionUrl(),
                issued.expiresAt(),
                issued.ttlMinutes()));
    }

    @PostMapping("/console-session")
    @Operation(
            summary =
                    "Issue TTL creds and return pgweb console launch payload (SSO-gated console URL)")
    public ResponseEntity<ProjectDatabaseConsoleSessionResponse> openConsole(
            @PathVariable UUID projectId, @Valid @RequestBody(required = false) IssueProjectDatabaseCredentialRequest body) {
        IssueProjectDatabaseCredentialRequest req =
                body == null ? new IssueProjectDatabaseCredentialRequest(null, null) : body;
        var session = openProjectDatabaseConsoleUseCase.open(projectId, req.profile(), req.ttlMinutes());
        return ResponseEntity.ok(new ProjectDatabaseConsoleSessionResponse(
                session.consoleUrl(),
                session.schema(),
                session.database(),
                session.server(),
                session.role(),
                session.profile(),
                session.connectionUrl(),
                session.expiresAt(),
                session.ttlMinutes()));
    }

    @DeleteMapping("/credentials/{role}")
    @Operation(summary = "Revoke a TTL credential role early")
    public ResponseEntity<Void> revokeCredential(
            @PathVariable UUID projectId, @PathVariable String role) {
        issueProjectDatabaseCredentialsUseCase.revoke(projectId, role);
        return ResponseEntity.noContent().build();
    }
}
