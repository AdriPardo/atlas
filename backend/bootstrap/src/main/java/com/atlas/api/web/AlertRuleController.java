package com.atlas.api.web;

import com.atlas.api.dto.request.CreateAlertRuleRequest;
import com.atlas.api.dto.request.UpdateAlertRuleRequest;
import com.atlas.api.dto.response.AlertRuleResponse;
import com.atlas.application.observability.ManageAlertRuleUseCase;
import com.atlas.domain.observability.AlertRule;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertRuleController {

    private final ManageAlertRuleUseCase manageAlertRuleUseCase;

    @GetMapping
    public ResponseEntity<List<AlertRuleResponse>> list() {
        return ResponseEntity.ok(manageAlertRuleUseCase.list().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<AlertRuleResponse> get(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(toResponse(manageAlertRuleUseCase.get(ruleId)));
    }

    @PostMapping
    public ResponseEntity<AlertRuleResponse> create(@Valid @RequestBody CreateAlertRuleRequest request) {
        AlertRule rule = manageAlertRuleUseCase.create(
                request.name(), request.eventType(), request.projectId(), request.channelId());
        return ResponseEntity.created(URI.create("/api/v1/alerts/" + rule.getId())).body(toResponse(rule));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<AlertRuleResponse> update(
            @PathVariable UUID ruleId, @Valid @RequestBody UpdateAlertRuleRequest request) {
        return ResponseEntity.ok(toResponse(manageAlertRuleUseCase.update(
                ruleId, request.name(), request.eventType(), request.projectId(), request.channelId())));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable UUID ruleId) {
        manageAlertRuleUseCase.delete(ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ruleId}/silence")
    public ResponseEntity<AlertRuleResponse> silence(@PathVariable UUID ruleId) {
        return ResponseEntity.ok(toResponse(manageAlertRuleUseCase.silence(ruleId)));
    }

    private AlertRuleResponse toResponse(AlertRule rule) {
        return new AlertRuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getEventType(),
                rule.getProjectId(),
                rule.getChannelId(),
                rule.getStatus(),
                rule.getLastFiredAt(),
                rule.getLastError(),
                rule.getCreatedAt(),
                rule.getUpdatedAt());
    }
}
