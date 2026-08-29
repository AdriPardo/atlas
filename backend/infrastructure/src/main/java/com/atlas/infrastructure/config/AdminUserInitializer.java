package com.atlas.infrastructure.config;

import com.atlas.application.port.out.PasswordEncoderPort;
import com.atlas.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.atlas.infrastructure.persistence.jpa.repository.UserJpaRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Keeps bootstrap admin in sync with ATLAS_ADMIN_* (single-tenant self-hosted). */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserJpaRepository userRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Value("${atlas.security.admin.username:admin}")
    private String adminUsername;

    @Value("${atlas.security.admin.password}")
    private String adminPassword;

    /** Extra SSO usernames to seed as ADMIN if missing (comma-separated), e.g. {@code apardomo}. */
    @Value("${atlas.security.sso.ensure-usernames:}")
    private String ensureUsernames;

    public AdminUserInitializer(UserJpaRepository userRepository, PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository
                .findByUsernameIgnoreCase(adminUsername)
                .ifPresentOrElse(this::syncAdminPassword, this::seedAdmin);

        Arrays.stream(ensureUsernames.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .filter(name -> !name.equalsIgnoreCase(adminUsername))
                .forEach(this::ensureAdminUser);
    }

    private void seedAdmin() {
        UserJpaEntity admin = new UserJpaEntity();
        admin.setId(UUID.randomUUID());
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setCreatedAt(Instant.now());
        userRepository.save(admin);
        log.info("Seeded default admin user '{}'", adminUsername);
    }

    private void ensureAdminUser(String username) {
        userRepository.findByUsernameIgnoreCase(username).ifPresentOrElse(
                existing -> log.debug("SSO ensure user '{}' already exists", username),
                () -> {
                    UserJpaEntity user = new UserJpaEntity();
                    user.setId(UUID.randomUUID());
                    user.setUsername(username);
                    user.setPasswordHash(passwordEncoder.encode("sso-ensure-" + UUID.randomUUID()));
                    user.setRole("ADMIN");
                    user.setCreatedAt(Instant.now());
                    userRepository.save(user);
                    log.info("Seeded SSO ensure admin user '{}'", username);
                });
    }

    private void syncAdminPassword(UserJpaEntity admin) {
        if (passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
            return;
        }
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);
        log.info("Synced admin user '{}' password from configuration", adminUsername);
    }
}
