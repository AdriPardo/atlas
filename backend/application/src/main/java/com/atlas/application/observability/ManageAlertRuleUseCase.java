package com.atlas.application.observability;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.AlertRuleRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.NotificationChannelRepositoryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.access.ProjectPermission;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRule;
import com.atlas.domain.shared.ForbiddenException;
import com.atlas.domain.shared.NotFoundException;
import com.atlas.domain.user.Role;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageAlertRuleUseCase {

    private final AlertRuleRepositoryPort alertRuleRepository;
    private final NotificationChannelRepositoryPort channelRepository;
    private final ProjectRepositoryPort projectRepository;
    private final ProjectAuthorizationService authorizationService;
    private final RecordAuditUseCase recordAuditUseCase;

    @Transactional(readOnly = true)
    public List<AlertRule> list() {
        requireOperatorOrAdmin();
        return alertRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AlertRule get(UUID ruleId) {
        AlertRule rule = requireRule(ruleId);
        authorizeRead(rule);
        return rule;
    }

    @Transactional
    public AlertRule create(String name, AlertEventType eventType, UUID projectId, UUID channelId) {
        authorizeWrite(projectId);
        requireChannel(channelId);
        AlertRule saved = alertRuleRepository.save(AlertRule.create(name, eventType, projectId, channelId));
        recordAuditUseCase.execute(
                "ALERT_RULE_CREATE",
                "alert_rule",
                saved.getId(),
                "{\"name\":\""
                        + saved.getName()
                        + "\",\"eventType\":\""
                        + saved.getEventType()
                        + "\",\"channelId\":\""
                        + channelId
                        + "\"}");
        return saved;
    }

    @Transactional
    public AlertRule update(
            UUID ruleId, String name, AlertEventType eventType, UUID projectId, UUID channelId) {
        AlertRule rule = requireRule(ruleId);
        authorizeWrite(rule.getProjectId());
        if (projectId != null && (rule.getProjectId() == null || !rule.getProjectId().equals(projectId))) {
            authorizeWrite(projectId);
        }
        requireChannel(channelId);
        rule.update(name, eventType, projectId, channelId);
        AlertRule saved = alertRuleRepository.save(rule);
        recordAuditUseCase.execute(
                "ALERT_RULE_UPDATE",
                "alert_rule",
                saved.getId(),
                "{\"name\":\"" + saved.getName() + "\"}");
        return saved;
    }

    @Transactional
    public void delete(UUID ruleId) {
        AlertRule rule = requireRule(ruleId);
        authorizeWrite(rule.getProjectId());
        alertRuleRepository.deleteById(ruleId);
        recordAuditUseCase.execute(
                "ALERT_RULE_DELETE",
                "alert_rule",
                ruleId,
                "{\"name\":\"" + rule.getName() + "\"}");
    }

    @Transactional
    public AlertRule silence(UUID ruleId) {
        AlertRule rule = requireRule(ruleId);
        authorizeWrite(rule.getProjectId());
        rule.silence();
        AlertRule saved = alertRuleRepository.save(rule);
        recordAuditUseCase.execute(
                "ALERT_RULE_SILENCE",
                "alert_rule",
                saved.getId(),
                "{\"status\":\"SILENCED\"}");
        return saved;
    }

    private void authorizeRead(AlertRule rule) {
        if (rule.getProjectId() == null) {
            requireOperatorOrAdmin();
            return;
        }
        authorizationService.require(rule.getProjectId(), ProjectPermission.READ);
    }

    private void authorizeWrite(UUID projectId) {
        if (projectId == null) {
            requireOperatorOrAdmin();
            return;
        }
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new NotFoundException("Project not found: " + projectId);
        }
        authorizationService.require(projectId, ProjectPermission.WRITE);
    }

    private void requireOperatorOrAdmin() {
        CurrentUserPort.Actor actor = authorizationService.requireActor();
        if (!actor.isAdmin() && actor.role() != Role.OPERATOR) {
            throw new ForbiddenException("ADMIN or OPERATOR required");
        }
    }

    private AlertRule requireRule(UUID ruleId) {
        return alertRuleRepository
                .findById(ruleId)
                .orElseThrow(() -> new NotFoundException("Alert rule not found: " + ruleId));
    }

    private void requireChannel(UUID channelId) {
        channelRepository
                .findById(channelId)
                .orElseThrow(() -> new NotFoundException("Notification channel not found: " + channelId));
    }
}
