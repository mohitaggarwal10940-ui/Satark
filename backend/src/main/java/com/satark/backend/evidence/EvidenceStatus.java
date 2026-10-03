package com.satark.backend.evidence;

import java.util.Locale;

/**
 * Phase 9 (P4): canonical evidence-status taxonomy.
 *
 * <p>The ONLY statuses P4 may emit. Serialized as {@code code} into
 * {@code Evidence.status} (String), so the /api/analyze contract and existing
 * Mongo documents are untouched — {@code model/Evidence.java} keeps its
 * String field by design (see {@code backend/docs/P4-EVIDENCE.md}).
 *
 * <p>Meaning contract (never fabricate):
 * <ul>
 *   <li>VERIFIED_TRUE — a trusted source confirms the claim.</li>
 *   <li>VERIFIED_FALSE — a trusted source contradicts the claim.</li>
 *   <li>UNVERIFIED — checked, but no confirmation (e.g. no registration
 *       number supplied to look up).</li>
 *   <li>INSUFFICIENT_EVIDENCE — not enough information to attempt verification.</li>
 *   <li>AMBIGUOUS — available information is conflicting or unclear.</li>
 *   <li>UNAVAILABLE — the relevant source could not be checked (offline, no
 *       access); uncertainty is preserved, never guessed.</li>
 * </ul>
 */
public enum EvidenceStatus {
    VERIFIED_TRUE("VERIFIED_TRUE", "Confirmed by a trusted source."),
    VERIFIED_FALSE("VERIFIED_FALSE", "Contradicted by a trusted source."),
    UNVERIFIED("UNVERIFIED", "Checked but not confirmed; needs a verifiable identifier."),
    INSUFFICIENT_EVIDENCE("INSUFFICIENT_EVIDENCE", "Not enough information to attempt verification."),
    AMBIGUOUS("AMBIGUOUS", "Available information is conflicting or unclear."),
    UNAVAILABLE("UNAVAILABLE", "Relevant source could not be checked; uncertainty preserved.");

    private final String code;
    private final String meaning;

    EvidenceStatus(String code, String meaning) {
        this.code = code;
        this.meaning = meaning;
    }

    public String getCode() {
        return code;
    }

    public String getMeaning() {
        return meaning;
    }

    /**
     * Normalize a raw status string to canonical form. Unknown, null, or
     * blank values collapse to INSUFFICIENT_EVIDENCE (never invent a
     * stronger claim). Case- and whitespace-tolerant.
     */
    public static EvidenceStatus fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return INSUFFICIENT_EVIDENCE;
        }
        String c = raw.trim().toUpperCase(Locale.ROOT);
        for (EvidenceStatus s : values()) {
            if (s.code.equals(c)) {
                return s;
            }
        }
        return INSUFFICIENT_EVIDENCE;
    }

    /** Whether this status makes a positive factual assertion (needs a cited source). */
    public boolean assertsFact() {
        return this == VERIFIED_TRUE || this == VERIFIED_FALSE;
    }
}
