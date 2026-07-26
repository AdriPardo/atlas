package com.atlas.application.host;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateHostUseCase {

    private final HostRepositoryPort hostRepository;

    @Transactional
    public Host execute(CreateHostCommand command) {
        if (hostRepository.existsByHostname(command.hostname())) {
            throw new ConflictException("Host hostname already exists: " + command.hostname());
        }
        Host host = Host.create(
                command.hostname(),
                command.ip(),
                command.operatingSystem(),
                command.dockerVersion(),
                command.online());
        return hostRepository.save(host);
    }

    public record CreateHostCommand(
            String hostname, String ip, String operatingSystem, String dockerVersion, boolean online) {}
}
