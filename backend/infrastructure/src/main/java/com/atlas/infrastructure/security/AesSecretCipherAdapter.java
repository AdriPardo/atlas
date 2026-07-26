package com.atlas.infrastructure.security;

import com.atlas.application.port.out.SecretCipherPort;
import com.atlas.domain.shared.DomainException;
import com.atlas.infrastructure.config.AtlasProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class AesSecretCipherAdapter implements SecretCipherPort {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final byte[] keyBytes;

    public AesSecretCipherAdapter(AtlasProperties properties) {
        String masterKey = properties.getSecrets().getMasterKey();
        if (masterKey == null || masterKey.isBlank()) {
            // Deterministic local fallback so tests/boot work; never use in production.
            masterKey = "atlas-dev-only-secrets-master-key!!";
        }
        this.keyBytes = deriveKey(masterKey);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new DomainException("Failed to encrypt secret: " + ex.getMessage());
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        try {
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new DomainException("Failed to decrypt secret: " + ex.getMessage());
        }
    }

    private static byte[] deriveKey(String masterKey) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new DomainException("Failed to derive secrets key");
        }
    }
}
