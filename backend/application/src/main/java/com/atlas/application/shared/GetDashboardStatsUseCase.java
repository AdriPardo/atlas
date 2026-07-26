package com.atlas.application.shared;

import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDashboardStatsUseCase {

    private final ProjectRepositoryPort projectRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional(readOnly = true)
    public DashboardStats execute() {
        long projects = projectRepository.count();
        return new DashboardStats(
                projects,
                projects, // deprecated alias for UI still reading "applications"
                hostRepository.count(),
                deploymentRepository.count());
    }

    public record DashboardStats(long projects, long applications, long hosts, long deployments) {}
}
