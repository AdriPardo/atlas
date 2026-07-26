package com.atlas.application.project;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProjectsUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final ProjectMembershipRepositoryPort membershipRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResult<Project> execute(String name, ProjectStatus status, PageQuery pageQuery) {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (actor.isAdmin()) {
            return projectRepository.search(name, status, pageQuery);
        }
        List<UUID> projectIds = membershipRepository.findProjectIdsByUserId(actor.id());
        return projectRepository.searchByIds(projectIds, name, status, pageQuery);
    }
}
