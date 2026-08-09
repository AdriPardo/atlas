package com.atlas.infrastructure.adapter.database;

import com.atlas.application.port.out.DbConsoleConfigPort;
import com.atlas.infrastructure.config.AtlasProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PropertiesDbConsoleConfigAdapter implements DbConsoleConfigPort {

    private final AtlasProperties properties;

    public PropertiesDbConsoleConfigAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> publicBaseUrl() {
        String raw = properties.getDbConsole().getPublicUrl();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return Optional.of(trimmed);
    }

    @Override
    public Optional<String> connectToken() {
        String raw = properties.getDbConsole().getConnectToken();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw.trim());
    }
}
