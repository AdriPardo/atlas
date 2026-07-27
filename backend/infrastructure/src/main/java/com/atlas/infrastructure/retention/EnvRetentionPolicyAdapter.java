package com.atlas.infrastructure.retention;

import com.atlas.application.port.out.RetentionPolicyPort;
import com.atlas.infrastructure.config.AtlasProperties;
import org.springframework.stereotype.Component;

@Component
public class EnvRetentionPolicyAdapter implements RetentionPolicyPort {

    private final AtlasProperties properties;

    public EnvRetentionPolicyAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean enabled() {
        return properties.getRetention().isEnabled();
    }

    @Override
    public int jobsRetentionDays() {
        return properties.getRetention().getJobsDays();
    }

    @Override
    public int pipelineRunsRetentionDays() {
        return properties.getRetention().getPipelineRunsDays();
    }
}
