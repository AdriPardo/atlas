package com.atlas.application.application;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.domain.application.Application;
import com.atlas.domain.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateApplicationUseCase {

    private final ApplicationRepositoryPort applicationRepository;

    @Transactional
    public Application execute(CreateApplicationCommand command) {
        if (applicationRepository.existsByName(command.name())) {
            throw new ConflictException("Application name already exists: " + command.name());
        }
        Application application = Application.create(
                command.name(),
                command.description(),
                command.repositoryUrl(),
                command.branch(),
                command.composePath(),
                command.domain());
        return applicationRepository.save(application);
    }

    public record CreateApplicationCommand(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain) {}
}
