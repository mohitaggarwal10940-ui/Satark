package com.dev.satark.ui.analyze

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.satark.data.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalyzeViewModel(
    private val repository: AnalysisRepository = AnalysisRepository()
) : ViewModel() {

    private val _analysisState =
        MutableStateFlow<AnalysisState>(AnalysisState.Idle)

    val analysisState: StateFlow<AnalysisState> =
        _analysisState.asStateFlow()

    fun analyze(
        inputType: String,
        text: String,
        language: String = "en"
    ) {

        if (text.isBlank()) {
            _analysisState.value =
                AnalysisState.Error("Please enter some content to analyze.")
            return
        }

        viewModelScope.launch {

            _analysisState.value = AnalysisState.Loading

            val result = repository.analyzeContent(
                inputType = inputType,
                text = text,
                language = language
            )

            _analysisState.value = result.fold(

                onSuccess = { response ->
                    AnalysisState.Success(response)
                },

                onFailure = { error ->
                    AnalysisState.Error(
                        error.message ?: "Something went wrong."
                    )
                }
            )
        }
    }

    fun reset() {
        _analysisState.value = AnalysisState.Idle
    }
}