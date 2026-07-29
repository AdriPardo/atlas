package com.atlas.infrastructure.networking;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Maps opaque Cloudflare HTTP failures to operator-facing Ensure messages.
 * 403 → insufficient API token scopes (Tunnel / DNS Autopilot).
 */
final class CloudflareApiErrorMessages {

    /**
     * Stable phrase for UI detection + docs. Keep in sync with Domains panel scope alert.
     */
    static final String INSUFFICIENT_SCOPES =
            "token scopes insufficient — need Zone → DNS → Edit and Account → Cloudflare Tunnel"
                    + " / Cloudflare One → Edit on secret cloudflare.api.token"
                    + " (see Org secrets or Project secrets scopes hint)";

    private static final Pattern FORBIDDEN_STATUS =
            Pattern.compile("(?i)Cloudflare\\s+(GET|PUT|POST|PATCH)\\s+403\\b");

    private CloudflareApiErrorMessages() {}

    static boolean isForbidden(Throwable ex) {
        if (ex == null) {
            return false;
        }
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        return FORBIDDEN_STATUS.matcher(message).find()
                || message.toLowerCase(Locale.ROOT).contains("authentication error")
                        && message.contains("403");
    }

    static String failedEnsureMessage(String apiLabel, Throwable ex) {
        String raw = ex == null || ex.getMessage() == null ? "unknown error" : ex.getMessage();
        if (isForbidden(ex)) {
            return apiLabel + " failed: " + INSUFFICIENT_SCOPES + " — use copy as fallback";
        }
        return apiLabel + " failed: " + raw + " — use copy as fallback";
    }
}
