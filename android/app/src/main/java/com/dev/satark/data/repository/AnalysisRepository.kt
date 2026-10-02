package com.dev.satark.data.repository

import com.dev.satark.data.model.AnalysisRequest
import com.dev.satark.data.model.AnalysisResponse
import com.dev.satark.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AnalysisRepository {

    suspend fun analyzeContent(
        inputType: String,
        text: String,
        language: String = "en"
    ): Result<AnalysisResponse> = withContext(Dispatchers.IO) {

        try {
            val response = RetrofitClient.apiService.analyzeContent(
                AnalysisRequest(
                    inputType = inputType,
                    text = text,
                    language = language
                )
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    IOException("Server returned error ${response.code()}")
                )
            }

        } catch (e: Exception) {
            Result.failure(
                IOException(
                    e.message ?: "Unable to connect to SATARK server.",
                    e
                )
            )
        }
    }
}