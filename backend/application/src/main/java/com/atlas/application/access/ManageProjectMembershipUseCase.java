package com.atlas.application.access;

import com.atlas.application.port.out.ProjectMembershipRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.application.port.out.UserRepositoryPort;
import com.atlas.domain.access.ProjectMemberRole;
import com.atlas.domain.access.ProjectMembership;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageProjectMembershipUseCase {

    private final ProjectMembershipRepositoryPort membershipRepository;
    private final ProjectRepositoryPort projectRepository;
    private final UserRepositoryPort userRepository;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public List<ProjectMembership> list(UUID projectId) {
        authorizationService.require(projectId, ProjectPermission.READ);
        return membershipRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectMembership add(UUID projectId, UUID userId, ProjectMemberRole role) {
        authorizationService.require(projectId, ProjectPermission.MANAGE_MEMBERS);
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundException("User not found: " + userId);
        }
        if (membershipRepository.findByProjectIdAndUserId(projectId, userId).isPresent()) {
            throw new ConflictException("User already has membership on project");
        }
        return membershipRepository.save(ProjectMembership.create(projectId, userId, role));
    }

    @Transactional
    public ProjectMembership update(UUID membershipId, ProjectMemberRole role) {
        ProjectMembership membership = membershipRepository
                .findById(membershipId)
                .orElseThrow(() -> new NotFoundException("Membership not found: " + membershipId));
        authorizationService.require(membership.getProjectId(), ProjectPermission.MANAGE_MEMBERS);
        membership.updateRole(role);
        return membershipRepository.save(membership);
    }

    @Transactional
    public void remove(UUID membershipId) {
        ProjectMembership membership = membershipRepository
                .findById(membershipId)
                .orElseThrow(() -> new NotFoundException("Membership not found: " + membershipId));
        authorizationService.require(membership.getProjectId(), ProjectPermission.MANAGE_MEMBERS);
        membershipRepository.deleteById(membershipId);
    }
}
