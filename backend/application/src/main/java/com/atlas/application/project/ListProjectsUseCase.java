package com.atlas.application.project;

import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.project.Project;
import com.atlas.domain.project.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListProjectsUseCase {

    private final ProjectRepositoryPort projectRepository;

    @Transactional(readOnly = true)
    public PageResult<Project> execute(String name, ProjectStatus status, PageQuery pageQuery) {
        return projectRepository.search(name, status, pageQuery);
    }
}
