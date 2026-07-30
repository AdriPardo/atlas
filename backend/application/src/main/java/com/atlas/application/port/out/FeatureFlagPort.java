package com.atlas.application.port.out;

import java.util.Map;

/** Local plan + feature flags (config-driven; no remote flag service). */
public interface FeatureFlagPort {

    String currentPlanCode();

    boolean isEnabled(String flag);

    /** Stable map of known flags → enabled. */
    Map<String, Boolean> allFlags();
}
