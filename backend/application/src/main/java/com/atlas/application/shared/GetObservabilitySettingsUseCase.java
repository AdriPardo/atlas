package com.atlas.application.shared;

import com.atlas.application.port.out.ObservabilitySettingsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetObservabilitySettingsUseCase {

    private final ObservabilitySettingsPort observabilitySettingsPort;

    public ObservabilitySettingsPort.ObservabilitySettings execute() {
        return observabilitySettingsPort.current();
    }

    public String containerLogsDeepLink(String containerName, String hostHostname) {
        return observabilitySettingsPort.containerLogsDeepLink(containerName, hostHostname);
    }

    public String hostMetricsDeepLink(String hostHostname) {
        return observabilitySettingsPort.hostMetricsDeepLink(hostHostname);
    }
}
