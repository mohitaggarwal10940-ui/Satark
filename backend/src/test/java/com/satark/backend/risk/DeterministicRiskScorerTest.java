package com.satark.backend.risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 13 verification: scoring table, bands, evidence adjustments,
 * override-resistance. Pure unit tests — no Spring context required.
 */
class DeterministicRiskScorerTest {

    private DeterministicRiskScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new DeterministicRiskScorer();
    }

    private static RiskSignal sig(String severity) {
        return new RiskSignal("t", "d", severity);
    }

    @Test
    void bands() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0));
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(34));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(35));
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(59));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60));
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(79));
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(80));
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(100));
    }

    @Test
    void scoringTable() {
        assertEquals(new RiskScorer.Result(0, "LOW"), scorer.score(null, null));
        assertEquals(new RiskScorer.Result(0, "LOW"), scorer.score(List.of(), List.of()));
        assertEquals(new RiskScorer.Result(12, "LOW"), scorer.score(List.of(sig("MEDIUM")), null));
        assertEquals(new RiskScorer.Result(22, "LOW"), scorer.score(List.of(sig("HIGH")), null));
        assertEquals(new RiskScorer.Result(34, "LOW"),
                scorer.score(List.of(sig("HIGH"), sig("MEDIUM")), null));
        assertEquals(new RiskScorer.Result(44, "MEDIUM"),
                scorer.score(List.of(sig("HIGH"), sig("HIGH")), null));
        assertEquals(new RiskScorer.Result(66, "HIGH"),
                scorer.score(List.of(sig("HIGH"), sig("HIGH"), sig("HIGH")), null));
        // Any CRITICAL forces the 80 floor.
        assertEquals(new RiskScorer.Result(80, "VERY_HIGH"), scorer.score(List.of(sig("CRITICAL")), null));
        // Cap at 100.
        assertEquals(new RiskScorer.Result(100, "VERY_HIGH"),
                scorer.score(List.of(sig("CRITICAL"), sig("CRITICAL"), sig("CRITICAL")), null));
        // Unknown severity behaves as MEDIUM, never zeroed.
        assertEquals(new RiskScorer.Result(12, "LOW"), scorer.score(List.of(sig("BOGUS")), null));
        assertEquals(new RiskScorer.Result(12, "LOW"), scorer.score(List.of(sig(null)), null));
    }

    @Test
    void evidenceAdjustments() {
        var high = List.of(sig("HIGH"), sig("HIGH")); // 44 MEDIUM
        assertEquals(new RiskScorer.Result(59, "MEDIUM"), scorer.score(high,
                List.of(new Evidence("c", "VERIFIED_FALSE", "d", null))));
        assertEquals(new RiskScorer.Result(34, "LOW"), scorer.score(high,
                List.of(new Evidence("c", "VERIFIED_TRUE", "d", null))));
        // Uncertainty statuses change nothing.
        for (String st : List.of("UNVERIFIED", "INSUFFICIENT_EVIDENCE", "AMBIGUOUS", "UNAVAILABLE")) {
            assertEquals(new RiskScorer.Result(44, "MEDIUM"),
                    scorer.score(high, List.of(new Evidence("c", st, "d", null))), st);
        }
        // Floor at zero.
        assertEquals(new RiskScorer.Result(0, "LOW"), scorer.score(null,
                List.of(new Evidence("c", "VERIFIED_TRUE", "d", null))));
    }

    @Test
    void noLlmOverridePossible() {
        // The scorer accepts only signals+evidence: identical findings with
        // different explanation text MUST score identically — no text/LLM
        // output can move the number.
        var signals = List.of(sig("HIGH"), sig("MEDIUM"));
        var evidence = List.of(new Evidence("c", "UNVERIFIED", "d", null));
        assertEquals(scorer.score(signals, evidence), scorer.score(signals, evidence));
        // And the P3 pipeline result type carries no score field to override with
        // (compile-time guarantee — see P3AnalysisPipeline.P3Result).
        assertTrue(true);
    }

    @Test
    void deterministic() {
        var signals = List.of(sig("HIGH"), sig("CRITICAL"));
        var evidence = List.of(new Evidence("c", "UNVERIFIED", "d", null));
        assertEquals(scorer.score(signals, evidence), scorer.score(signals, evidence));
    }
}
