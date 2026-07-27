package com.atlas.application.access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.domain.access.ProjectMemberRole;
import com.atlas.domain.access.ProjectMembership;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.user.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationServiceTest {

    @Mock
    private CurrentUserPort currentUserPort;

    @Mock
    private ProjectMembershipRepositoryPort membershipRepository;

    @InjectMocks
    private ProjectAuthorizationService service;

    @Test
    void adminBypassesMembership() {
        UUID projectId = UUID.randomUUID();
        when(currentUserPort.current())
                .thenReturn(Optional.of(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN)));
        service.require(projectId, ProjectPermission.DEPLOY);
    }

    @Test
    void operatorWithoutMembershipIsForbidden() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUserPort.current())
                .thenReturn(Optional.of(new CurrentUserPort.Actor(userId, "ops", Role.OPERATOR)));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, () -> service.require(projectId, ProjectPermission.READ));
    }

    @Test
    void viewerCanReadButNotWriteOrDeploy() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUserPort.current())
                .thenReturn(Optional.of(new CurrentUserPort.Actor(userId, "viewer", Role.OPERATOR)));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(ProjectMembership.create(projectId, userId, ProjectMemberRole.VIEWER)));
        service.require(projectId, ProjectPermission.READ);
        assertThrows(ForbiddenException.class, () -> service.require(projectId, ProjectPermission.WRITE));
        assertThrows(ForbiddenException.class, () -> service.require(projectId, ProjectPermission.DEPLOY));
    }

    @Test
    void developerCanWriteButNotDeploy() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUserPort.current())
                .thenReturn(Optional.of(new CurrentUserPort.Actor(userId, "dev", Role.OPERATOR)));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(ProjectMembership.create(projectId, userId, ProjectMemberRole.DEVELOPER)));
        service.require(projectId, ProjectPermission.WRITE);
        assertThrows(ForbiddenException.class, () -> service.require(projectId, ProjectPermission.DEPLOY));
    }

    @Test
    void operatorMemberCanDeploy() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUserPort.current())
                .thenReturn(Optional.of(new CurrentUserPort.Actor(userId, "ops", Role.OPERATOR)));
        when(membershipRepository.findByProjectIdAndUserId(projectId, userId))
                .thenReturn(Optional.of(ProjectMembership.create(projectId, userId, ProjectMemberRole.OPERATOR)));
        service.require(projectId, ProjectPermission.DEPLOY);
    }
}
