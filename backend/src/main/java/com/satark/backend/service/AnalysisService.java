package com.satark.backend.service;

import com.satark.backend.dto.AnalysisRequest;
import com.satark.backend.dto.AnalysisResponse;
import com.satark.backend.ai.AiProperties;
import com.satark.backend.ai.P3AnalysisPipeline;
import com.satark.backend.evidence.EvidenceProperties;
import com.satark.backend.evidence.EvidenceService;
import com.satark.backend.risk.RiskScorer;
import com.satark.backend.model.Analysis;
import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import com.satark.backend.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final AiProperties aiProperties;
    private final P3AnalysisPipeline p3Pipeline;
    private final EvidenceProperties evidenceProperties;
    private final EvidenceService evidenceService;
    private final RiskScorer riskScorer;

    public AnalysisResponse analyze(AnalysisRequest request) {

        // P3+P4 pipeline is ON by default (both flags true). Flag(s) off ->
        // byte-identical legacy mock path below (kept as fallback).
        // Enabled -> deterministic P3 claims/signals/explanation; risk score
        // and evidence stay mock until Phase 13 (scorer) and P4 (Phase 12).
        if (aiProperties != null && aiProperties.isEnabled()) {
            return analyzeWithP3(request);
        }
        return analyzeMock(request);
    }

    private AnalysisResponse analyzeWithP3(AnalysisRequest request) {
        P3AnalysisPipeline.P3Result p3 = p3Pipeline.analyze(request.text(), request.language());

        // Phase 12: real evidence only when BOTH flags are on; otherwise the
        // temporary mock evidence below (removed when P4 is fully live).
        // Risk score stays mock until Phase 13 (deterministic scorer).
        List<Evidence> evidence;
        if (evidenceProperties != null && evidenceProperties.isEnabled() && evidenceService != null) {
            evidence = evidenceService.verifyAll(p3.claims());
        } else {
            evidence = List.of(
                    new Evidence(
                            p3.claims().isEmpty()
                                    ? "No specific claim extracted"
                                    : p3.claims().get(0).getClaimText(),
                            "INSUFFICIENT_EVIDENCE",
                            "No supporting official evidence was found in this prototype analysis.",
                            null
                    )
            );
        }

        // Phase 13: the deterministic scorer is the SOLE owner of the numeric
        // score. No LLM (or any other input) can set riskScore/riskLevel.
        RiskScorer.Result risk = riskScorer.score(p3.riskSignals(), evidence);
        int riskScore = risk.score();
        String riskLevel = risk.riskLevel();

        Analysis analysis = new Analysis(
                null,
                request.inputType(),
                request.text(),
                request.language() != null ? request.language() : "en",
                riskScore,
                riskLevel,
                p3.claims(),
                p3.riskSignals(),
                evidence,
                p3.explanation(),
                p3.recommendedActions(),
                LocalDateTime.now()
        );

        Analysis savedAnalysis = analysisRepository.save(analysis);

        return new AnalysisResponse(
                savedAnalysis.getId(),
                savedAnalysis.getRiskScore(),
                savedAnalysis.getRiskLevel(),
                savedAnalysis.getClaims(),
                savedAnalysis.getRiskSignals(),
                savedAnalysis.getEvidence(),
                savedAnalysis.getExplanation(),
                savedAnalysis.getRecommendedActions()
        );
    }

    private AnalysisResponse analyzeMock(AnalysisRequest request) {

        // Temporary mock data.
        // P3/P4 integration will replace this later.

        List<Claim> claims = List.of(
                new Claim(
                        "Guaranteed 30% monthly returns",
                        "GUARANTEED_RETURN",
                        "HIGH"
                )
        );

        List<RiskSignal> riskSignals = List.of(
                new RiskSignal(
                        "Guaranteed returns",
                        "The message promises a guaranteed high return.",
                        "HIGH"
                ),
                new RiskSignal(
                        "Urgency",
                        "The message encourages the user to act immediately.",
                        "MEDIUM"
                )
        );

        List<Evidence> evidence = List.of(
                new Evidence(
                        "Guaranteed 30% monthly returns",
                        "INSUFFICIENT_EVIDENCE",
                        "No supporting official evidence was found in this prototype analysis.",
                        null
                )
        );

        List<String> recommendedActions = List.of(
                "Do not transfer money until the claim is independently verified.",
                "Verify the sender and registration details through official sources.",
                "Preserve the original message and any payment-related information."
        );

        int riskScore = 82;
        String riskLevel = "VERY_HIGH";

        String explanation =
                "This message contains multiple warning signals, including a guaranteed "
                        + "high-return claim and pressure to act quickly.";

        Analysis analysis = new Analysis(
                null,
                request.inputType(),
                request.text(),
                request.language() != null ? request.language() : "en",
                riskScore,
                riskLevel,
                claims,
                riskSignals,
                evidence,
                explanation,
                recommendedActions,
                LocalDateTime.now()
        );

        Analysis savedAnalysis = analysisRepository.save(analysis);

        return new AnalysisResponse(
                savedAnalysis.getId(),
                savedAnalysis.getRiskScore(),
                savedAnalysis.getRiskLevel(),
                savedAnalysis.getClaims(),
                savedAnalysis.getRiskSignals(),
                savedAnalysis.getEvidence(),
                savedAnalysis.getExplanation(),
                savedAnalysis.getRecommendedActions()
        );
    }
}