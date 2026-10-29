package com.atlas.api.web;

import com.atlas.api.dto.request.CreateApiKeyRequest;
import com.atlas.api.dto.response.ApiKeyCreatedResponse;
import com.atlas.api.dto.response.ApiKeyResponse;
import com.atlas.application.apikey.ManageApiKeysUseCase;
import com.atlas.application.port.out.ApiKeyRepositoryPort;
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
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ManageApiKeysUseCase manageApiKeysUseCase;

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(@Valid @RequestBody CreateApiKeyRequest request) {
        var created = manageApiKeysUseCase.create(request.name());
        return ResponseEntity.ok(new ApiKeyCreatedResponse(created.id(), created.name(), created.prefix(), created.key()));
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> list() {
        return ResponseEntity.ok(manageApiKeysUseCase.list().stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id) {
        manageApiKeysUseCase.revoke(id);
        return ResponseEntity.noContent().build();
    }

    private ApiKeyResponse toResponse(ApiKeyRepositoryPort.Stored stored) {
        return new ApiKeyResponse(
                stored.id(),
                stored.name(),
                stored.prefix(),
                stored.createdAt(),
                stored.lastUsedAt(),
                stored.revokedAt());
    }
}
