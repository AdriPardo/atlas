package com.atlas.application.port.out;

/**
 * Symmetric encryption for secret values at rest.
 */
public interface SecretCipherPort {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
