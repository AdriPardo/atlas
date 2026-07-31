package com.atlas.api.web;

import com.atlas.api.dto.request.CreateSecretRequest;
import com.atlas.api.dto.response.SecretResponse;
import com.atlas.api.mapper.ApiMapper;
import com.atlas.application.secret.CreateSecretUseCase;
import com.atlas.application.secret.ListSecretsUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secrets")
@RequiredArgsConstructor
public class SecretController {

    private final CreateSecretUseCase createSecretUseCase;
    private final ListSecretsUseCase listSecretsUseCase;
    private final ApiMapper apiMapper;

    @PostMapping
    public ResponseEntity<SecretResponse> create(@Valid @RequestBody CreateSecretRequest request) {
        var secret = createSecretUseCase.execute(
                new CreateSecretUseCase.CreateSecretCommand(request.name(), request.value()));
        return ResponseEntity.created(URI.create("/api/v1/secrets/" + secret.getId()))
                .body(apiMapper.toSecretResponse(secret));
    }

    /** Idempotent org/global upsert (ops seed / rotate). ADMIN only. */
    @PutMapping
    public ResponseEntity<SecretResponse> upsert(@Valid @RequestBody CreateSecretRequest request) {
        CreateSecretUseCase.UpsertResult result = createSecretUseCase.upsert(
                new CreateSecretUseCase.CreateSecretCommand(request.name(), request.value()));
        HttpStatus status = result.updated() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(apiMapper.toSecretResponse(result.secret()));
    }

    @GetMapping
    public ResponseEntity<List<SecretResponse>> list() {
        return ResponseEntity.ok(
                listSecretsUseCase.execute().stream().map(apiMapper::toSecretResponse).toList());
    }
}
