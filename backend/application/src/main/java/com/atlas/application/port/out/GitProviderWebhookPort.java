package com.atlas.application.port.out;

import java.util.Optional;

/**
 * Registers a push webhook on a git hosting provider (GitHub today).
 * Implementations must be best-effort: failures return a result, not throw.
 */
public interface GitProviderWebhookPort {

    /**
     * @param repositoryUrl clone URL (https or ssh)
     * @param webhookUrl absolute Atlas webhook URL
     * @param secret HMAC secret (Atlas pipeline webhook token)
     * @param accessToken provider PAT (e.g. git.token)
     */
    RegisterResult registerPushWebhook(
            String repositoryUrl, String webhookUrl, String secret, String accessToken);

    record RegisterResult(boolean registered, String message, Optional<String> providerHookId) {
        public static RegisterResult ok(String message, String hookId) {
            return new RegisterResult(true, message, Optional.ofNullable(hookId));
        }

        public static RegisterResult skipped(String message) {
            return new RegisterResult(false, message, Optional.empty());
        }

        public static RegisterResult failed(String message) {
            return new RegisterResult(false, message, Optional.empty());
        }
    }
}
