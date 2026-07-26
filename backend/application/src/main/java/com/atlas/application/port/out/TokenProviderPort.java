package com.atlas.application.port.out;

import com.atlas.domain.user.User;

public interface TokenProviderPort {

    String generateToken(User user);

    long getExpirationSeconds();
}
