package com.atlas.infrastructure.persistence.jpa.adapter;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.application.Application;
import com.atlas.domain.application.ApplicationStatus;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import com.atlas.domain.service.ServiceStatus;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anti-corruption layer: legacy Application aggregate over Project + default Service (ADR-0004).
 * Application.id maps to Project.id.
 */
@Component
@RequiredArgsConstructor
public class ApplicationRepositoryAdapter implements ApplicationRepositoryPort {

    private final ProjectRepositoryPort projectRepository;
    private final ServiceRepositoryPort serviceRepository;

    @Override
    @Transactional
    public Application save(Application application) {
        ProjectStatus projectStatus = ProjectStatus.valueOf(application.getStatus().name());
        ServiceStatus serviceStatus = ServiceStatus.valueOf(application.getStatus().name());

        Optional<Project> existing = projectRepository.findById(application.getId());
        Project toSave;
        if (existing.isPresent()) {
            toSave = existing.get();
            toSave.update(application.getName(), application.getDescription(), projectStatus);
        } else {
            toSave = Project.rehydrate(
                    application.getId(),
                    com.atlas.domain.organization.Organization.DEFAULT_ID,
                    application.getName(),
                    Project.slugify(application.getName()),
                    application.getDescription(),
                    projectStatus,
                    application.getCreatedAt(),
                    application.getUpdatedAt());
        }
        final Project project = projectRepository.save(toSave);

        ServiceUnit service = serviceRepository
                .findDefaultByProjectId(project.getId())
                .orElseGet(() -> ServiceUnit.createDefault(
                        project.getId(),
                        application.getRepositoryUrl(),
                        application.getBranch(),
                        application.getComposePath(),
                        application.getDomain()));
        service.update(
                service.getName(),
                application.getRepositoryUrl(),
                application.getBranch(),
                application.getComposePath(),
                application.getDomain(),
                service.getEnvironment(),
                serviceStatus);
        serviceRepository.save(service);

        return toApplication(project, service);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Application> findById(UUID id) {
        return projectRepository.findById(id).flatMap(project -> serviceRepository
                .findDefaultByProjectId(project.getId())
                .map(service -> toApplication(project, service)));
    }

    @Override
    public boolean existsByName(String name) {
        return projectRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return projectRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Application> search(String name, ApplicationStatus status, PageQuery pageQuery) {
        ProjectStatus projectStatus = status == null ? null : ProjectStatus.valueOf(status.name());
        PageResult<Project> projects = projectRepository.search(name, projectStatus, pageQuery);
        List<Application> content = new ArrayList<>();
        for (Project project : projects.content()) {
            ServiceUnit service = serviceRepository
                    .findDefaultByProjectId(project.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Default service missing for project: " + project.getId()));
            content.add(toApplication(project, service));
        }
        return PageResult.of(
                content, projects.page(), projects.size(), projects.totalElements(), projects.sort());
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        projectRepository.deleteById(id);
    }

    @Override
    public long count() {
        return projectRepository.count();
    }

    private static Application toApplication(Project project, ServiceUnit service) {
        return Application.rehydrate(
                project.getId(),
                project.getName(),
                project.getDescription(),
                service.getRepositoryUrl(),
                service.getBranch(),
                service.getComposePath(),
                service.getDomain(),
                ApplicationStatus.valueOf(service.getStatus().name()),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
