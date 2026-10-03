package com.satark.backend.ai;

import com.satark.backend.model.Claim;
import com.satark.backend.model.RiskSignal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Phase 7 (P3 integration): deterministic analysis orchestrator.
 *
 * <p>Runs extract -&gt; validate -&gt; detect -&gt; validate -&gt; advise
 * with zero LLM calls. Called by {@code AnalysisService} only when
 * {@code satark.ai.enabled=true}; the default mock path is untouched.
 *
 * <p>Scope notes: numeric risk scoring stays mock until Phase 13, and
 * evidence stays mock until P4 (Phase 12). This pipeline owns only
 * claims + signals + explanation + recommendations.
 */
@Service
@RequiredArgsConstructor
public class P3AnalysisPipeline {

    private final ClaimExtractor claimExtractor;
    private final RiskSignalDetector riskSignalDetector;
    private final SafetyAdvisor safetyAdvisor;

    /** P3-owned slice of the analysis. Fields never null. */
    public record P3Result(
            List<Claim> claims,
            List<RiskSignal> riskSignals,
            String explanation,
            List<String> recommendedActions) {
    }

    /**
     * Analyze untrusted text deterministically. Never returns null, never
     * throws on bad input (worst case: empty lists + generic safe advice).
     */
    public P3Result analyze(String text, String language) {
        List<Claim> claims;
        try {
            claims = StructuredOutputValidator.validateClaims(claimExtractor.extract(text)).items();
        } catch (RuntimeException e) {
            claims = List.of();
        }
        List<RiskSignal> signals;
        try {
            signals = StructuredOutputValidator.validateSignals(riskSignalDetector.detect(claims, text)).items();
        } catch (RuntimeException e) {
            signals = List.of();
        }
        SafetyAdvisor.Advice advice = safetyAdvisor.advise(claims, signals, language);
        return new P3Result(claims, signals, advice.explanation(), advice.recommendedActions());
    }
}
