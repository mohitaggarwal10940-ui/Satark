package com.dev.satark.data.model

import com.google.gson.annotations.SerializedName

data class AnalysisResponse(
    @SerializedName("analysisId")
    val analysisId: String,
    
    @SerializedName("riskScore")
    val riskScore: Int,
    
    @SerializedName("riskLevel")
    val riskLevel: String,
    
    @SerializedName("claims")
    val claims: List<Claim> = emptyList(),
    
    @SerializedName("riskSignals")
    val riskSignals: List<RiskSignal> = emptyList(),
    
    @SerializedName("evidence")
    val evidence: List<Evidence> = emptyList(),
    
    @SerializedName("explanation")
    val explanation: String,
    
    @SerializedName("recommendedActions")
    val recommendedActions: List<String> = emptyList()
)
