package com.atlas.application.application;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    @Transactional
    public Application execute(UUID id, UpdateApplicationCommand command) {
        Application application = applicationRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found: " + id));

        if (applicationRepository.existsByNameAndIdNot(command.name(), id)) {
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

    public record UpdateApplicationCommand(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain,
            ApplicationStatus status) {}
}
