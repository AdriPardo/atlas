package com.atlas.platform.domain.port.out;

public interface PasswordHasherPort {

    boolean matches(String rawPassword, String hashedPassword);

    String hash(String rawPassword);
}
