package com.atlas.application.access;

import com.atlas.application.port.out.FeatureFlagPort;
import com.atlas.domain.shared.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureGateService {

    private final FeatureFlagPort featureFlagPort;

    public void require(String flag) {
        if (!featureFlagPort.isEnabled(flag)) {
            throw new ForbiddenException(
                    "Feature '" + flag + "' is not enabled for plan " + featureFlagPort.currentPlanCode());
        }
    }

    public boolean enabled(String flag) {
        return featureFlagPort.isEnabled(flag);
    }

    public String currentPlanCode() {
        return featureFlagPort.currentPlanCode();
    }
}
