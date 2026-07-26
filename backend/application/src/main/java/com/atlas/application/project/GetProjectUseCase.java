package com.atlas.application.project;

import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.project.Project;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetProjectUseCase {

    private final ProjectRepositoryPort projectRepository;

    @Transactional(readOnly = true)
    public Project execute(UUID id) {
        return projectRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }
}
