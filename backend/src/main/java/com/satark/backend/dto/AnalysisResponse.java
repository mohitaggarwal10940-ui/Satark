package com.satark.backend.dto;

import com.satark.backend.model.Claim;
import com.satark.backend.model.Evidence;
import com.satark.backend.model.RiskSignal;

import java.util.List;

public record AnalysisResponse(

        String analysisId,

        int riskScore,

        String riskLevel,

        List<Claim> claims,

        List<RiskSignal> riskSignals,

        List<Evidence> evidence,

        String explanation,

        List<String> recommendedActions
) {
}