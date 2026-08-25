package com.atlas.application.port.out;

import java.util.List;
import java.util.Optional;

/** Sends email via the platform SMTP relay (ADR-0018). */
public interface MailSenderPort {

    boolean isConfigured();

    SendResult send(SendRequest request);

    record SendRequest(
            String host,
            int port,
            boolean tls,
            boolean auth,
            String username,
            String password,
            String from,
            List<String> to,
            Optional<String> cc,
            Optional<String> bcc,
            String subject,
            String textBody,
            Optional<String> htmlBody) {}

    record SendResult(boolean sent, String detail) {}
}
