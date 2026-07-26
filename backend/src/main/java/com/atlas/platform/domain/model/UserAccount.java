package com.atlas.platform.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class UserAccount {

    private final UUID id;
    private final UUID installationId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public UserAccount(
            UUID id,
            UUID installationId,
            String username,
            String passwordHash,
            Role role,
            boolean enabled) {
        this.id = Objects.requireNonNull(id);
        this.installationId = Objects.requireNonNull(installationId);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.enabled = enabled;
    }

    public UUID getId() { return id; }
    public UUID getInstallationId() { return installationId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
}
