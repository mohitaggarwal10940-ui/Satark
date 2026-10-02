package com.dev.satark.ui.analyze

import com.dev.satark.data.model.AnalysisResponse

sealed interface AnalysisState {

    data object Idle : AnalysisState

    data object Loading : AnalysisState

    data class Success(
        val response: AnalysisResponse
    ) : AnalysisState

    data class Error(
        val message: String
    ) : AnalysisState
}