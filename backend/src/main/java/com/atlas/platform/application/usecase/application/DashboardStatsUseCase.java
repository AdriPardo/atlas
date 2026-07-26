package com.atlas.platform.application.usecase.application;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.port.out.ApplicationRepositoryPort;
import com.atlas.platform.domain.port.out.DeploymentRepositoryPort;
import com.atlas.platform.domain.port.out.HostRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardStatsUseCase {

    private final ApplicationRepositoryPort applicationRepository;
    private final HostRepositoryPort hostRepository;
    private final DeploymentRepositoryPort deploymentRepository;

    public DashboardStatsUseCase(
            ApplicationRepositoryPort applicationRepository,
            HostRepositoryPort hostRepository,
            DeploymentRepositoryPort deploymentRepository) {
        this.applicationRepository = applicationRepository;
        this.hostRepository = hostRepository;
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats execute() {
        var installationId = InstallationContext.currentInstallationId();
        long applications = applicationRepository
                .search(installationId, null, null, 0, 1, "createdAt", false)
                .totalElements();
        long running = applicationRepository
                .search(installationId, null, ApplicationStatus.RUNNING, 0, 1, "createdAt", false)
                .totalElements();
        long hosts = hostRepository.countByInstallation(installationId);
        long deployments = deploymentRepository
                .search(installationId, null, null, null, 0, 1, "startedAt", false)
                .totalElements();
        return new DashboardStats(applications, running, hosts, deployments);
    }

    public record DashboardStats(
            long applications, long runningApplications, long hosts, long deployments) {}
}
