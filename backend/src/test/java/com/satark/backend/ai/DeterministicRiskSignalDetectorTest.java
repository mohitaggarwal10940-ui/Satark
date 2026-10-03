package com.satark.backend.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Phase 3 verification: deterministic signal mapping.
 * Pure unit tests — no Spring context required.
 */
class DeterministicRiskSignalDetectorTest {

    private DeterministicRiskSignalDetector detector;
    private DeterministicClaimExtractor extractor;

    @BeforeEach
    void setUp() {
        detector = new DeterministicRiskSignalDetector();
        extractor = new DeterministicClaimExtractor(new AiProperties());
    }

    @Test
    void readmeExampleProducesCriticalAndHigh() {
        var claims = extractor.extract("Join our VIP Telegram group! Guaranteed 30% monthly return "
                + "with zero risk. SEBI registered analyst Amit Sharma. Only 5 slots remaining. "
                + "Pay Rs 5000 to UPI ID fastprofit@upi to start immediately.");
        List<RiskSignal> signals = detector.detect(claims, "raw");
        assertTrue(signals.size() >= 3, "expected >=3 signals, got " + signals.size());
        assertTrue(signals.stream().anyMatch(s -> "CRITICAL".equals(s.getSeverity())),
                "guaranteed+UPI combo must yield CRITICAL");
        assertTrue(signals.stream().allMatch(s -> s.getTitle() != null && s.getDescription() != null));
    }

    @Test
    void highConfidenceGuaranteeIsCritical() {
        var signals = detector.detect(
                List.of(new Claim("Guaranteed 30% monthly returns", "GUARANTEED_RETURN", "HIGH")), null);
        assertEquals(1, signals.size());
        assertEquals("CRITICAL", signals.get(0).getSeverity());
    }

    @Test
    void mediumGuaranteeIsHigh() {
        var signals = detector.detect(
                List.of(new Claim("fixed returns?", "GUARANTEED_RETURN", "MEDIUM")), null);
        assertEquals("HIGH", signals.get(0).getSeverity());
    }

    @Test
    void legacyRegistrationAliasMaps() {
        var signals = detector.detect(
                List.of(new Claim("SEBI registered advisor", "REGISTRATION_CLAIM", "MEDIUM")), null);
        assertEquals(1, signals.size());
        assertEquals("HIGH", signals.get(0).getSeverity());
    }

    @Test
    void emptyAndNullYieldEmpty() {
        assertEquals(List.of(), detector.detect(null, null));
        assertEquals(List.of(), detector.detect(List.of(), "text"));
        assertEquals(List.of(), detector.detect(
                List.of(new Claim("x", "UNKNOWN_CATEGORY", "HIGH")), null));
    }

    @Test
    void deterministic() {
        var claims = List.of(
                new Claim("a", "GUARANTEED_RETURN", "HIGH"),
                new Claim("b", "URGENCY_PRESSURE", "MEDIUM"));
        assertEquals(detector.detect(claims, "t"), detector.detect(claims, "t"));
    }
}
