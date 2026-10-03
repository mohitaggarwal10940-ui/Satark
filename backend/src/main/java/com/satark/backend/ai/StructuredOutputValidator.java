package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Phase 4 (P3): structured-output validation.
 *
 * <p>Enforces the /api/analyze contract on anything a future LLM (or the
 * deterministic extractor) produces <em>before</em> it reaches
 * AnalysisService persistence: allowlisted categories/severities, non-blank
 * bounded text, sane list sizes. Invalid entries are dropped (never repaired
 * by invention); if everything is invalid the caller must use the
 * deterministic fallback (Phase 5), never fabricated data.
 *
 * <p>Pure static utility: null-safe, deterministic, no I/O.
 */
public final class StructuredOutputValidator {

    private StructuredOutputValidator() {
    }

    private static final Set<String> CLAIM_CODES = Set.of(
            "GUARANTEED_RETURN",
            "REGULATORY_IMPERSONATION",
            "REGISTRATION_CLAIM",
            "URGENCY_PRESSURE",
            "UNOFFICIAL_COMMUNICATION");

    private static final Set<String> CONFIDENCES = Set.of("HIGH", "MEDIUM", "LOW");

    private static final Set<String> SEVERITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");

    private static final int MAX_TEXT = 500;
    private static final int MAX_ITEMS = 10;

    /** Validation outcome: surviving items + human-readable error codes (no user text). */
    public record ValidationResult<T>(List<T> items, List<String> errors, boolean valid) {
    }

    /** Validate + sanitize claims. Never returns null; never throws on bad input. */
    public static ValidationResult<Claim> validateClaims(List<Claim> claims) {
        List<Claim> clean = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (claims == null) {
            errors.add("claims_null");
            return new ValidationResult<>(List.of(), errors, false);
        }
        if (claims.size() > MAX_ITEMS) {
            errors.add("claims_truncated_to_" + MAX_ITEMS);
        }
        for (Claim c : claims.stream().limit(MAX_ITEMS).toList()) {
            if (c == null || c.getClaimText() == null || c.getClaimText().isBlank()) {
                errors.add("claim_blank_text_dropped");
                continue;
            }
            String code = c.getCategory() == null ? "" : c.getCategory().trim().toUpperCase(Locale.ROOT);
            if (!CLAIM_CODES.contains(code)) {
                errors.add("claim_unknown_category_dropped:" + sanitizeCode(c.getCategory()));
                continue;
            }
            String conf = c.getConfidence() == null ? "MEDIUM" : c.getConfidence().trim().toUpperCase(Locale.ROOT);
            if (!CONFIDENCES.contains(conf)) {
                errors.add("claim_bad_confidence_defaulted_MEDIUM");
                conf = "MEDIUM";
            }
            String text = c.getClaimText().trim();
            if (text.length() > MAX_TEXT) {
                text = text.substring(0, MAX_TEXT);
                errors.add("claim_text_truncated");
            }
            clean.add(new Claim(text, code, conf));
        }
        return new ValidationResult<>(List.copyOf(clean), List.copyOf(errors), errors.isEmpty());
    }

    /** Validate + sanitize risk signals. Never returns null; never throws on bad input. */
    public static ValidationResult<RiskSignal> validateSignals(List<RiskSignal> signals) {
        List<RiskSignal> clean = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (signals == null) {
            errors.add("signals_null");
            return new ValidationResult<>(List.of(), errors, false);
        }
        if (signals.size() > MAX_ITEMS) {
            errors.add("signals_truncated_to_" + MAX_ITEMS);
        }
        for (RiskSignal s : signals.stream().limit(MAX_ITEMS).toList()) {
            if (s == null || s.getTitle() == null || s.getTitle().isBlank()
                    || s.getDescription() == null || s.getDescription().isBlank()) {
                errors.add("signal_blank_fields_dropped");
                continue;
            }
            String sev = s.getSeverity() == null ? "MEDIUM" : s.getSeverity().trim().toUpperCase(Locale.ROOT);
            if (!SEVERITIES.contains(sev)) {
                errors.add("signal_bad_severity_defaulted_MEDIUM");
                sev = "MEDIUM";
            }
            String title = bound(s.getTitle());
            String desc = bound(s.getDescription());
            if (!title.equals(s.getTitle()) || !desc.equals(s.getDescription())) {
                errors.add("signal_text_truncated");
            }
            clean.add(new RiskSignal(title, desc, sev));
        }
        return new ValidationResult<>(List.copyOf(clean), List.copyOf(errors), errors.isEmpty());
    }

    private static String bound(String s) {
        String t = s.trim();
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) : t;
    }

    private static String sanitizeCode(String code) {
        if (code == null) {
            return "null";
        }
        String t = code.trim().toUpperCase(Locale.ROOT);
        return t.length() > 64 ? t.substring(0, 64) : (t.isEmpty() ? "blank" : t);
    }
}
