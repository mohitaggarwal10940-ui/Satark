package com.satark.backend.ai;

import java.util.Locale;

/**
 * Phase 3 (P3): regulatory rule table.
 *
 * <p>Centralizes the claim-category -&gt; signal‐severity mapping so the
 * detector stays small and the future deterministic scorer (Phase 13) can
 * reuse the same weights. No I/O, no LLM, no network.
 *
 * <p>Severity ladder: CRITICAL &gt; HIGH &gt; MEDIUM &gt; LOW. The
 * guaranteed-return + unofficial-payment combo forces a CRITICAL floor,
 * mirroring the README "critical red flags guarantee &gt;= 80" rule —
 * without computing any numeric score here.
 */
final class RegulatoryRuleEngine {

    private RegulatoryRuleEngine() {
    }

    /** Severity for a guaranteed-return claim given its confidence. */
    static String guaranteedSeverity(String confidence) {
        return "HIGH".equalsIgnoreCase(confidence) ? "CRITICAL" : "HIGH";
    }

    /** Severity for a regulatory-impersonation claim. Always HIGH: unverified credentials must be checked. */
    static String regulatorySeverity(String confidence) {
        return "HIGH";
    }

    /** Severity for urgency/pressure given confidence. */
    static String urgencySeverity(String confidence) {
        return "HIGH".equalsIgnoreCase(confidence) ? "HIGH" : "MEDIUM";
    }

    /** Severity for unofficial-channel/payment claims. */
    static String unofficialSeverity(String confidence) {
        return "HIGH".equalsIgnoreCase(confidence) ? "HIGH" : "MEDIUM";
    }

    /**
     * Combo rule: guaranteed returns collected via an unofficial channel
     * (personal UPI / Telegram VIP / WhatsApp group) is a critical pattern.
     */
    static boolean isCriticalCombo(boolean hasGuaranteed, boolean hasUnofficial) {
        return hasGuaranteed && hasUnofficial;
    }

    /** Normalize category strings from extractor/model to canonical enum; null on unknown. */
    static ClaimCategory canonical(String category) {
        if (category == null) {
            return null;
        }
        String c = category.trim().toUpperCase(Locale.ROOT);
        if (c.equals(ClaimCategory.REGISTRATION_CLAIM_ALIAS) || c.equals("REGISTRATION_CLAIM")) {
            return ClaimCategory.REGULATORY_IMPERSONATION;
        }
        for (ClaimCategory k : ClaimCategory.values()) {
            if (k.getCode().equals(c)) {
                return k;
            }
        }
        return null;
    }
}
