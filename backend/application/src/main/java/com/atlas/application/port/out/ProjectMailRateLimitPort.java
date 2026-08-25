package com.atlas.application.port.out;

import java.util.UUID;

/** Soft rate limit for project mail sends (UTC day bucket). */
public interface ProjectMailRateLimitPort {

    /** @return true when send is allowed */
    boolean tryConsume(UUID projectId, int dailyLimit);

    int remainingToday(UUID projectId, int dailyLimit);
}
