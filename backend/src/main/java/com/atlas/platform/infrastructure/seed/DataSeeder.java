package com.atlas.platform.infrastructure.seed;

import com.atlas.platform.application.security.InstallationContext;
import com.atlas.platform.domain.model.ApplicationStatus;
import com.atlas.platform.domain.model.DeploymentStatus;
import com.atlas.platform.domain.model.Role;
import com.atlas.platform.infrastructure.persistence.entity.ApplicationJpaEntity;
import com.atlas.platform.infrastructure.persistence.entity.DeploymentJpaEntity;
import com.atlas.platform.infrastructure.persistence.entity.HostJpaEntity;
import com.atlas.platform.infrastructure.persistence.entity.UserJpaEntity;
import com.atlas.platform.infrastructure.persistence.repository.ApplicationJpaRepository;
import com.atlas.platform.infrastructure.persistence.repository.DeploymentJpaRepository;
import com.atlas.platform.infrastructure.persistence.repository.HostJpaRepository;
import com.atlas.platform.infrastructure.persistence.repository.UserJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    @Profile("!test")
    CommandLineRunner seedData(
            UserJpaRepository userRepository,
            HostJpaRepository hostRepository,
            ApplicationJpaRepository applicationRepository,
            DeploymentJpaRepository deploymentRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            UUID installationId = InstallationContext.DEFAULT_INSTALLATION_ID;
            if (userRepository.count() == 0) {
                UserJpaEntity admin = new UserJpaEntity();
                admin.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
                admin.setInstallationId(installationId);
                admin.setUsername("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);

                UserJpaEntity operator = new UserJpaEntity();
                operator.setId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
                operator.setInstallationId(installationId);
                operator.setUsername("operator");
                operator.setPasswordHash(passwordEncoder.encode("operator123"));
                operator.setRole(Role.OPERATOR);
                operator.setEnabled(true);
                userRepository.save(operator);
            }

            if (hostRepository.count() == 0) {
                HostJpaEntity host = new HostJpaEntity();
                host.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
                host.setInstallationId(installationId);
                host.setHostname("atlas-node-01");
                host.setIp("192.168.1.35");
                host.setOperatingSystem("Ubuntu 24.04 LTS");
                host.setDockerVersion("27.x");
                host.setOnline(true);
                host.setCreatedAt(Instant.now());
                hostRepository.save(host);
            }

            if (applicationRepository.count() == 0) {
                ApplicationJpaEntity app = new ApplicationJpaEntity();
                app.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
                app.setInstallationId(installationId);
                app.setName("sample-web");
                app.setDescription("Sample application registered for MVP walkthrough");
                app.setRepositoryUrl("https://github.com/example/sample-web");
                app.setBranch("main");
                app.setComposePath("docker-compose.yml");
                app.setDomain("sample.local");
                app.setStatus(ApplicationStatus.READY);
                Instant now = Instant.now();
                app.setCreatedAt(now);
                app.setUpdatedAt(now);
                applicationRepository.save(app);

                DeploymentJpaEntity deployment = new DeploymentJpaEntity();
                deployment.setId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
                deployment.setInstallationId(installationId);
                deployment.setApplicationId(app.getId());
                deployment.setHostId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
                deployment.setStatus(DeploymentStatus.SUCCEEDED);
                deployment.setStartedAt(now.minusSeconds(3600));
                deployment.setFinishedAt(now.minusSeconds(3500));
                deployment.setLogs("MVP seed deployment record. No real deploy executed.");
                deploymentRepository.save(deployment);
            }
        };
    }
}
