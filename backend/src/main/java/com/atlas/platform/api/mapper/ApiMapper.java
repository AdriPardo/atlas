package com.atlas.platform.api.mapper;

import com.atlas.platform.api.dto.response.ApplicationResponse;
import com.atlas.platform.api.dto.response.DeploymentResponse;
import com.atlas.platform.api.dto.response.HostResponse;
import com.atlas.platform.api.dto.response.PageResponse;
import com.atlas.platform.api.dto.response.UserResponse;
import com.atlas.platform.domain.model.Application;
import com.atlas.platform.domain.model.Deployment;
import com.atlas.platform.domain.model.Host;
import com.atlas.platform.domain.model.PageResult;
import com.atlas.platform.domain.model.UserAccount;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getName(),
                application.getDescription(),
                application.getRepositoryUrl(),
                application.getBranch(),
                application.getComposePath(),
                application.getDomain(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }

    public HostResponse toResponse(Host host) {
        return new HostResponse(
                host.getId(),
                host.getHostname(),
                host.getIp(),
                host.getOperatingSystem(),
                host.getDockerVersion(),
                host.isOnline(),
                host.getCreatedAt());
    }

    public DeploymentResponse toResponse(Deployment deployment) {
        return new DeploymentResponse(
                deployment.getId(),
                deployment.getApplicationId(),
                deployment.getHostId(),
                deployment.getStatus(),
                deployment.getStartedAt(),
                deployment.getFinishedAt(),
                deployment.getLogs());
    }

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getInstallationId());
    }

    public <T, R> PageResponse<R> toPage(PageResult<T> page, Function<T, R> mapper) {
        return new PageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
