package com.atlas.api.web;

import com.atlas.api.dto.response.FeatureFlagsResponse;
import com.atlas.api.dto.response.ObservabilitySettingsResponse;
import com.atlas.application.platform.GetFeatureFlagsUseCase;
import com.atlas.application.port.out.ObservabilitySettingsPort;
import com.atlas.application.shared.GetObservabilitySettingsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SettingsController {

    private final GetObservabilitySettingsUseCase getObservabilitySettingsUseCase;
    private final GetFeatureFlagsUseCase getFeatureFlagsUseCase;

    @GetMapping("/observability")
    @Operation(summary = "Observability deep-link settings")
    public ResponseEntity<ObservabilitySettingsResponse> observability() {
        ObservabilitySettingsPort.ObservabilitySettings settings = getObservabilitySettingsUseCase.execute();
        return ResponseEntity.ok(new ObservabilitySettingsResponse(
                settings.grafanaBaseUrl(),
                settings.lokiBaseUrl(),
                settings.configured(),
                getObservabilitySettingsUseCase.hostMetricsDeepLink("")));
    }

    @GetMapping("/features")
    @Operation(summary = "Local plan code and feature flags (authenticated)")
    public ResponseEntity<FeatureFlagsResponse> features() {
        GetFeatureFlagsUseCase.Result result = getFeatureFlagsUseCase.execute();
        return ResponseEntity.ok(new FeatureFlagsResponse(result.planCode(), result.flags()));
    }
}
