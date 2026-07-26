package com.atlas.platform.application.usecase.application;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.ConflictException;
import com.atlas.platform.domain.exception.NotFoundException;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    public UpdateApplicationUseCase(ApplicationRepositoryPort applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public Application execute(UpdateApplicationCommand command) {
        var installationId = InstallationContext.currentInstallationId();
        Application application = applicationRepository
                .findById(installationId, command.id())
                .orElseThrow(() -> new NotFoundException("Application not found: " + command.id()));
        if (applicationRepository.existsByName(installationId, command.name(), command.id())) {
            throw new ConflictException("Application name already exists: " + command.name());
        }
        application.update(
                command.name(),
                command.description(),
                command.repositoryUrl(),
                command.branch(),
                command.composePath(),
                command.domain(),
                command.status());
        return applicationRepository.save(application);
    }
}
