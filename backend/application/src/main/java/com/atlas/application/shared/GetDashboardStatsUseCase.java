package com.atlas.application.shared;

import com.atlas.application.port.out.ApplicationRepositoryPort;
import com.atlas.application.port.out.DeploymentRepositoryPort;
import com.atlas.application.port.out.HostRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetDashboardStatsUseCase {

    private final ApplicationRepositoryPort applicationRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    @Transactional(readOnly = true)
    public DashboardStats execute() {
        return new DashboardStats(
                applicationRepository.count(),
                hostRepository.count(),
                deploymentRepository.count());
    }

    public record DashboardStats(long applications, long hosts, long deployments) {}
}
