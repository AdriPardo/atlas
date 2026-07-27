package com.atlas.api.web;

import com.atlas.api.dto.request.CreateDomainRequest;
import com.atlas.api.dto.request.UpdateDomainRequest;
import com.atlas.api.dto.response.DnsCnameResponse;
import com.atlas.api.dto.response.DomainResponse;
import com.atlas.api.dto.response.TraefikMetadataResponse;
import com.atlas.api.dto.response.TunnelIngressResponse;
import com.atlas.application.networking.EnsureDomainDnsCnameUseCase;
import com.atlas.application.networking.EnsureDomainTunnelIngressUseCase;
import com.atlas.application.networking.GetDomainDnsCnameUseCase;
import com.atlas.application.networking.GetDomainTraefikMetadataUseCase;
import com.atlas.application.networking.GetDomainTunnelIngressUseCase;
import com.atlas.application.networking.ManageDomainUseCase;
import com.atlas.application.networking.VerifyDomainUseCase;
import com.atlas.application.port.out.CloudflareTunnelPort;
import com.atlas.application.port.out.DnsProviderPort;
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
    private final GetDomainTunnelIngressUseCase getDomainTunnelIngressUseCase;
    private final EnsureDomainTunnelIngressUseCase ensureDomainTunnelIngressUseCase;
    private final GetDomainDnsCnameUseCase getDomainDnsCnameUseCase;
    private final EnsureDomainDnsCnameUseCase ensureDomainDnsCnameUseCase;

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

    @GetMapping("/api/v1/domains/{domainId}/tunnel-ingress")
    public ResponseEntity<TunnelIngressResponse> tunnelIngress(@PathVariable UUID domainId) {
        CloudflareTunnelPort.TunnelIngressSpec spec = getDomainTunnelIngressUseCase.execute(domainId);
        return ResponseEntity.ok(toTunnelResponse(spec, null, null));
    }

    @PostMapping("/api/v1/domains/{domainId}/tunnel-ingress/ensure")
    public ResponseEntity<TunnelIngressResponse> ensureTunnelIngress(@PathVariable UUID domainId) {
        CloudflareTunnelPort.EnsureResult result = ensureDomainTunnelIngressUseCase.execute(domainId);
        return ResponseEntity.ok(toTunnelResponse(result.ingress(), result.mode().name(), result.message()));
    }

    @GetMapping("/api/v1/domains/{domainId}/dns-cname")
    public ResponseEntity<DnsCnameResponse> dnsCname(@PathVariable UUID domainId) {
        DnsProviderPort.CnameSpec spec = getDomainDnsCnameUseCase.execute(domainId);
        return ResponseEntity.ok(toDnsCnameResponse(spec, null, null));
    }

    @PostMapping("/api/v1/domains/{domainId}/dns-cname/ensure")
    public ResponseEntity<DnsCnameResponse> ensureDnsCname(@PathVariable UUID domainId) {
        DnsProviderPort.CnameEnsureResult result = ensureDomainDnsCnameUseCase.execute(domainId);
        return ResponseEntity.ok(toDnsCnameResponse(result.spec(), result.mode().name(), result.message()));
    }

    private static TunnelIngressResponse toTunnelResponse(
            CloudflareTunnelPort.TunnelIngressSpec spec, String mode, String message) {
        return new TunnelIngressResponse(
                spec.hostname(),
                spec.subdomain(),
                spec.zone(),
                spec.type(),
                spec.originUrl(),
                spec.originService(),
                spec.noTlsVerify(),
                spec.tunnelId(),
                spec.cnameTarget(),
                spec.copyBlock(),
                spec.zeroTrustHint(),
                mode,
                message);
    }

    private static DnsCnameResponse toDnsCnameResponse(
            DnsProviderPort.CnameSpec spec, String mode, String message) {
        return new DnsCnameResponse(
                spec.hostname(),
                spec.zone(),
                spec.recordName(),
                spec.cnameTarget(),
                spec.proxied(),
                spec.copyBlock(),
                mode,
                message);
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
