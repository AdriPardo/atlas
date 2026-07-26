package com.atlas.platform.infrastructure.security;

import com.atlas.platform.domain.port.out.UserRepositoryPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AtlasUserDetailsService implements UserDetailsService {

    private final UserRepositoryPort userRepository;

    public AtlasUserDetailsService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(AtlasUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
