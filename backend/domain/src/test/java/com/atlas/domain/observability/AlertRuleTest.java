package com.atlas.domain.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertRuleTest {

    @Test
    void createStartsOk() {
        UUID channelId = UUID.randomUUID();
        AlertRule rule = AlertRule.create("Deploy failed", AlertEventType.DEPLOY_FAILED, null, channelId);
        assertEquals(AlertRuleStatus.OK, rule.getStatus());
        assertEquals(AlertEventType.DEPLOY_FAILED, rule.getEventType());
        assertTrue(rule.isActive());
        assertTrue(rule.matchesProject(UUID.randomUUID()));
    }

    @Test
    void projectScopedOnlyMatchesSameProject() {
        UUID projectId = UUID.randomUUID();
        AlertRule rule =
                AlertRule.create("Scoped", AlertEventType.JOB_FAILED, projectId, UUID.randomUUID());
        assertTrue(rule.matchesProject(projectId));
        assertFalse(rule.matchesProject(UUID.randomUUID()));
        assertFalse(rule.matchesProject(null));
    }

    @Test
    void silenceAndFire() {
        AlertRule rule =
                AlertRule.create("x", AlertEventType.DEPLOY_FAILED, null, UUID.randomUUID());
        rule.silence();
        assertEquals(AlertRuleStatus.SILENCED, rule.getStatus());
        assertFalse(rule.isActive());
        rule.unsilence();
        assertEquals(AlertRuleStatus.OK, rule.getStatus());
        rule.markFired();
        assertEquals(AlertRuleStatus.FIRING, rule.getStatus());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(
                DomainException.class,
                () -> AlertRule.create("  ", AlertEventType.DEPLOY_FAILED, null, UUID.randomUUID()));
    }
}
