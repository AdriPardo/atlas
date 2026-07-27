package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.DomainRepositoryPort;
import com.atlas.domain.networking.Domain;
import com.atlas.domain.networking.DomainStatus;
import com.atlas.infrastructure.persistence.jpa.entity.DomainJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.DomainJpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DomainRepositoryAdapter implements DomainRepositoryPort {

    private final DomainJpaRepository repository;

    @Override
    public Domain save(Domain domain) {
        return toDomain(repository.save(toEntity(domain)));
    }

    @Override
    public Optional<Domain> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Domain> findByProjectId(UUID projectId) {
        return repository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByProjectIdAndHostnameIgnoreCase(UUID projectId, String hostname) {
        return repository.existsByProjectIdAndHostnameIgnoreCase(projectId, hostname);
    }

    @Override
    public boolean existsByProjectIdAndHostnameIgnoreCaseAndIdNot(
            UUID projectId, String hostname, UUID id) {
        return repository.existsByProjectIdAndHostnameIgnoreCaseAndIdNot(projectId, hostname, id);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Domain toDomain(DomainJpaEntity entity) {
        return Domain.rehydrate(
                entity.getId(),
                entity.getProjectId(),
                entity.getServiceId(),
                entity.getHostname(),
                DomainStatus.valueOf(entity.getStatus()),
                entity.getVerificationToken(),
                entity.getCertificateIssuer(),
                entity.getCertificateExpiresAt(),
                entity.getCertificateSans(),
                entity.getVerifiedAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private DomainJpaEntity toEntity(Domain domain) {
        DomainJpaEntity entity = new DomainJpaEntity();
        entity.setId(domain.getId());
        entity.setProjectId(domain.getProjectId());
        entity.setServiceId(domain.getServiceId());
        entity.setHostname(domain.getHostname());
        entity.setStatus(domain.getStatus().name());
        entity.setVerificationToken(domain.getVerificationToken());
        entity.setCertificateIssuer(domain.getCertificateIssuer());
        entity.setCertificateExpiresAt(domain.getCertificateExpiresAt());
        entity.setCertificateSans(domain.getCertificateSans());
        entity.setVerifiedAt(domain.getVerifiedAt());
        entity.setLastError(domain.getLastError());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
