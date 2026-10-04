package com.satark.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.satark.backend.ai.*;
import com.satark.backend.dto.AnalysisRequest;
import com.satark.backend.dto.AnalysisResponse;
import com.satark.backend.evidence.DeterministicClaimVerifier;
import com.satark.backend.evidence.EvidenceProperties;
import com.satark.backend.evidence.EvidenceService;
import com.satark.backend.evidence.TrustedSourceRegistry;
import com.satark.backend.model.Analysis;
import com.satark.backend.repository.AnalysisRepository;
import com.satark.backend.risk.DeterministicRiskScorer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Phase 7 verification: flag-gated integration.
 *
 * <p>Uses a {@link Proxy} fake for {@link AnalysisRepository} (assigns an ID
 * on {@code save} and captures the entity) so no MongoDB or Mockito is
 * needed. Asserts the disabled path is byte-identical to the legacy mock
 * and the enabled path returns the P3 slice in the same DTO shape.
 */
class AnalysisServiceToggleTest {

    private static final String SCAM_TEXT = "Join our VIP Telegram group! Guaranteed 30% monthly return "
            + "with zero risk. SEBI registered analyst Amit Sharma. Only 5 slots remaining. "
            + "Pay Rs 5000 to UPI ID fastprofit@upi to start immediately.";

    private AnalysisService serviceWith(boolean enabled, AtomicReference<Analysis> saved) {
        return serviceWith(enabled, false, saved);
    }

    private AnalysisService serviceWith(boolean aiEnabled, boolean evidenceEnabled,
            AtomicReference<Analysis> saved) {
        AiProperties props = new AiProperties();
        props.setEnabled(aiEnabled);
        EvidenceProperties evidenceProps = new EvidenceProperties();
        evidenceProps.setEnabled(evidenceEnabled);
        TrustedSourceRegistry sources = new TrustedSourceRegistry();
        P3AnalysisPipeline pipeline = new P3AnalysisPipeline(
                new DeterministicClaimExtractor(props),
                new DeterministicRiskSignalDetector(),
                new SafetyAdvisor(new DeterministicFallbackExplainer()),
                new NoOpLlmClient(props),
                props
        );
        return new AnalysisService(fakeRepository(saved), props, pipeline,
                evidenceProps, new EvidenceService(new DeterministicClaimVerifier(sources), sources),
                new DeterministicRiskScorer());
    }

    private AnalysisRepository fakeRepository(AtomicReference<Analysis> saved) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if (method.getName().equals("save") && args != null && args.length == 1) {
                Analysis a = (Analysis) args[0];
                if (a.getId() == null) {
                    a.setId(UUID.randomUUID().toString());
                }
                saved.set(a);
                return a;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (AnalysisRepository) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {AnalysisRepository.class}, handler);
    }

    @Test
    void disabledByDefaultReturnsLegacyMock() {
        AtomicReference<Analysis> saved = new AtomicReference<>();
        AnalysisService service = serviceWith(false, saved);
        AnalysisResponse res = service.analyze(new AnalysisRequest("TEXT", SCAM_TEXT, "en"));

        assertNotNull(res.analysisId());
        assertEquals(82, res.riskScore());
        assertEquals("VERY_HIGH", res.riskLevel());
        assertEquals(1, res.claims().size());
        assertEquals("Guaranteed 30% monthly returns", res.claims().get(0).getClaimText());
        assertEquals(2, res.riskSignals().size());
        assertEquals("This message contains multiple warning signals, including a guaranteed "
                + "high-return claim and pressure to act quickly.", res.explanation());
        assertEquals(3, res.recommendedActions().size());
        // Persistence unchanged.
        assertNotNull(saved.get());
        assertEquals("TEXT", saved.get().getInputType());
        assertEquals(SCAM_TEXT, saved.get().getInputText());
    }

    @Test
    void enabledReturnsP3SliceInSameDtoShape() {
        AtomicReference<Analysis> saved = new AtomicReference<>();
        AnalysisService service = serviceWith(true, saved);
        AnalysisResponse res = service.analyze(new AnalysisRequest("TEXT", SCAM_TEXT, "en"));

        assertNotNull(res.analysisId());
        // Same DTO shape; score now comes from the deterministic scorer
        // (Phase 13): critical combo guarantees >= 80 / VERY_HIGH.
        assertTrue(res.riskScore() >= 80, "score: " + res.riskScore());
        assertEquals("VERY_HIGH", res.riskLevel());
        assertTrue(res.claims().size() >= 3, "P3 claims: " + res.claims().size());
        assertTrue(res.riskSignals().size() >= 3, "P3 signals: " + res.riskSignals().size());
        assertTrue(res.explanation() != null && !res.explanation().isBlank());
        assertEquals(3, res.recommendedActions().size());
        assertEquals(1, res.evidence().size());
        assertEquals("INSUFFICIENT_EVIDENCE", res.evidence().get(0).getStatus());
        assertNotNull(saved.get());
        assertEquals(res.analysisId(), saved.get().getId());
    }

    @Test
    void enabledWithBenignTextStaysValid() {
        AtomicReference<Analysis> saved = new AtomicReference<>();
        AnalysisService service = serviceWith(true, saved);
        AnalysisResponse res = service.analyze(new AnalysisRequest("TEXT", "Hello, how are you?", "en"));
        assertNotNull(res.analysisId());
        assertTrue(res.explanation() != null && !res.explanation().isBlank());
        assertEquals(3, res.recommendedActions().size());
        // No signals -> scorer yields 0 / LOW.
        assertEquals(0, res.riskScore());
        assertEquals("LOW", res.riskLevel());
    }

    @Test
    void p3WithoutP4KeepsMockEvidence() {
        AtomicReference<Analysis> saved = new AtomicReference<>();
        AnalysisService service = serviceWith(true, false, saved);
        AnalysisResponse res = service.analyze(new AnalysisRequest("TEXT", SCAM_TEXT, "en"));
        assertEquals(1, res.evidence().size());
        assertEquals("INSUFFICIENT_EVIDENCE", res.evidence().get(0).getStatus());
        assertTrue(res.evidence().get(0).getSourceUrl() == null);
    }

    @Test
    void p3WithP4ReturnsVerifiedEvidence() {
        AtomicReference<Analysis> saved = new AtomicReference<>();
        AnalysisService service = serviceWith(true, true, saved);
        AnalysisResponse res = service.analyze(new AnalysisRequest("TEXT", SCAM_TEXT, "en"));
        assertNotNull(res.analysisId());
        // Same DTO shape; one evidence per P3 claim.
        assertEquals(res.claims().size(), res.evidence().size());
        assertTrue(res.evidence().stream().anyMatch(e -> e.getStatus().equals("UNVERIFIED")));
        assertTrue(res.evidence().stream()
                .filter(e -> e.getSourceUrl() != null)
                .allMatch(e -> e.getSourceUrl().startsWith("https://www.sebi.gov.in")));
        // Scorer owns the score here too.
        assertEquals("VERY_HIGH", res.riskLevel());
        assertNotNull(saved.get());
    }
}
