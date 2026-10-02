package com.dev.satark.data.model

import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("inputType")
    val inputType: String,
    
    @SerializedName("text")
    val text: String,

    @SerializedName("language")
    val language: String = "en"
)
