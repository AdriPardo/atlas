package com.atlas.application.host;

import com.atlas.application.port.out.HostRepositoryPort;
import com.atlas.application.shared.PageQuery;
import com.atlas.application.shared.PageResult;
import com.atlas.domain.host.Host;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListHostsUseCase {

    private final HostRepositoryPort hostRepository;

    @Transactional(readOnly = true)
    public PageResult<Host> execute(String hostname, Boolean online, PageQuery pageQuery) {
        return hostRepository.search(hostname, online, pageQuery);
    }
}
