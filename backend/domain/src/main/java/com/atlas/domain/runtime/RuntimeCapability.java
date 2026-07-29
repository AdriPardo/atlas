package com.atlas.domain.runtime;

import java.util.Locale;

/**
 * Host capacity tag for which runtime adapters can run stacks (ADR-0014 phase D).
 * Wire format: lowercase ({@code compose}, {@code podman}, {@code k8s}, …).
 */
public enum RuntimeCapability {
    COMPOSE,
    PODMAN,
    K8S,
    SYSTEMD;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RuntimeCapability fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("runtime capability tag is required");
        }
        return RuntimeCapability.valueOf(tag.trim().toUpperCase(Locale.ROOT));
    }
}
