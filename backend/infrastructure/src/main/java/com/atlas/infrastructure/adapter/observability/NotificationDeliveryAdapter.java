package com.atlas.infrastructure.adapter.observability;

import com.atlas.application.port.out.MailSenderPort;
import com.atlas.application.port.out.NotificationDeliveryPort;
import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.NotificationChannel;
import com.atlas.domain.observability.NotificationChannelType;
import com.atlas.infrastructure.config.AtlasProperties;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Delivers product alerts: real SMTP when {@code atlas.app-smtp.host} is set; otherwise email/webhook
 * stubs (v0.7 behaviour).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryAdapter implements NotificationDeliveryPort {

    private final MailSenderPort mailSender;
    private final AtlasProperties properties;

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
            return deliverEmail(channel, detail);
        }

        String target = channel.getTarget().toLowerCase(Locale.ROOT);
        if (target.startsWith("stub://")) {
            log.info("Alert webhook stub → {}: {}", channel.getTarget(), detail);
            return new DeliveryResult(true, "webhook stub delivered to " + channel.getTarget());
        }

        log.info("Alert webhook (queued stub) → {}: {}", channel.getTarget(), detail);
        return new DeliveryResult(true, "webhook stub accepted for " + channel.getTarget());
    }

    private DeliveryResult deliverEmail(NotificationChannel channel, String detail) {
        if (!mailSender.isConfigured()) {
            log.info("Alert email stub → {}: {}", channel.getTarget(), detail);
            return new DeliveryResult(true, "email stub delivered to " + channel.getTarget());
        }

        AtlasProperties.AppSmtp smtp = properties.getAppSmtp();
        String from = smtp.getAlertFrom();
        if (from == null || from.isBlank()) {
            from = "noreply@" + smtp.getFromDomain();
        }
        MailSenderPort.SendResult result = mailSender.send(new MailSenderPort.SendRequest(
                smtp.getHost(),
                smtp.getPort(),
                smtp.isTls(),
                smtp.isAuth(),
                smtp.getUsername(),
                smtp.getPassword(),
                from,
                List.of(channel.getTarget()),
                Optional.empty(),
                Optional.empty(),
                "Atlas alert",
                detail,
                Optional.empty()));
        if (result.sent()) {
            log.info("Alert email sent → {}", channel.getTarget());
            return new DeliveryResult(true, "email delivered to " + channel.getTarget());
        }
        log.warn("Alert email failed → {}: {}", channel.getTarget(), result.detail());
        return new DeliveryResult(false, result.detail());
    }
}
