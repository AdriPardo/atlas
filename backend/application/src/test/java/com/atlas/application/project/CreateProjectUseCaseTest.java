package com.atlas.application.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.access.ProjectMemberRole;
import com.atlas.domain.access.ProjectMembership;
import com.atlas.domain.project.Project;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.user.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateProjectUseCaseTest {

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ServiceRepositoryPort serviceRepository;

    @Mock
    private ProjectMembershipRepositoryPort membershipRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @InjectMocks
    private CreateProjectUseCase useCase;

    @Test
    void creatorIsSavedAsOperatorMember() {
        UUID userId = UUID.randomUUID();
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(userId, "apardomo", Role.ADMIN));
        when(projectRepository.existsByName("flarecost")).thenReturn(false);
        when(projectRepository.existsBySlug("flarecost")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRepository.save(any(ServiceUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(membershipRepository.save(any(ProjectMembership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new CreateProjectUseCase.CreateProjectCommand(
                "flarecost",
                "Flarecost",
                "https://github.com/AdriPardo/flarecost.git",
                "master",
                "docker-compose.atlas.yml",
                "flarecost.atlasops.dev"));

        ArgumentCaptor<ProjectMembership> membership = ArgumentCaptor.forClass(ProjectMembership.class);
        verify(membershipRepository).save(membership.capture());
        assertEquals(result.project().getId(), membership.getValue().getProjectId());
        assertEquals(userId, membership.getValue().getUserId());
        assertEquals(ProjectMemberRole.OPERATOR, membership.getValue().getRole());
        assertEquals("flarecost", result.project().getName());
        assertEquals("docker-compose.atlas.yml", result.defaultService().getComposePath());
    }
}
