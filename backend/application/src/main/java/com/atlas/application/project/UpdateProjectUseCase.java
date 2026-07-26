package com.atlas.application.project;

import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final ProjectRepositoryPort projectRepository;

    @Transactional
    public Project execute(UUID id, UpdateProjectCommand command) {
        Project project = projectRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
        if (projectRepository.existsByNameAndIdNot(command.name(), id)) {
            throw new ConflictException("Project name already exists: " + command.name());
        }
        String slug = Project.slugify(command.name());
        if (projectRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("Project slug already exists: " + slug);
        }
        project.update(command.name(), command.description(), command.status());
        return projectRepository.save(project);
    }

    public record UpdateProjectCommand(String name, String description, ProjectStatus status) {}
}
