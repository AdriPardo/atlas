package com.atlas.api.web;

import com.atlas.api.dto.response.ObservabilitySettingsResponse;
import com.atlas.application.port.out.ObservabilitySettingsPort;
import com.atlas.application.shared.GetObservabilitySettingsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final GetObservabilitySettingsUseCase getObservabilitySettingsUseCase;

    @GetMapping("/observability")
    public ResponseEntity<ObservabilitySettingsResponse> observability() {
        ObservabilitySettingsPort.ObservabilitySettings settings = getObservabilitySettingsUseCase.execute();
        return ResponseEntity.ok(new ObservabilitySettingsResponse(
                settings.grafanaBaseUrl(),
                settings.lokiBaseUrl(),
                settings.configured(),
                getObservabilitySettingsUseCase.hostMetricsDeepLink("")));
    }
}
