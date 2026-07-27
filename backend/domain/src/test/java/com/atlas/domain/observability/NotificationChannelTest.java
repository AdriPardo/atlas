package com.atlas.domain.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlas.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

class NotificationChannelTest {

    @Test
    void createWebhook() {
        NotificationChannel channel =
                NotificationChannel.create("Ops hook", NotificationChannelType.WEBHOOK, "https://hooks.example/x");
        assertTrue(channel.isEnabled());
        assertEquals(NotificationChannelType.WEBHOOK, channel.getType());
        assertEquals("https://hooks.example/x", channel.getTarget());
    }

    @Test
    void createEmailNormalizes() {
        NotificationChannel channel =
                NotificationChannel.create("Mail", NotificationChannelType.EMAIL, "Ops@Example.COM");
        assertEquals("ops@example.com", channel.getTarget());
    }

    @Test
    void rejectsInvalidWebhook() {
        assertThrows(
                DomainException.class,
                () -> NotificationChannel.create("bad", NotificationChannelType.WEBHOOK, "ftp://x"));
    }

    @Test
    void allowsStubWebhook() {
        NotificationChannel channel =
                NotificationChannel.create("stub", NotificationChannelType.WEBHOOK, "stub://local");
        assertEquals("stub://local", channel.getTarget());
    }
}
