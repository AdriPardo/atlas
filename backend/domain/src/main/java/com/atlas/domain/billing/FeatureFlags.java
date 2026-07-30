package com.atlas.domain.billing;

/** Canonical feature-flag keys for plan gating (v0.9). */
public final class FeatureFlags {

    /** True when local plan is enterprise (or explicit override). */
    public static final String ENTERPRISE = "enterprise";

    /** Billing usage + entitlements API/UI. */
    public static final String BILLING = "billing";

    /** Audit JSON export (enterprise). */
    public static final String AUDIT_EXPORT = "audit_export";

    private FeatureFlags() {}
}
