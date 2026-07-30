package com.atlas.infrastructure.billing;

import com.atlas.application.port.out.FeatureFlagPort;
import com.atlas.domain.billing.FeatureFlags;
import com.atlas.domain.billing.PlanCodes;
import com.atlas.infrastructure.config.AtlasProperties;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Config-driven local plan + feature flags. Default plan {@code community}; set {@code
 * ATLAS_PLAN_CODE=enterprise} for enterprise entitlements and audit export.
 */
@Component
public class LocalFeatureFlagAdapter implements FeatureFlagPort {

    private final AtlasProperties properties;

    public LocalFeatureFlagAdapter(AtlasProperties properties) {
        this.properties = properties;
    }

    @Override
    public String currentPlanCode() {
        return PlanCodes.normalize(properties.getPlan().getCode());
    }

    @Override
    public boolean isEnabled(String flag) {
        if (flag == null || flag.isBlank()) {
            return false;
        }
        Boolean value = allFlags().get(flag.trim().toLowerCase(Locale.ROOT));
        return Boolean.TRUE.equals(value);
    }

    @Override
    public Map<String, Boolean> allFlags() {
        boolean enterprise = PlanCodes.isEnterprise(currentPlanCode());
        boolean billing = properties.getFeatures().isBilling();
        boolean auditExport = resolveAuditExport(enterprise);

        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put(FeatureFlags.ENTERPRISE, enterprise);
        flags.put(FeatureFlags.BILLING, billing);
        flags.put(FeatureFlags.AUDIT_EXPORT, auditExport);
        return Map.copyOf(flags);
    }

    private boolean resolveAuditExport(boolean enterprise) {
        String override = properties.getFeatures().getAuditExport();
        if (override != null && !override.isBlank()) {
            return Boolean.parseBoolean(override.trim());
        }
        return enterprise;
    }
}
