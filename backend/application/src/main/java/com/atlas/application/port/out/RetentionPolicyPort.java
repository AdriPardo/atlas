package com.atlas.application.port.out;

public interface RetentionPolicyPort {

    boolean enabled();

    int jobsRetentionDays();

    int pipelineRunsRetentionDays();
}
