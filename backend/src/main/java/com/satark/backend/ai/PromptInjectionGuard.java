package com.satark.backend.ai;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Phase 4 (P3): prompt-injection guard.
 *
 * <p>Analyzed user content is UNTRUSTED DATA. This guard only <em>detects</em>
 * known instruction-override phrases so downstream code can flag them as
 * risk context — it never obeys them, never alters verification outcomes,
 * and never logs the raw text.
 *
 * <p>Pure static utility: null-safe, deterministic, no I/O, zero dependencies.
 */
public final class PromptInjectionGuard {

    private PromptInjectionGuard() {
    }

    private static final List<Pattern> ATTACK_PATTERNS = List.of(
            Pattern.compile("ignore\\s+(all\\s+|any\\s+)?previous\\s+instructions?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore\\s+(the\\s+)?(system|developer)\\s+(prompt|message|instructions?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you\\s+are\\s+now\\s+(an?\\s+)?(unrestricted|unfiltered|jailbroken|dan|evil)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("return\\s+this\\s+as\\s+verified", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmark\\s+(this|it)\\s+as\\s+verified\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("say\\s+this\\s+company\\s+is\\s+registered", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdo\\s+anything\\s+now\\b.{0,20}\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bjailbreak\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal\\s+(your\\s+)?system\\s+prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bypass\\s+(all\\s+)?(safety|content\\s+policy|guardrails?)", Pattern.CASE_INSENSITIVE));

    /**
     * Whether the text contains a known instruction-override phrase.
     * Null/blank -&gt; false.
     */
    public static boolean containsInjection(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (Pattern p : ATTACK_PATTERNS) {
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Bound untrusted text for any future prompt/context use: trim, strip
     * control chars (except newlines/tabs), and truncate to {@code maxChars}.
     * Never returns null.
     */
    public static String bound(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        int cap = maxChars <= 0 ? 4000 : maxChars;
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").trim();
        return cleaned.length() > cap ? cleaned.substring(0, cap) : cleaned;
    }

    /** Canonical lowercase language code fallback ("en"). */
    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        return language.trim().toLowerCase(Locale.ROOT).split("[_-]")[0];
    }
}
