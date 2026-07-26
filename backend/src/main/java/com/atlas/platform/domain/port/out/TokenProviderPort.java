package com.atlas.platform.domain.port.out;

import com.atlas.platform.domain.model.Role;
import com.atlas.platform.domain.model.UserAccount;
import java.util.UUID;

public interface TokenProviderPort {

    String createAccessToken(UserAccount user);

    UUID extractUserId(String token);

    String extractUsername(String token);

    Role extractRole(String token);

    boolean isValid(String token);
}
