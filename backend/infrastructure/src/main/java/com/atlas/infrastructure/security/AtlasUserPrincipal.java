package com.atlas.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AtlasUserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public AtlasUserPrincipal(UUID id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public UUID getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
