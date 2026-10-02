package com.dev.satark.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TextRecognitionManager(
    private val context: Context
) {

    private fun getRecognizer(language: String): TextRecognizer {
        return if (language == "hi") {
            TextRecognition.getClient(
                DevanagariTextRecognizerOptions.Builder().build()
            )
        } else {
            TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )
        }
    }

    suspend fun recognizeTextFromUri(
        uri: Uri,
        language: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->

        val recognizer = getRecognizer(language)

        try {
            val image = InputImage.fromFilePath(context, uri)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    recognizer.close()
                    continuation.resume(Result.success(text))
                }
                .addOnFailureListener { exception ->
                    recognizer.close()
                    continuation.resume(Result.failure(exception))
                }

        } catch (exception: Exception) {
            recognizer.close()
            continuation.resume(Result.failure(exception))
        }
    }

    suspend fun recognizeTextFromBitmap(
        bitmap: Bitmap,
        language: String
    ): Result<String> = suspendCancellableCoroutine { continuation ->

        val recognizer = getRecognizer(language)

        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    recognizer.close()
                    continuation.resume(Result.success(text))
                }
                .addOnFailureListener { exception ->
                    recognizer.close()
                    continuation.resume(Result.failure(exception))
                }

        } catch (exception: Exception) {
            recognizer.close()
            continuation.resume(Result.failure(exception))
        }
    }
}