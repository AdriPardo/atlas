package com.atlas.domain.mail;

import com.atlas.domain.shared.DomainException;
import java.util.Locale;
import java.util.Objects;

/**
 * ADR-0018 naming: per-project SMTP secrets ({@code smtp.*}) and sender identity
 * {@code <slug>@<fromDomain>}.
 */
public final class ProjectMailNames {

    public static final String SMTP_HOST_SECRET = "smtp.host";
    public static final String SMTP_PORT_SECRET = "smtp.port";
    public static final String SMTP_USER_SECRET = "smtp.user";
    public static final String SMTP_PASSWORD_SECRET = "smtp.password";
    public static final String SMTP_FROM_SECRET = "smtp.from";
    public static final String SMTP_TLS_SECRET = "smtp.tls";

    /** HTTP API credential stored alongside SMTP secrets after provision. */
    public static final String MAIL_API_TOKEN_SECRET = "mail.api_token";

    private ProjectMailNames() {}

    public static String senderLocalPart(String projectSlug) {
        String base = sanitizeSlug(projectSlug);
        if (base.length() > 48) {
            base = base.substring(0, 48).replaceAll("_+$", "");
        }
        return base;
    }

    public static String senderAddress(String projectSlug, String fromDomain) {
        Objects.requireNonNull(fromDomain, "fromDomain");
        String domain = fromDomain.trim().toLowerCase(Locale.ROOT);
        if (domain.isBlank()) {
            throw new DomainException("from domain is required");
        }
        return senderLocalPart(projectSlug) + "@" + domain;
    }

    public static String relayUsername(String projectSlug) {
        return "mail_" + senderLocalPart(projectSlug);
    }

    private static String sanitizeSlug(String projectSlug) {
        Objects.requireNonNull(projectSlug, "projectSlug");
        String base = projectSlug
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replaceAll("[^a-z0-9_]", "")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (base.isBlank()) {
            throw new DomainException("project slug must yield a non-empty mail identifier");
        }
        return base;
    }
}
