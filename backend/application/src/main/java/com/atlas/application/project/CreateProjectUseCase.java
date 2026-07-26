package com.atlas.application.project;

import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final ServiceRepositoryPort serviceRepository;

    @Transactional
    public ProjectWithDefaultService execute(CreateProjectCommand command) {
        if (projectRepository.existsByName(command.name())) {
            throw new ConflictException("Project name already exists: " + command.name());
        }
        String slug = Project.slugify(command.name());
        if (projectRepository.existsBySlug(slug)) {
            throw new ConflictException("Project slug already exists: " + slug);
        }

        Project project = projectRepository.save(Project.create(command.name(), command.description()));
        ServiceUnit service = serviceRepository.save(ServiceUnit.createDefault(
                project.getId(),
                command.repositoryUrl(),
                command.branch(),
                command.composePath(),
                command.domain() == null ? "" : command.domain()));
        return new ProjectWithDefaultService(project, service);
    }

    public record CreateProjectCommand(
            String name,
            String description,
            String repositoryUrl,
            String branch,
            String composePath,
            String domain) {}

    public record ProjectWithDefaultService(Project project, ServiceUnit defaultService) {}
}
