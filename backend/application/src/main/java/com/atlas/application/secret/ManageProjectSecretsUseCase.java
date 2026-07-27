package com.atlas.application.secret;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ProjectSecretBindingRepositoryPort;
import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.application.port.out.SecretRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.secret.ProjectSecretBinding;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.DomainException;
import com.atlas.domain.shared.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageProjectSecretsUseCase {

    public enum EntryKind {
        OWNED,
        LINKED
    }

    public record ProjectSecretEntry(
            EntryKind kind,
            UUID secretId,
            String name,
            String secretName,
            UUID bindingId,
            String alias,
            Instant createdAt,
            Instant updatedAt) {}

    private final SecretRepositoryPort secretRepository;
    private final ProjectSecretBindingRepositoryPort bindingRepository;
    private final ProjectRepositoryPort projectRepository;
    private final SecretCipherPort secretCipher;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<ProjectSecretEntry> list(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        requireProject(projectId);

        List<ProjectSecretEntry> entries = new ArrayList<>();
        for (Secret secret : secretRepository.findByProjectId(projectId)) {
            entries.add(new ProjectSecretEntry(
                    EntryKind.OWNED,
                    secret.getId(),
                    secret.getName(),
                    secret.getName(),
                    null,
                    null,
                    secret.getCreatedAt(),
                    secret.getUpdatedAt()));
        }
        for (ProjectSecretBinding binding : bindingRepository.findByProjectId(projectId)) {
            Secret secret = secretRepository
                    .findById(binding.getSecretId())
                    .orElseThrow(() -> new NotFoundException("Bound secret not found: " + binding.getSecretId()));
            entries.add(new ProjectSecretEntry(
                    EntryKind.LINKED,
                    secret.getId(),
                    binding.getAlias(),
                    secret.getName(),
                    binding.getId(),
                    binding.getAlias(),
                    binding.getCreatedAt(),
                    binding.getCreatedAt()));
        }
        entries.sort(Comparator.comparing(ProjectSecretEntry::name, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    @Transactional
    public Secret createOwned(UUID projectId, String name, String value) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        requireProject(projectId);
        if (secretRepository.existsByProjectIdAndName(projectId, name)) {
            throw new ConflictException("Project secret name already exists: " + name);
        }
        if (bindingRepository.existsByProjectIdAndAlias(projectId, name)) {
            throw new ConflictException("Alias already linked on project: " + name);
        }
        String ciphertext = secretCipher.encrypt(value);
        return secretRepository.save(Secret.createForProject(projectId, name, ciphertext));
    }

    @Transactional
    public ProjectSecretEntry linkGlobal(UUID projectId, UUID secretId, String aliasOrNull) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        requireProject(projectId);
        Secret secret = secretRepository
                .findById(secretId)
                .orElseThrow(() -> new NotFoundException("Secret not found: " + secretId));
        if (!secret.isGlobal()) {
            throw new DomainException("Only organization/global secrets can be linked into a project");
        }
        String alias = (aliasOrNull == null || aliasOrNull.isBlank()) ? secret.getName() : aliasOrNull.trim();
        if (bindingRepository.existsByProjectIdAndSecretId(projectId, secretId)) {
            throw new ConflictException("Secret already linked to this project");
        }
        if (bindingRepository.existsByProjectIdAndAlias(projectId, alias)) {
            throw new ConflictException("Alias already linked on project: " + alias);
        }
        if (secretRepository.existsByProjectIdAndName(projectId, alias)) {
            throw new ConflictException("Project already has an owned secret named: " + alias);
        }
        ProjectSecretBinding binding =
                bindingRepository.save(ProjectSecretBinding.create(projectId, secretId, alias));
        return new ProjectSecretEntry(
                EntryKind.LINKED,
                secret.getId(),
                binding.getAlias(),
                secret.getName(),
                binding.getId(),
                binding.getAlias(),
                binding.getCreatedAt(),
                binding.getCreatedAt());
    }

    @Transactional
    public void unlink(UUID projectId, UUID bindingId) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        ProjectSecretBinding binding = bindingRepository
                .findById(bindingId)
                .orElseThrow(() -> new NotFoundException("Binding not found: " + bindingId));
        if (!binding.getProjectId().equals(projectId)) {
            throw new NotFoundException("Binding not found: " + bindingId);
        }
        bindingRepository.deleteById(bindingId);
    }

    @Transactional
    public void deleteOwned(UUID projectId, UUID secretId) {
        authorizationService.require(projectId, ProjectPermission.DEPLOY);
        Secret secret = secretRepository
                .findById(secretId)
                .orElseThrow(() -> new NotFoundException("Secret not found: " + secretId));
        if (secret.getProjectId() == null || !secret.getProjectId().equals(projectId)) {
            throw new NotFoundException("Secret not found on project: " + secretId);
        }
        secretRepository.deleteById(secretId);
    }

    private void requireProject(UUID projectId) {
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new NotFoundException("Project not found: " + projectId);
        }
    }
}
