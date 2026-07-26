package com.atlas.application.host;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.domain.host.Host;
import com.atlas.domain.shared.ConflictException;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateHostUseCase {

    private final HostRepositoryPort hostRepository;

    @Transactional
    public Host execute(UUID id, UpdateHostCommand command) {
        Host host = hostRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Host not found: " + id));

        if (hostRepository.existsByHostnameAndIdNot(command.hostname(), id)) {
            throw new ConflictException("Host hostname already exists: " + command.hostname());
        }

        host.update(
                command.hostname(),
                command.ip(),
                command.operatingSystem(),
                command.dockerVersion(),
                command.online());
        return hostRepository.save(host);
    }

    public record UpdateHostCommand(
            String hostname, String ip, String operatingSystem, String dockerVersion, boolean online) {}
}
