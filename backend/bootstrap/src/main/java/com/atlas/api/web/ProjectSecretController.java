package com.atlas.api.web;

import com.atlas.api.dto.request.BindProjectSecretRequest;
import com.atlas.api.dto.request.CreateSecretRequest;
import com.atlas.api.dto.response.ProjectSecretEntryResponse;
import com.atlas.api.dto.response.SecretResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.secret.ManageProjectSecretsUseCase;
import com.atlas.domain.secret.Secret;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectSecretController {

    private final ManageProjectSecretsUseCase manageProjectSecretsUseCase;
    private final ApiMapper apiMapper;

    @GetMapping("/api/v1/projects/{projectId}/secrets")
    public ResponseEntity<List<ProjectSecretEntryResponse>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(manageProjectSecretsUseCase.list(projectId).stream()
                .map(this::toEntry)
                .toList());
    }

    @PostMapping("/api/v1/projects/{projectId}/secrets")
    public ResponseEntity<SecretResponse> createOwned(
            @PathVariable UUID projectId, @Valid @RequestBody CreateSecretRequest request) {
        Secret secret = manageProjectSecretsUseCase.createOwned(projectId, request.name(), request.value());
        return ResponseEntity.created(URI.create("/api/v1/projects/" + projectId + "/secrets/" + secret.getId()))
                .body(apiMapper.toSecretResponse(secret));
    }

    /** Idempotent project-owned upsert (UI rotate / seed). */
    @PutMapping("/api/v1/projects/{projectId}/secrets")
    public ResponseEntity<SecretResponse> upsertOwned(
            @PathVariable UUID projectId, @Valid @RequestBody CreateSecretRequest request) {
        ManageProjectSecretsUseCase.UpsertOwnedResult result =
                manageProjectSecretsUseCase.upsertOwned(projectId, request.name(), request.value());
        HttpStatus status = result.updated() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(apiMapper.toSecretResponse(result.secret()));
    }

    @PostMapping("/api/v1/projects/{projectId}/secrets/bindings")
    public ResponseEntity<ProjectSecretEntryResponse> link(
            @PathVariable UUID projectId, @Valid @RequestBody BindProjectSecretRequest request) {
        ManageProjectSecretsUseCase.ProjectSecretEntry entry =
                manageProjectSecretsUseCase.linkGlobal(projectId, request.secretId(), request.alias());
        return ResponseEntity.created(
                        URI.create("/api/v1/projects/" + projectId + "/secrets/bindings/" + entry.bindingId()))
                .body(toEntry(entry));
    }

    @DeleteMapping("/api/v1/projects/{projectId}/secrets/bindings/{bindingId}")
    public ResponseEntity<Void> unlink(@PathVariable UUID projectId, @PathVariable UUID bindingId) {
        manageProjectSecretsUseCase.unlink(projectId, bindingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/projects/{projectId}/secrets/{secretId}")
    public ResponseEntity<Void> deleteOwned(@PathVariable UUID projectId, @PathVariable UUID secretId) {
        manageProjectSecretsUseCase.deleteOwned(projectId, secretId);
        return ResponseEntity.noContent().build();
    }

    private ProjectSecretEntryResponse toEntry(ManageProjectSecretsUseCase.ProjectSecretEntry entry) {
        return new ProjectSecretEntryResponse(
                entry.kind().name(),
                entry.secretId(),
                entry.name(),
                entry.secretName(),
                entry.bindingId(),
                entry.alias(),
                entry.createdAt(),
                entry.updatedAt());
    }
}
