package com.satark.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 6 verification: 8-language safety content, banned-phrase scan.
 * Pure unit tests — no Spring context required.
 */
class SafetyAdvisorTest {

    private SafetyAdvisor advisor;

    private static final List<String> LANGS = List.of("en", "hi", "ta", "te", "bn", "mr", "gu", "kn");

    private static final List<String> BANNED = List.of(
            "guaranteed safe", "safe to invest", "you should invest", "recommended investment",
            "scam probability", "definitely a scam", "definitely legit", "100% safe",
            "assured profit", "buy now");

    @BeforeEach
    void setUp() {
        advisor = new SafetyAdvisor(new DeterministicFallbackExplainer());
    }

    @Test
    void allLanguagesProduceCompleteAdvice() {
        var claims = List.of(new Claim("Guaranteed 30% returns", "GUARANTEED_RETURN", "HIGH"));
        var signals = List.of(new RiskSignal("t", "d", "CRITICAL"));
        for (String lang : LANGS) {
            var a = advisor.advise(claims, signals, lang);
            assertTrue(a.explanation() != null && !a.explanation().isBlank(), lang);
            assertEquals(3, a.recommendedActions().size(), lang);
            assertTrue(a.recommendedActions().stream().allMatch(s -> s != null && !s.isBlank()), lang);
        }
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        var claims = List.of();
        var signals = List.of();
        assertEquals(advisor.advise(claims, signals, "en").explanation(),
                advisor.advise(claims, signals, "xx").explanation());
    }

    @Test
    void nullInputsSafe() {
        var a = advisor.advise(null, null, null);
        assertTrue(a.explanation() != null && !a.explanation().isBlank());
        assertEquals(3, a.recommendedActions().size());
    }

    @Test
    void noBannedPhrasesInAnyLanguage() {
        var claims = List.of(new Claim("Guaranteed 30% returns", "GUARANTEED_RETURN", "HIGH"));
        var signals = List.of(new RiskSignal("t", "d", "CRITICAL"));
        for (String lang : LANGS) {
            var a = advisor.advise(claims, signals, lang);
            String joined = (a.explanation() + " " + String.join(" ", a.recommendedActions()))
                    .toLowerCase(Locale.ROOT);
            for (String banned : BANNED) {
                assertFalse(joined.contains(banned), lang + " contains banned: " + banned);
            }
        }
    }

    @Test
    void everyLanguageReferencesRedressal() {
        var claims = List.of(new Claim("x", "URGENCY_PRESSURE", "MEDIUM"));
        var signals = List.of(new RiskSignal("t", "d", "MEDIUM"));
        for (String lang : LANGS) {
            var a = advisor.advise(claims, signals, lang);
            String joined = String.join(" ", a.recommendedActions());
            assertTrue(joined.contains("1930"), lang + " must cite helpline 1930");
        }
    }

    @Test
    void deterministic() {
        var claims = List.of(new Claim("a", "GUARANTEED_RETURN", "HIGH"));
        var signals = List.of(new RiskSignal("t", "d", "HIGH"));
        assertEquals(advisor.advise(claims, signals, "ta").explanation(),
                advisor.advise(claims, signals, "ta").explanation());
    }
}
