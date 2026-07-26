package com.atlas.application.service;

import com.atlas.application.port.out.ServiceRepositoryPort;
import com.atlas.domain.service.ServiceUnit;
import com.atlas.domain.shared.NotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;

    @Transactional(readOnly = true)
    public ServiceUnit execute(UUID id) {
        return serviceRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }
}
