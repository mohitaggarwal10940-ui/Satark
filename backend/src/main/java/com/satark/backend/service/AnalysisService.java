package com.satark.backend.service;

import com.satark.backend.dto.AnalysisRequest;
import com.satark.backend.dto.AnalysisResponse;
import com.satark.backend.model.Analysis;
import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;
import com.satark.backend.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;

    public AnalysisResponse analyze(AnalysisRequest request) {

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