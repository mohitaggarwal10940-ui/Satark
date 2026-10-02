package com.dev.satark.data.model

import com.google.gson.annotations.SerializedName

data class Evidence(
    @SerializedName("claim")
    val claim: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("details")
    val details: String,
    
    @SerializedName("sourceUrl")
    val sourceUrl: String? = null
)
