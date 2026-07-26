package com.atlas.platform.application.usecase.application;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.exception.ConflictException;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    public CreateApplicationUseCase(ApplicationRepositoryPort applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public Application execute(CreateApplicationCommand command) {
        var installationId = InstallationContext.currentInstallationId();
        if (applicationRepository.existsByName(installationId, command.name(), null)) {
            throw new ConflictException("Application name already exists: " + command.name());
        }
        Application application = Application.create(
                installationId,
                command.name(),
                command.description(),
                command.repositoryUrl(),
                command.branch() == null || command.branch().isBlank() ? "main" : command.branch(),
                command.composePath() == null || command.composePath().isBlank()
                        ? "docker-compose.yml"
                        : command.composePath(),
                command.domain());
        return applicationRepository.save(application);
    }
}
