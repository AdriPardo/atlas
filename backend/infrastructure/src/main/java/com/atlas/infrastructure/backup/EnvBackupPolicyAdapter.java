package com.atlas.infrastructure.backup;

import com.atlas.application.port.out.BackupPolicyPort;
import com.atlas.infrastructure.config.AtlasProperties;
import org.springframework.stereotype.Component;

@Component
public class EnvBackupPolicyAdapter implements BackupPolicyPort {

    private final AtlasProperties properties;

    public EnvBackupPolicyAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean enabled() {
        return properties.getBackup().isEnabled();
    }

    @Override
    public String directory() {
        return properties.getBackup().getDir();
    }

    @Override
    public int keepCount() {
        return properties.getBackup().getKeepCount();
    }

    @Override
    public String cron() {
        return properties.getBackup().getCron();
    }
}
