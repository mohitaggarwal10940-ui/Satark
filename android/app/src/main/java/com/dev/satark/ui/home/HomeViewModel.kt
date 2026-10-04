package com.dev.satark.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dev.satark.R
import com.dev.satark.data.mock.MockAnalysisData
import com.dev.satark.data.model.AnalysisResponse
import com.dev.satark.data.repository.AnalysisRepository
import com.dev.satark.data.translator.AnalysisTranslator
import com.dev.satark.ocr.TextRecognitionManager
import com.dev.satark.util.SatarkTranslator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val isOcrProcessing: Boolean = false,
    val loadingStage: Int = 0,
    val extractedText: String = "",
    val selectedImageUri: Uri? = null,
    val capturedBitmap: Bitmap? = null,
    val result: AnalysisResponse? = null,
    val error: String? = null,
    val inputType: String = "SCREENSHOT",
    val useMockMode: Boolean = false
)

class HomeViewModel(
    private val repository: AnalysisRepository = AnalysisRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(
        if (java.util.Locale.getDefault().language == "hi") "hi" else "en"
    )
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private var loadingJob: Job? = null

    private val satarkTranslator = SatarkTranslator()
    private val analysisTranslator = AnalysisTranslator(satarkTranslator)

    private var canonicalResult: AnalysisResponse? = null
    private var translationJob: Job? = null

    fun toggleLanguage() {
        _selectedLanguage.value = if (_selectedLanguage.value == "hi") "en" else "hi"
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang

        val originalResult = canonicalResult ?: return

        translationJob?.cancel()

        translationJob = viewModelScope.launch {
            try {
                val translatedResult = analysisTranslator.translate(
                    response = originalResult,
                    language = lang
                )

                _uiState.update {
                    it.copy(
                        result = translatedResult,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Translation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun setMockMode(enabled: Boolean) {
        _uiState.update { it.copy(useMockMode = enabled) }
    }

    fun onImageSelected(context: Context, uri: Uri, onSuccess: () -> Unit) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                capturedBitmap = null,
                inputType = "SCREENSHOT",
                isOcrProcessing = true,
                error = null
            )
        }

        viewModelScope.launch {
            val ocrManager = TextRecognitionManager(context.applicationContext)
            val result = ocrManager.recognizeTextFromUri(
                uri,
                _selectedLanguage.value
            )
            result.onSuccess { text ->
                if (text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isOcrProcessing = false,
                            error = context.getString(R.string.error_ocr_no_text)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            extractedText = text,
                            isOcrProcessing = false,
                            error = null
                        )
                    }
                    onSuccess()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isOcrProcessing = false,
                        error = error.localizedMessage ?: context.getString(R.string.error_ocr_no_text)
                    )
                }
            }
        }
    }

    fun onBitmapCaptured(context: Context, bitmap: Bitmap, onSuccess: () -> Unit) {
        _uiState.update {
            it.copy(
                capturedBitmap = bitmap,
                selectedImageUri = null,
                inputType = "SCREENSHOT",
                isOcrProcessing = true,
                error = null
            )
        }

        viewModelScope.launch {
            val ocrManager = TextRecognitionManager(context.applicationContext)
            val result = ocrManager.recognizeTextFromBitmap(
                bitmap,
                _selectedLanguage.value
            )

            result.onSuccess { text ->
                if (text.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isOcrProcessing = false,
                            error = context.getString(R.string.error_ocr_no_text)
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            extractedText = text,
                            isOcrProcessing = false,
                            error = null
                        )
                    }
                    onSuccess()
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isOcrProcessing = false,
                        error = error.localizedMessage ?: context.getString(R.string.error_ocr_no_text)
                    )
                }
            }
        }
    }

    fun onDirectTextSubmitted(text: String, onSuccess: () -> Unit) {
        if (text.isBlank()) {
            _uiState.update { it.copy(error = "Please enter some text to analyze.") }
            return
        }
        _uiState.update {
            it.copy(
                extractedText = text.trim(),
                selectedImageUri = null,
                capturedBitmap = null,
                inputType = "TEXT",
                error = null
            )
        }
        onSuccess()
    }

    fun updateExtractedText(newText: String) {
        _uiState.update { it.copy(extractedText = newText) }
    }

    fun analyzeContent(onSuccess: () -> Unit) {

        val currentText = _uiState.value.extractedText

        if (currentText.isBlank()) {
            _uiState.update {
                it.copy(error = "No content available for analysis.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                loadingStage = 0,
                error = null
            )
        }

        loadingJob?.cancel()

        loadingJob = viewModelScope.launch {

            // UI loading stages
            launch {
                delay(600)

                if (_uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(loadingStage = 1)
                    }
                }

                delay(700)

                if (_uiState.value.isLoading) {
                    _uiState.update {
                        it.copy(loadingStage = 2)
                    }
                }
            }

            val result = repository.analyzeContent(
                inputType = _uiState.value.inputType,
                text = currentText,
                language = "en"
            )

            result.onSuccess { response ->

                // Keep the original English response.
                canonicalResult = response

                try {
                    val translatedResult = analysisTranslator.translate(
                        response = response,
                        language = _selectedLanguage.value
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = translatedResult,
                            error = null
                        )
                    }

                } catch (e: Exception) {
                    // Analysis succeeded even if translation failed.
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = response,
                            error = "Analysis completed, but translation failed."
                        )
                    }
                }

                onSuccess()
            }.onFailure { throwable ->

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message
                            ?: "Analysis failed. Please try again."
                    )
                }
            }
        }
    }
    fun loadMockResult(onSuccess: () -> Unit) {

        val mockData = MockAnalysisData.createMockResponse(
            languageCode = "en"
        )

        val sampleText =
            "SEBI registered advisor.\n" +
                    "Guaranteed 30% monthly returns.\n" +
                    "Pay ₹20,000 today."

        canonicalResult = mockData

        viewModelScope.launch {
            try {
                val translatedResult = analysisTranslator.translate(
                    response = mockData,
                    language = _selectedLanguage.value
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        extractedText = sampleText,
                        result = translatedResult,
                        error = null
                    )
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        extractedText = sampleText,
                        result = mockData,
                        error = "Sample loaded, but translation failed."
                    )
                }
            }

            onSuccess()
        }
    }

    private var voiceReplyManager: com.dev.satark.voice.VoiceReplyManager? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun toggleVoiceReply(context: Context) {
        val result = _uiState.value.result ?: return
        if (voiceReplyManager == null) {
            voiceReplyManager = com.dev.satark.voice.VoiceReplyManager(context.applicationContext)
            viewModelScope.launch {
                voiceReplyManager?.isSpeaking?.collect { speaking ->
                    _isSpeaking.value = speaking
                }
            }
        }

        if (_isSpeaking.value) {
            voiceReplyManager?.stop()
            _isSpeaking.value = false
        } else {
            val lang = _selectedLanguage.value
            val speechText = buildVoiceSummary(result)
            voiceReplyManager?.speak(speechText, lang)
        }
    }

    fun stopVoiceReply() {
        voiceReplyManager?.stop()
        _isSpeaking.value = false
    }

    private fun buildVoiceSummary(
        response: AnalysisResponse
    ): String {

        val levelText = when (response.riskLevel.uppercase()) {
            "VERY_HIGH", "CRITICAL" -> "Very High Concern"
            "HIGH" -> "High Concern"
            "MEDIUM", "MODERATE" -> "Moderate Concern"
            else -> "Low Concern"
        }

        val signalsText = if (response.riskSignals.isNotEmpty()) {
            "Warning signals detected: " +
                    response.riskSignals.joinToString(", ") { it.title } +
                    "."
        } else {
            ""
        }

        val actionsText = if (response.recommendedActions.isNotEmpty()) {
            "Recommended action: " +
                    response.recommendedActions.first()
        } else {
            ""
        }

        return """
        SATARK Safety Assessment.
        Concern level: $levelText.
        Concern score: ${response.riskScore} out of 100.
        ${response.explanation}
        $signalsText
        $actionsText
    """.trimIndent()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetFlow() {
        stopVoiceReply()
        _uiState.update {
            AnalysisUiState(
                useMockMode = it.useMockMode
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceReplyManager?.shutdown()
        voiceReplyManager = null
    }
}
