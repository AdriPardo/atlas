package com.atlas.application.observability;

import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.AlertRuleRepositoryPort;
import com.atlas.application.port.out.NotificationChannelRepositoryPort;
import com.atlas.application.port.out.NotificationDeliveryPort;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRule;
import com.atlas.domain.observability.NotificationChannel;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates product alert rules for deploy/job failures and delivers via configured channels.
 * Best-effort: delivery failures are recorded on the rule and never abort the caller.
 */
@Service
@RequiredArgsConstructor
public class EvaluateProductAlertsUseCase {

    private final AlertRuleRepositoryPort alertRuleRepository;
    private final NotificationChannelRepositoryPort channelRepository;
    private final NotificationDeliveryPort notificationDeliveryPort;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional
    public int execute(
            AlertEventType eventType,
            UUID projectId,
            String message,
            String resourceType,
            UUID resourceId) {
        List<AlertRule> rules = alertRuleRepository.findByEventType(eventType).stream()
                .filter(AlertRule::isActive)
                .filter(rule -> rule.matchesProject(projectId))
                .toList();
        int fired = 0;
        for (AlertRule rule : rules) {
            try {
                fire(rule, eventType, projectId, message, resourceType, resourceId);
                fired++;
            } catch (Exception ex) {
                rule.markDeliveryError(ex.getMessage() == null ? "delivery failed" : ex.getMessage());
                alertRuleRepository.save(rule);
            }
        }
        return fired;
    }

    private void fire(
            AlertRule rule,
            AlertEventType eventType,
            UUID projectId,
            String message,
            String resourceType,
            UUID resourceId) {
        NotificationChannel channel = channelRepository
                .findById(rule.getChannelId())
                .orElse(null);
        if (channel == null || !channel.isEnabled()) {
            rule.markDeliveryError("channel missing or disabled");
            alertRuleRepository.save(rule);
            return;
        }

        NotificationDeliveryPort.DeliveryResult result = notificationDeliveryPort.deliver(
                channel, eventType, rule.getName(), projectId, message, resourceType, resourceId);

        if (result.delivered()) {
            rule.markFired();
        } else {
            rule.markDeliveryError(result.detail());
        }
        alertRuleRepository.save(rule);

        recordAuditUseCase.execute(
                "ALERT_FIRED",
                "alert_rule",
                rule.getId(),
                "{\"eventType\":\""
                        + eventType
                        + "\",\"channelId\":\""
                        + channel.getId()
                        + "\",\"delivered\":"
                        + result.delivered()
                        + ",\"detail\":\""
                        + escape(result.detail())
                        + "\"}");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
