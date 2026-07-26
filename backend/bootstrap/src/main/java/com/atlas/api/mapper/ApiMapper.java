package com.atlas.api.mapper;

import com.atlas.api.dto.response.ApplicationResponse;
import com.atlas.api.dto.response.DeploymentResponse;
import com.atlas.api.dto.response.HostResponse;
import com.atlas.api.dto.response.UserResponse;
import com.atlas.domain.application.Application;
import com.atlas.domain.deployment.Deployment;
import com.atlas.domain.host.Host;
import com.atlas.domain.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ApiMapper {

    ApplicationResponse toApplicationResponse(Application application);

    HostResponse toHostResponse(Host host);

    DeploymentResponse toDeploymentResponse(Deployment deployment);

    UserResponse toUserResponse(User user);
}
