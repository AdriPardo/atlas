package com.atlas.api.web;

import com.atlas.api.dto.request.CreateProjectMembershipRequest;
import com.atlas.api.dto.request.UpdateProjectMembershipRequest;
import com.atlas.api.dto.response.ProjectMembershipResponse;
import com.atlas.application.access.ManageProjectMembershipUseCase;
import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.domain.access.ProjectMembership;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/memberships")
@RequiredArgsConstructor
public class ProjectMembershipController {

    private final ManageProjectMembershipUseCase manageProjectMembershipUseCase;
    private final ProjectAuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<ProjectMembershipResponse>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(manageProjectMembershipUseCase.list(projectId).stream()
                .map(this::toResponse)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ProjectMembershipResponse> add(
            @PathVariable UUID projectId, @Valid @RequestBody CreateProjectMembershipRequest request) {
        var role = authorizationService.requireMembershipRole(request.role());
        ProjectMembership membership = manageProjectMembershipUseCase.add(projectId, request.userId(), role);
        return ResponseEntity.created(URI.create(
                        "/api/v1/projects/" + projectId + "/memberships/" + membership.getId()))
                .body(toResponse(membership));
    }

    @PutMapping("/{membershipId}")
    public ResponseEntity<ProjectMembershipResponse> update(
            @PathVariable UUID projectId,
            @PathVariable UUID membershipId,
            @Valid @RequestBody UpdateProjectMembershipRequest request) {
        var role = authorizationService.requireMembershipRole(request.role());
        return ResponseEntity.ok(toResponse(manageProjectMembershipUseCase.update(membershipId, role)));
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> remove(@PathVariable UUID projectId, @PathVariable UUID membershipId) {
        manageProjectMembershipUseCase.remove(membershipId);
        return ResponseEntity.noContent().build();
    }

    private ProjectMembershipResponse toResponse(ProjectMembership membership) {
        return new ProjectMembershipResponse(
                membership.getId(),
                membership.getProjectId(),
                membership.getUserId(),
                membership.getRole().name(),
                membership.getCreatedAt(),
                membership.getUpdatedAt());
    }
}
