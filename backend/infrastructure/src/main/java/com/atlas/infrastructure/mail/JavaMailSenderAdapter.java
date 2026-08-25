package com.atlas.infrastructure.mail;

import com.atlas.application.port.out.MailSenderPort;
import com.atlas.infrastructure.config.AtlasProperties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JavaMailSenderAdapter implements MailSenderPort {

    private final AtlasProperties properties;

    public JavaMailSenderAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return properties.getAppSmtp().isConfigured();
    }

    @Override
    public SendResult send(SendRequest request) {
        try {
            Session session = buildSession(request);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(request.from()));
            for (String recipient : request.to()) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            message.setSubject(request.subject(), "UTF-8");
            if (request.htmlBody().isPresent()) {
                message.setContent(request.htmlBody().get(), "text/html; charset=UTF-8");
            } else {
                message.setText(request.textBody(), "UTF-8");
            }
            if (request.cc().isPresent()) {
                addRecipient(message, Message.RecipientType.CC, request.cc().get());
            }
            if (request.bcc().isPresent()) {
                addRecipient(message, Message.RecipientType.BCC, request.bcc().get());
            }
            message.saveChanges();
            Transport.send(message);
            log.info("Mail sent from {} to {}", request.from(), request.to());
            return new SendResult(true, "delivered");
        } catch (MessagingException ex) {
            log.warn("Mail send failed from {}: {}", request.from(), ex.getMessage());
            return new SendResult(false, ex.getMessage());
        }
    }

    private static void addRecipient(MimeMessage message, Message.RecipientType type, String address)
            throws MessagingException {
        for (String part : address.split("[,;]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                message.addRecipient(type, new InternetAddress(trimmed));
            }
        }
    }

    private Session buildSession(SendRequest request) {
        Properties props = new Properties();
        props.put("mail.smtp.host", request.host());
        props.put("mail.smtp.port", String.valueOf(request.port()));
        props.put("mail.smtp.auth", String.valueOf(request.auth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(request.tls()));
        props.put("mail.smtp.starttls.required", String.valueOf(request.tls()));
        return Session.getInstance(
                props,
                request.auth()
                        ? new jakarta.mail.Authenticator() {
                            @Override
                            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new jakarta.mail.PasswordAuthentication(
                                        request.username(), request.password());
                            }
                        }
                        : null);
    }
}
