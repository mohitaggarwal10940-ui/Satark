package com.dev.satark.data.model

import com.google.gson.annotations.SerializedName

data class RiskSignal(
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("severity")
    val severity: String? = null
)
