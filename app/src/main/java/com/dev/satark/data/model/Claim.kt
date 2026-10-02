package com.dev.satark.data.model

import com.google.gson.annotations.SerializedName

data class Claim(
    @SerializedName("claimText")
    val claimText: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("confidence")
    val confidence: String? = null
)
