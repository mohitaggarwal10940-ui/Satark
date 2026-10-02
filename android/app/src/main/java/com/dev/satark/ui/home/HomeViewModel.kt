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
import com.dev.satark.ocr.TextRecognitionManager
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

    fun toggleLanguage() {
        _selectedLanguage.value = if (_selectedLanguage.value == "hi") "en" else "hi"
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
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
                language = _selectedLanguage.value
            )

            result.onSuccess { response ->

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = response,
                        error = null
                    )
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

        val lang = _selectedLanguage.value

        val mockData = MockAnalysisData.createMockResponse(
            languageCode = lang
        )

        val sampleText = when (lang.lowercase()) {
            "hi" -> "सेबी पंजीकृत सलाहकार।\n30% मासिक रिटर्न की गारंटी।\nआज ही ₹20,000 का भुगतान करें."

            "ta" -> "செபி பதிவு பெற்ற ஆலோசகர்.\nமாதம் 30% உத்தரவாத லாபம்.\nஇன்றே ₹20,000 செலுத்துங்கள்."

            "te" -> "సెబి నమోదిత సలహాదారు.\nనెలకు 30% హామీ రాబడి.\nఈ రోజే ₹20,000 చెల్లించండి."

            "bn" -> "সেবি নিবন্ধিত উপদেষ্টা।\nমাসে ৩০% নিশ্চিত রিটার্ন।\nআজই ₹২০,০০০ প্রদান করুন."

            "mr" -> "सेबी नोंदणीकृत सल्लागार.\nदरमहा ३०% हमी परतावा.\nआजच ₹२०,००० भरा."

            "gu" -> "સેબી રજિસ્ટર્ડ સલાહકાર.\nદર મહિને 30% ગેરંટીડ રિટર્ન.\nઆજે જ ₹20,000 ચૂકવો."

            "kn" -> "ಸೆಬಿ ನೋಂದಾಯಿತ ಸಲಹೆಗಾರ.\nತಿಂಗಳಿಗೆ 30% ಖಾತರಿಯ ಲಾಭ.\nಇಂದೇ ₹20,000 ಪಾವತಿಸಿ."

            else -> "SEBI registered advisor.\nGuaranteed 30% monthly returns.\nPay ₹20,000 today."
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                extractedText = sampleText,
                result = mockData,
                error = null
            )
        }

        onSuccess()
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
            val speechText = buildVoiceSummary(result, lang)
            voiceReplyManager?.speak(speechText, lang)
        }
    }

    fun stopVoiceReply() {
        voiceReplyManager?.stop()
        _isSpeaking.value = false
    }

    private fun buildVoiceSummary(response: AnalysisResponse, languageCode: String): String {
        return when (languageCode.lowercase()) {
            "hi" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "अत्यधिक चिंता"
                    "HIGH" -> "उच्च चिंता"
                    "MEDIUM", "MODERATE" -> "मध्यम चिंता"
                    else -> "कम चिंता"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "पहचाने गए चेतावनी संकेत: " + response.riskSignals.joinToString(", ") { it.title } + "।"
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "सुझाव: " + response.recommendedActions.first()
                } else ""
                "सतर्क सुरक्षा मूल्यांकन। स्तर: $levelText। चिंता स्कोर: 100 में से ${response.riskScore}। ${response.explanation} $signalsText $actionsText"
            }
            "ta" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "மிக அதிக கவலை"
                    "HIGH" -> "அதிக கவலை"
                    "MEDIUM", "MODERATE" -> "மிதமான கவலை"
                    else -> "குறைந்த கவலை"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "கண்டறியப்பட்ட எச்சரிக்கை சமிக்ஞைகள்: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "பாதுகாப்பு ஆலோசனை: " + response.recommendedActions.first()
                } else ""
                "சதர்க் பாதுகாப்பு மதிப்பீடு. நிலை: $levelText. கவலை மதிப்பெண்: 100க்கு ${response.riskScore}. ${response.explanation} $signalsText $actionsText"
            }
            "te" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "చాలా ఎక్కువ ఆందోళన"
                    "HIGH" -> "అధిక ఆందోళన"
                    "MEDIUM", "MODERATE" -> "మధ్యస్థ ఆందోళన"
                    else -> "తక్కువ ఆందోళన"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "గుర్తించిన హెచ్చరిక సంకేతాలు: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "సలహా: " + response.recommendedActions.first()
                } else ""
                "సతర్క్ భద్రతా అంచనా. స్థాయి: $levelText. ఆందోళన స్కోర్: 100కి ${response.riskScore}. ${response.explanation} $signalsText $actionsText"
            }
            "bn" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "অত্যন্ত উদ্বেগজনক"
                    "HIGH" -> "উচ্চ উদ্বেগ"
                    "MEDIUM", "MODERATE" -> "মাঝারি উদ্বেগ"
                    else -> "কম উদ্বেগ"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "চিহ্নিত সতর্কতা সংকেত: " + response.riskSignals.joinToString(", ") { it.title } + "।"
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "পরামর্শ: " + response.recommendedActions.first()
                } else ""
                "সতর্ক সুরক্ষা মূল্যায়ন। স্তর: $levelText। উদ্বেগ স্কোর: ১০০ তে ${response.riskScore}। ${response.explanation} $signalsText $actionsText"
            }
            "mr" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "अति तीव्र चिंता"
                    "HIGH" -> "उच्च चिंता"
                    "MEDIUM", "MODERATE" -> "मध्यम चिंता"
                    else -> "कमी चिंता"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "आढळलेले धोक्याचे संकेत: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "सल्ला: " + response.recommendedActions.first()
                } else ""
                "सतर्क सुरक्षा मूल्यांकन. स्तर: $levelText. चिंता गुण: १०० पैकी ${response.riskScore}. ${response.explanation} $signalsText $actionsText"
            }
            "gu" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "અતિ ગંભીર ચિંતા"
                    "HIGH" -> "ઉચ્ચ ચિંતા"
                    "MEDIUM", "MODERATE" -> "મધ્યમ ચિંતા"
                    else -> "ઓછી ચિંતા"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "મળેલા ચેતવણી સંકેતો: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "સુરક્ષા સલાહ: " + response.recommendedActions.first()
                } else ""
                "સતર્ક સુરક્ષા મૂલ્યાંકન. સ્તર: $levelText. ચિંતા સ્કોર: 100 માંથી ${response.riskScore}. ${response.explanation} $signalsText $actionsText"
            }
            "kn" -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "ಅತ್ಯಂತ ತೀವ್ರ ಕಳವಳ"
                    "HIGH" -> "ಹೆಚ್ಚಿನ ಕಳವಳ"
                    "MEDIUM", "MODERATE" -> "ಮಧ್ಯಮ ಕಳವಳ"
                    else -> "ಕಡಿಮೆ ಕಳವಳ"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "ಪತ್ತೆಯಾದ ಎಚ್ಚರಿಕೆ ಸಂಕೇತಗಳು: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "ಸಲಹೆ: " + response.recommendedActions.first()
                } else ""
                "ಸತರ್ಕ್ ಸುರಕ್ಷತಾ ಮೌಲ್ಯಮಾಪನ. ಮಟ್ಟ: $levelText. ಕಾಳಜಿ ಸ್ಕೋರ್: 100 ಕ್ಕೆ ${response.riskScore}. ${response.explanation} $signalsText $actionsText"
            }
            else -> {
                val levelText = when (response.riskLevel.uppercase()) {
                    "VERY_HIGH", "CRITICAL" -> "Very High Concern"
                    "HIGH" -> "High Concern"
                    "MEDIUM", "MODERATE" -> "Moderate Concern"
                    else -> "Low Concern"
                }
                val signalsText = if (response.riskSignals.isNotEmpty()) {
                    "Warning signals detected: " + response.riskSignals.joinToString(", ") { it.title } + "."
                } else ""
                val actionsText = if (response.recommendedActions.isNotEmpty()) {
                    "Advice: " + response.recommendedActions.first()
                } else ""
                "SATARK Safety Assessment. Concern level: $levelText. Concern score: ${response.riskScore} out of 100. ${response.explanation} $signalsText $actionsText"
            }
        }
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
