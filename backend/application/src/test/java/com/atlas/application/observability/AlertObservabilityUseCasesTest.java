package com.atlas.application.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlas.application.access.ProjectAuthorizationService;
import com.atlas.application.audit.RecordAuditUseCase;
import com.atlas.application.port.out.AlertRuleRepositoryPort;
import com.atlas.application.port.out.CurrentUserPort;
import com.atlas.application.port.out.NotificationChannelRepositoryPort;
import com.atlas.application.port.out.NotificationDeliveryPort;
import com.atlas.application.port.out.ProjectRepositoryPort;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.AlertRule;
import com.atlas.domain.observability.AlertRuleStatus;
import com.atlas.domain.observability.NotificationChannel;
import com.atlas.domain.observability.NotificationChannelType;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.user.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertObservabilityUseCasesTest {

    @Mock
    private AlertRuleRepositoryPort alertRuleRepository;

    @Mock
    private NotificationChannelRepositoryPort channelRepository;

    @Mock
    private ProjectRepositoryPort projectRepository;

    @Mock
    private ProjectAuthorizationService authorizationService;

    @Mock
    private RecordAuditUseCase recordAuditUseCase;

    @Mock
    private NotificationDeliveryPort notificationDeliveryPort;

    @InjectMocks
    private ManageNotificationChannelUseCase manageChannelUseCase;

    @InjectMocks
    private ManageAlertRuleUseCase manageAlertRuleUseCase;

    @InjectMocks
    private EvaluateProductAlertsUseCase evaluateProductAlertsUseCase;

    @Test
    void createChannelRequiresOperatorAndPersists() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));
        when(channelRepository.existsByNameIgnoreCase("Ops hook")).thenReturn(false);
        when(channelRepository.save(any(NotificationChannel.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationChannel created = manageChannelUseCase.create(
                "Ops hook", NotificationChannelType.WEBHOOK, "stub://local");

        assertEquals("Ops hook", created.getName());
        verify(recordAuditUseCase)
                .execute(eq("NOTIFICATION_CHANNEL_CREATE"), eq("notification_channel"), eq(created.getId()), anyString());
    }

    @Test
    void createChannelRejectsDuplicateName() {
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "admin", Role.ADMIN));
        when(channelRepository.existsByNameIgnoreCase("dup")).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> manageChannelUseCase.create("dup", NotificationChannelType.EMAIL, "a@b.co"));
    }

    @Test
    void createAlertRulePersistsForGlobalOperatorScope() {
        UUID channelId = UUID.randomUUID();
        when(authorizationService.requireActor())
                .thenReturn(new CurrentUserPort.Actor(UUID.randomUUID(), "ops", Role.OPERATOR));
        when(channelRepository.findById(channelId))
                .thenReturn(Optional.of(
                        NotificationChannel.create("c", NotificationChannelType.WEBHOOK, "stub://x")));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        AlertRule rule = manageAlertRuleUseCase.create(
                "Deploy failed", AlertEventType.DEPLOY_FAILED, null, channelId);

        assertEquals(AlertEventType.DEPLOY_FAILED, rule.getEventType());
        assertEquals(AlertRuleStatus.OK, rule.getStatus());
        verify(recordAuditUseCase)
                .execute(eq("ALERT_RULE_CREATE"), eq("alert_rule"), eq(rule.getId()), anyString());
    }

    @Test
    void evaluateFiresMatchingActiveRules() {
        UUID channelId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        AlertRule rule = AlertRule.create("r", AlertEventType.DEPLOY_FAILED, projectId, channelId);
        NotificationChannel channel =
                NotificationChannel.create("c", NotificationChannelType.WEBHOOK, "stub://ok");

        when(alertRuleRepository.findByEventType(AlertEventType.DEPLOY_FAILED)).thenReturn(List.of(rule));
        when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));
        when(notificationDeliveryPort.deliver(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new NotificationDeliveryPort.DeliveryResult(true, "ok"));
        when(alertRuleRepository.save(any(AlertRule.class))).thenAnswer(inv -> inv.getArgument(0));

        int fired = evaluateProductAlertsUseCase.execute(
                AlertEventType.DEPLOY_FAILED, projectId, "boom", "deployment", UUID.randomUUID());

        assertEquals(1, fired);
        assertEquals(AlertRuleStatus.FIRING, rule.getStatus());
        assertTrue(rule.getLastFiredAt() != null);
        verify(recordAuditUseCase).execute(eq("ALERT_FIRED"), eq("alert_rule"), eq(rule.getId()), anyString());
    }

    @Test
    void evaluateSkipsSilencedRules() {
        AlertRule rule = AlertRule.create("r", AlertEventType.JOB_FAILED, null, UUID.randomUUID());
        rule.silence();
        when(alertRuleRepository.findByEventType(AlertEventType.JOB_FAILED)).thenReturn(List.of(rule));

        int fired = evaluateProductAlertsUseCase.execute(
                AlertEventType.JOB_FAILED, null, "fail", "job", UUID.randomUUID());

        assertEquals(0, fired);
    }
}
