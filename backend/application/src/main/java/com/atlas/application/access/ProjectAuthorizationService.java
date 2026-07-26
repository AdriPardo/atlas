package com.atlas.application.access;

import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.domain.access.ProjectMemberRole;
import com.atlas.domain.access.ProjectMembership;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.UnauthorizedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectAuthorizationService {

    private final CurrentUserPort currentUserPort;
    private final ProjectMembershipRepositoryPort membershipRepository;

    @Transactional(readOnly = true)
    public CurrentUserPort.Actor requireActor() {
        return currentUserPort
                .current()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    @Transactional(readOnly = true)
    public void require(UUID projectId, ProjectPermission permission) {
        CurrentUserPort.Actor actor = requireActor();
        if (actor.isAdmin()) {
            return;
        }
        ProjectMembership membership = membershipRepository
                .findByProjectIdAndUserId(projectId, actor.id())
                .orElseThrow(() -> new ForbiddenException("No membership for project"));
        if (!membership.getRole().allows(permission)) {
            throw new ForbiddenException("Insufficient project role for " + permission.name());
        }
    }

    @Transactional(readOnly = true)
    public boolean can(UUID projectId, ProjectPermission permission) {
        return currentUserPort.current()
                .map(actor -> {
                    if (actor.isAdmin()) {
                        return true;
                    }
                    return membershipRepository
                            .findByProjectIdAndUserId(projectId, actor.id())
                            .map(ProjectMembership::getRole)
                            .map(role -> role.allows(permission))
                            .orElse(false);
                })
                .orElse(false);
    }

    public ProjectMemberRole requireMembershipRole(String roleName) {
        try {
            return ProjectMemberRole.valueOf(roleName.trim().toUpperCase());
        } catch (Exception ex) {
            throw new com.atlas.domain.shared.DomainException("Invalid membership role: " + roleName);
        }
    }
}
