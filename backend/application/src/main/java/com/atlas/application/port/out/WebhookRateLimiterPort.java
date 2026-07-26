package com.atlas.application.port.out;

public interface WebhookRateLimiterPort {

    /** @return true if the request is allowed under the rate limit */
    boolean tryAcquire(String key);
}
