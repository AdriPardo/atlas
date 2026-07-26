package com.atlas.application.port.out;

public interface PasswordEncoderPort {

    boolean matches(String rawPassword, String encodedPassword);

    String encode(String rawPassword);
}
