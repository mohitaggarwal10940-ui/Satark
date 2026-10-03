package com.satark.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 5 verification: offline fallback never fails, never fabricates,
 * never echoes user text. Pure unit tests — no Spring context required.
 */
class DeterministicFallbackExplainerTest {

    private DeterministicFallbackExplainer explainer;

    @BeforeEach
    void setUp() {
        explainer = new DeterministicFallbackExplainer();
    }

    @Test
    void englishFallbackHasThreeSafeActions() {
        var out = explainer.explain(
                List.of(new Claim("Guaranteed 30% returns", "GUARANTEED_RETURN", "HIGH")),
                List.of(new RiskSignal("t", "d", "CRITICAL")), "en");
        assertTrue(out.explanation() != null && !out.explanation().isBlank());
        assertEquals(3, out.recommendedActions().size());
        assertTrue(out.recommendedActions().stream().anyMatch(a -> a.contains("1930")));
    }

    @Test
    void hindiFallbackIsHindi() {
        var out = explainer.explain(List.of(), List.of(), "hi");
        assertTrue(out.explanation().contains("ऑफ़लाइन") || out.explanation().contains("सत्यापन"));
        assertEquals(3, out.recommendedActions().size());
    }

    @Test
    void unknownLanguageFallsBackToEnglish() {
        var unknown = explainer.explain(List.of(), List.of(), "xx");
        var english = explainer.explain(List.of(), List.of(), "en");
        assertEquals(english.explanation(), unknown.explanation());
        assertFalse(DeterministicFallbackExplainer.supports("xx"));
    }

    @Test
    void nullInputsYieldGenericSafeContent() {
        var out = explainer.explain(null, null, null);
        assertTrue(out.explanation().contains("insufficient"));
        assertEquals(3, out.recommendedActions().size());
    }

    @Test
    void neverEchoesUserText() {
        String secret = "SECRET-TOKEN-12345";
        var out = explainer.explain(
                List.of(new Claim(secret, "GUARANTEED_RETURN", "HIGH")),
                List.of(new RiskSignal(secret, secret, "HIGH")), "en");
        assertFalse(out.explanation().contains(secret));
        assertTrue(out.recommendedActions().stream().noneMatch(a -> a.contains(secret)));
    }

    @Test
    void deterministic() {
        var claims = List.of(new Claim("a", "GUARANTEED_RETURN", "HIGH"));
        var signals = List.of(new RiskSignal("t", "d", "CRITICAL"));
        assertEquals(explainer.explain(claims, signals, "en").explanation(),
                explainer.explain(claims, signals, "en").explanation());
    }
}
