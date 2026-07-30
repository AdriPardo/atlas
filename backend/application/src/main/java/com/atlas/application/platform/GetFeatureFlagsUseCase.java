package com.atlas.application.platform;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.port.out.FeatureFlagPort;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetFeatureFlagsUseCase {

    private final FeatureFlagPort featureFlagPort;
    private final ProjectAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public Result execute() {
        authorizationService.requireActor();
        return new Result(featureFlagPort.currentPlanCode(), featureFlagPort.allFlags());
    }

    public record Result(String planCode, Map<String, Boolean> flags) {}
}
