package com.atlas.application.pipeline;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Validates GitHub (X-Hub-Signature-256) and Gitea (X-Gitea-Signature) HMAC-SHA256.
 * Operators should set the provider webhook secret to the Atlas pipeline webhook token.
 * If no signature header is present, verification is skipped (path token alone authenticates).
 */
@Component
public class GitWebhookSignatureVerifier {

    public boolean isValid(byte[] body, String webhookToken, String githubSignature, String giteaSignature) {
        boolean hasGithub = githubSignature != null && !githubSignature.isBlank();
        boolean hasGitea = giteaSignature != null && !giteaSignature.isBlank();
        if (!hasGithub && !hasGitea) {
            return true;
        }
        byte[] payload = body == null ? new byte[0] : body;
        String expectedHex = hmacSha256Hex(webhookToken, payload);
        if (hasGithub) {
            String provided = githubSignature.trim();
            if (provided.regionMatches(true, 0, "sha256=", 0, 7)) {
                provided = provided.substring(7).trim();
            }
            if (!constantTimeEquals(expectedHex, provided)) {
                return false;
            }
        }
        if (hasGitea && !constantTimeEquals(expectedHex, giteaSignature.trim())) {
            return false;
        }
        return true;
    }

    private static String hmacSha256Hex(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute webhook HMAC", ex);
        }
    }

    private static boolean constantTimeEquals(String expectedHex, String providedHex) {
        if (expectedHex == null || providedHex == null) {
            return false;
        }
        byte[] a = expectedHex.getBytes(StandardCharsets.UTF_8);
        byte[] b = providedHex.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
