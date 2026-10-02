package com.dev.satark.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceReplyManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        }
    }

    fun speak(text: String, languageCode: String = "en") {
        if (!isInitialized || tts == null) return

        val locale = when (languageCode.lowercase()) {
            "hi" -> Locale.forLanguageTag("hi-IN")
            "ta" -> Locale.forLanguageTag("ta-IN")
            "te" -> Locale.forLanguageTag("te-IN")
            "bn" -> Locale.forLanguageTag("bn-IN")
            "mr" -> Locale.forLanguageTag("mr-IN")
            "gu" -> Locale.forLanguageTag("gu-IN")
            "kn" -> Locale.forLanguageTag("kn-IN")
            else -> Locale.ENGLISH
        }

        try {
            val availability = tts?.isLanguageAvailable(locale)
            if (availability == TextToSpeech.LANG_AVAILABLE || availability == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                tts?.language = locale
            } else {
                try {
                    tts?.language = Locale.forLanguageTag(locale.language)
                } catch (_: Exception) {
                    tts?.language = Locale.ENGLISH
                }
            }
            tts?.setSpeechRate(0.92f) // Clear, measured pace suitable for financial warnings
            tts?.setPitch(1.0f)
            tts?.stop()
            val utteranceId = "satark_utterance_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            _isSpeaking.value = true
        } catch (_: Exception) {
            _isSpeaking.value = false
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
        isInitialized = false
        _isSpeaking.value = false
    }
}
