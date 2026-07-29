package com.atlas.application.host;

import com.atlas.domain.runtime.RuntimeCapability;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maps host probe versions to {@link RuntimeCapability} tags (ADR-0014).
 *
 * <p>Docker engine present → {@code compose} (Atlas apply path uses {@code docker compose}). Podman
 * present → {@code podman}. Empty when neither detected (caller must not overwrite existing tags).
 */
public final class RuntimeCapabilityDetector {

    private RuntimeCapabilityDetector() {}

    public static Set<RuntimeCapability> fromProbe(String dockerVersion, String podmanVersion) {
        LinkedHashSet<RuntimeCapability> caps = new LinkedHashSet<>();
        if (hasVersion(dockerVersion)) {
            caps.add(RuntimeCapability.COMPOSE);
        }
        if (hasVersion(podmanVersion)) {
            caps.add(RuntimeCapability.PODMAN);
        }
        return Set.copyOf(caps);
    }

    public static Set<String> tagsFromProbe(String dockerVersion, String podmanVersion) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (RuntimeCapability capability : fromProbe(dockerVersion, podmanVersion)) {
            tags.add(capability.tag());
        }
        return Set.copyOf(tags);
    }

    private static boolean hasVersion(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String trimmed = version.trim().toLowerCase(Locale.ROOT);
        return !trimmed.contains("error") && !trimmed.contains("not found") && !trimmed.contains("command not found");
    }
}
