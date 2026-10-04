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
    private final LlmClient llmClient;
    private final AiProperties aiProperties;

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
        String explanation;
        List<String> recommendedActions;

        try {
            String prompt = PromptBuilder.buildAnalysisPrompt(
                    text,
                    claims,
                    signals,
                    language,
                    aiProperties.getMaxInputChars()
            );
            System.out.println("SATARK LLM PROVIDER = " + llmClient.getProvider());
            System.out.println("SATARK LLM AVAILABLE = " + llmClient.isAvailable());

            if (llmClient.isAvailable()) {

                LlmResult llmResult =
                        llmClient.analyze(
                                text,
                                language,
                                prompt
                        );
                System.out.println("SATARK GROQ RESPONSE RECEIVED");
                System.out.println("SATARK GROQ ACTIONS = " + llmResult.recommendedActions());
                explanation = llmResult.explanation();
                recommendedActions = llmResult.recommendedActions();

            } else {

                SafetyAdvisor.Advice advice =
                        safetyAdvisor.advise(
                                claims,
                                signals,
                                language
                        );

                explanation = advice.explanation();
                recommendedActions = advice.recommendedActions();
            }

        } catch (Exception e) {
            System.out.println("SATARK GROQ FAILED: " + e.getMessage());
            // AI failure must never break SATARK.
            SafetyAdvisor.Advice advice =
                    safetyAdvisor.advise(
                            claims,
                            signals,
                            language
                    );

            explanation = advice.explanation();
            recommendedActions = advice.recommendedActions();
        }

        return new P3Result(
                claims,
                signals,
                explanation,
                recommendedActions
        );
    }
}
