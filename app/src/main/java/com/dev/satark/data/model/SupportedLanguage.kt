package com.dev.satark.data.model

data class SupportedLanguage(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val speechTag: String
) {
    companion object {
        val ALL = listOf(
            SupportedLanguage("en", "English", "English", "en-IN"),
            SupportedLanguage("hi", "Hindi", "हिंदी", "hi-IN"),
            SupportedLanguage("ta", "Tamil", "தமிழ்", "ta-IN"),
            SupportedLanguage("te", "Telugu", "తెలుగు", "te-IN"),
            SupportedLanguage("bn", "Bengali", "বাংলা", "bn-IN"),
            SupportedLanguage("mr", "Marathi", "मराठी", "mr-IN"),
            SupportedLanguage("gu", "Gujarati", "ગુજરાતી", "gu-IN"),
            SupportedLanguage("kn", "Kannada", "ಕನ್ನಡ", "kn-IN")
        )

        fun findByCode(code: String): SupportedLanguage {
            return ALL.find { it.code.equals(code, ignoreCase = true) } ?: ALL[0]
        }
    }
}
