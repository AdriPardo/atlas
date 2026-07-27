package com.atlas.api.web;

import com.atlas.api.dto.request.CreateDomainRequest;
import com.atlas.api.dto.request.UpdateDomainRequest;
import com.atlas.api.dto.response.DomainResponse;
import com.atlas.api.dto.response.TraefikMetadataResponse;
import com.atlas.application.networking.GetDomainTraefikMetadataUseCase;
import com.atlas.application.networking.ManageDomainUseCase;
import com.atlas.application.networking.VerifyDomainUseCase;
import com.atlas.application.port.out.TraefikMetadataPort;
import com.atlas.domain.networking.Domain;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
public class DomainController {

    private final ManageDomainUseCase manageDomainUseCase;
    private final VerifyDomainUseCase verifyDomainUseCase;
    private final GetDomainTraefikMetadataUseCase getDomainTraefikMetadataUseCase;

    @GetMapping("/api/v1/projects/{projectId}/domains")
    public ResponseEntity<List<DomainResponse>> listByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(manageDomainUseCase.list(projectId).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping("/api/v1/projects/{projectId}/domains")
    public ResponseEntity<DomainResponse> create(
            @PathVariable UUID projectId, @Valid @RequestBody CreateDomainRequest request) {
        Domain domain = manageDomainUseCase.create(projectId, request.hostname(), request.serviceId());
        return ResponseEntity.created(URI.create("/api/v1/domains/" + domain.getId()))
                .body(toResponse(domain));
    }

    @GetMapping("/api/v1/domains/{domainId}")
    public ResponseEntity<DomainResponse> get(@PathVariable UUID domainId) {
        return ResponseEntity.ok(toResponse(manageDomainUseCase.get(domainId)));
    }

    @PutMapping("/api/v1/domains/{domainId}")
    public ResponseEntity<DomainResponse> update(
            @PathVariable UUID domainId, @Valid @RequestBody UpdateDomainRequest request) {
        return ResponseEntity.ok(
                toResponse(manageDomainUseCase.update(domainId, request.hostname(), request.serviceId())));
    }

    @DeleteMapping("/api/v1/domains/{domainId}")
    public ResponseEntity<Void> delete(@PathVariable UUID domainId) {
        manageDomainUseCase.delete(domainId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/domains/{domainId}/verify")
    public ResponseEntity<DomainResponse> verify(@PathVariable UUID domainId) {
        return ResponseEntity.ok(toResponse(verifyDomainUseCase.execute(domainId)));
    }

    @GetMapping("/api/v1/domains/{domainId}/traefik")
    public ResponseEntity<TraefikMetadataResponse> traefik(@PathVariable UUID domainId) {
        TraefikMetadataPort.TraefikRouteMetadata metadata =
                getDomainTraefikMetadataUseCase.execute(domainId);
        return ResponseEntity.ok(new TraefikMetadataResponse(
                metadata.routerName(),
                metadata.rule(),
                metadata.entryPoints(),
                metadata.tls(),
                metadata.certResolver(),
                metadata.labels()));
    }

    /** Alias aligned with docs/api/endpoints.md GET /traefik/routes for a domain. */
    @GetMapping("/api/v1/traefik/routes/{domainId}")
    public ResponseEntity<TraefikMetadataResponse> traefikRoute(@PathVariable UUID domainId) {
        return traefik(domainId);
    }

    private DomainResponse toResponse(Domain domain) {
        return new DomainResponse(
                domain.getId(),
                domain.getProjectId(),
                domain.getServiceId(),
                domain.getHostname(),
                domain.getStatus(),
                domain.getVerificationToken(),
                domain.dnsTxtName(),
                domain.dnsTxtValue(),
                domain.getCertificateIssuer(),
                domain.getCertificateExpiresAt(),
                domain.getCertificateSans(),
                domain.getVerifiedAt(),
                domain.getLastError(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
