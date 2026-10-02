package com.dev.satark.data.remote

import com.dev.satark.data.model.AnalysisResponse
import com.dev.satark.data.model.AnalyzeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("api/analyze")
    suspend fun analyzeContent(
        @Body request: AnalyzeRequest
    ): Response<AnalysisResponse>
}
