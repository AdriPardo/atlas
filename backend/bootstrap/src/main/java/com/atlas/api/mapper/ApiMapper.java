package com.atlas.api.mapper;

import com.atlas.api.dto.response.ApplicationResponse;
import com.atlas.api.dto.response.DeploymentResponse;
import com.atlas.api.dto.response.HostResponse;
import com.atlas.api.dto.response.JobResponse;
import com.atlas.api.dto.response.ProjectResponse;
import com.atlas.api.dto.response.SecretResponse;
import com.atlas.api.dto.response.ServiceResponse;
import com.atlas.api.dto.response.UserResponse;
import com.atlas.domain.application.Application;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.host.Host;
import com.atlas.domain.job.Job;
import com.atlas.domain.project.Project;
import com.atlas.domain.secret.Secret;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiMapper {

    ApplicationResponse toApplicationResponse(Application application);

    ProjectResponse toProjectResponse(Project project);

    ServiceResponse toServiceResponse(ServiceUnit service);

    @Mapping(target = "applicationId", source = "serviceId")
    DeploymentResponse toDeploymentResponse(Deployment deployment);

    HostResponse toHostResponse(Host host);

    JobResponse toJobResponse(Job job);

    SecretResponse toSecretResponse(Secret secret);

    UserResponse toUserResponse(User user);
}
