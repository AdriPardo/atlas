package com.atlas.infrastructure.adapter.observability;

import com.atlas.application.port.out.NotificationDeliveryPort;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.NotificationChannel;
import com.atlas.domain.observability.NotificationChannelType;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Minimal delivery stub: emails and stub:// webhooks succeed in-process;
 * http(s) webhooks are acknowledged without an outbound call (v0.7).
 */
@Slf4j
@Component
public class StubNotificationDeliveryAdapter implements NotificationDeliveryPort {

    @Override
    public DeliveryResult deliver(
            NotificationChannel channel,
            AlertEventType eventType,
            String ruleName,
            UUID projectId,
            String message,
            String resourceType,
            UUID resourceId) {
        if (!channel.isEnabled()) {
            return new DeliveryResult(false, "channel disabled");
        }

        String detail = "event="
                + eventType
                + " rule="
                + ruleName
                + " resource="
                + resourceType
                + "/"
                + resourceId
                + " project="
                + projectId
                + " message="
                + (message == null ? "" : message);

        if (channel.getType() == NotificationChannelType.EMAIL) {
            log.info("Alert email stub → {}: {}", channel.getTarget(), detail);
            return new DeliveryResult(true, "email stub delivered to " + channel.getTarget());
        }

        String target = channel.getTarget().toLowerCase(Locale.ROOT);
        if (target.startsWith("stub://")) {
            log.info("Alert webhook stub → {}: {}", channel.getTarget(), detail);
            return new DeliveryResult(true, "webhook stub delivered to " + channel.getTarget());
        }

        // http(s) webhooks: acknowledge without outbound HTTP in v0.7 control-plane stub
        log.info("Alert webhook (queued stub) → {}: {}", channel.getTarget(), detail);
        return new DeliveryResult(true, "webhook stub accepted for " + channel.getTarget());
    }
}
