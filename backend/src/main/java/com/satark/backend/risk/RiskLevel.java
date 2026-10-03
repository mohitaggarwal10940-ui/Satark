package com.satark.backend.risk;

/**
 * Phase 13: canonical concern bands (README Key Features).
 *
 * <p>0–34 LOW, 35–59 MEDIUM, 60–79 HIGH, 80–100 VERY_HIGH. Scores are
 * evidence-based concern ratings, never scam probabilities and never
 * investment advice. Serialized as {@code code} into
 * {@code AnalysisResponse.riskLevel}, so the contract is unchanged.
 */
public enum RiskLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    VERY_HIGH("VERY_HIGH");

    private final String code;

    RiskLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Band for a 0–100 score (out-of-range values are clamped first). */
    public static RiskLevel fromScore(int score) {
        int s = Math.max(0, Math.min(100, score));
        if (s >= 80) {
            return VERY_HIGH;
        }
        if (s >= 60) {
            return HIGH;
        }
        if (s >= 35) {
            return MEDIUM;
        }
        return LOW;
    }
}
