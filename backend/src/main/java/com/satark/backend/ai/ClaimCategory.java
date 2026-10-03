package com.satark.backend.ai;

/**
 * Phase 2 (P3): canonical claim categories.
 *
 * <p>Matches README Phase 3 taxonomy. Serialized as {@code code} into
 * {@code Claim.category} (String) so the /api/analyze contract is unchanged.
 * {@code REGISTRATION_CLAIM} is kept as a legacy alias of
 * {@code REGULATORY_IMPERSONATION} because the current mock + Android code
 * use the former string.
 */
public enum ClaimCategory {
    GUARANTEED_RETURN("GUARANTEED_RETURN"),
    REGULATORY_IMPERSONATION("REGULATORY_IMPERSONATION"),
    URGENCY_PRESSURE("URGENCY_PRESSURE"),
    UNOFFICIAL_COMMUNICATION("UNOFFICIAL_COMMUNICATION");

    /** Legacy alias used by existing mock/Android payloads. */
    public static final String REGISTRATION_CLAIM_ALIAS = "REGISTRATION_CLAIM";

    private final String code;

    ClaimCategory(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
