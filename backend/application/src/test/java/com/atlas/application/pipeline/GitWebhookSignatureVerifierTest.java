package com.atlas.application.pipeline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class GitWebhookSignatureVerifierTest {

    private final GitWebhookSignatureVerifier verifier = new GitWebhookSignatureVerifier();

    @Test
    void acceptsMissingSignatures() {
        assertTrue(verifier.isValid("{}".getBytes(), "secret", null, null));
    }

    @Test
    void validatesGithubSignature() throws Exception {
        byte[] body = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "atk_testsecret";
        String hex = hmac(secret, body);
        assertTrue(verifier.isValid(body, secret, "sha256=" + hex, null));
    }

    @Test
    void validatesGiteaSignature() throws Exception {
        byte[] body = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "atk_testsecret";
        String hex = hmac(secret, body);
        assertTrue(verifier.isValid(body, secret, null, hex));
    }

    @Test
    void rejectsBadGithubSignature() throws Exception {
        byte[] body = "{\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier.isValid(body, "atk_testsecret", "sha256=deadbeef", null));
    }

    private static String hmac(String secret, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
