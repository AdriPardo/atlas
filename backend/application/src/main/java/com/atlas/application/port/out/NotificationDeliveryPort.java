package com.atlas.application.port.out;

import com.atlas.domain.observability.AlertEventType;
import com.atlas.domain.observability.NotificationChannel;
import java.util.UUID;

public interface NotificationDeliveryPort {

    DeliveryResult deliver(
            NotificationChannel channel,
            AlertEventType eventType,
            String ruleName,
            UUID projectId,
            String message,
            String resourceType,
            UUID resourceId);

    record DeliveryResult(boolean delivered, String detail) {}
}
