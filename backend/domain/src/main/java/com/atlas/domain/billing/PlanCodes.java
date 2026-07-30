package com.atlas.domain.billing;

import java.util.Locale;

/** Local commercial plan codes (self-hosted; no Stripe). */
public final class PlanCodes {

    public static final String COMMUNITY = "community";
    public static final String ENTERPRISE = "enterprise";

    private PlanCodes() {}

    public static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return COMMUNITY;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        if (ENTERPRISE.equals(normalized)) {
            return ENTERPRISE;
        }
        return COMMUNITY;
    }

    public static boolean isEnterprise(String code) {
        return ENTERPRISE.equals(normalize(code));
    }
}
