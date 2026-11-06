package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiAiService"

data class GeminiActionPlan(
    val explanation: String,
    val detectedAction: String, // "COMPRESS_KB", "PASSPORT_PRESET", "REMOVE_BG", "ROTATE", "DOCUMENT_FILTER", "NAME_DATE_STRIP", "PRINT_SHEET", "OCR", "NONE"
    val targetKb: Int? = null,
    val rotationDegrees: Int? = null,
    val backgroundColor: String? = null, // "white", "blue", "red", "transparent"
    val passportCountry: String? = null, // "india", "us", "uk", "schengen", etc.
    val studentName: String? = null,
    val dateText: String? = null
)

class GeminiAiService {

    // Lazy initialization of the Gemini model via Firebase AI Logic
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 1024
            }
        )
    }

    /**
     * Ask Gemini model directly for conversational Q&A, photo advice, or document assistance.
     */
    suspend fun chatWithGemini(prompt: String, attachedBitmap: Bitmap? = null): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(45_000L) {
                val response = if (attachedBitmap != null) {
                    // Downscale bitmap if too large to ensure quick transmission under 20MB
                    val scaledBitmap = scaleBitmapForAi(attachedBitmap)
                    model.generateContent(
                        content {
                            image(scaledBitmap)
                            text(
                                "You are a professional Android Document and Photo Toolkit assistant. " +
                                "The user has provided an image and asked:\n$prompt\n\n" +
                                "Respond conversationally, helpfully, and concisely in the same language (Hindi or English) " +
                                "the user used. Give clear advice on dimensions, passport specs, file size optimization, or image quality."
                            )
                        }
                    )
                } else {
                    model.generateContent(
                        "You are a friendly, expert Android Document and Photo Toolkit AI assistant. " +
                        "Answer the user query in the same language (Hindi, Hinglish or English) as the query:\n$prompt\n" +
                        "Provide practical guidance regarding PDF compression, passport photo standards (India 35x45mm, US 2x2 in, PAN/Aadhaar), " +
                        "KB reduction, document scanning, and background removal."
                    )
                }
                response.text ?: "I am ready to help with your photos and documents! What would you like to do?"
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            "Gemini request timed out. Please check your internet connection and try again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed: ${e.message}", e)
            // Fallback gracefully if offline
            "AI Assistant: ${e.localizedMessage ?: "Unable to connect to Gemini at this moment. Please check your internet connection."}"
        }
    }

    /**
     * Analyze image and user instruction with Gemini multimodal reasoning,
     * to extract natural parameters and provide a personalized explanation.
     */
    suspend fun analyzeImageInstruction(
        userPrompt: String,
        bitmap: Bitmap
    ): String = withContext(Dispatchers.IO) {
        try {
            withTimeout(30_000L) {
                val scaled = scaleBitmapForAi(bitmap)
                val response = model.generateContent(
                    content {
                        image(scaled)
                        text(
                            "Analyze this user's image and request: \"$userPrompt\".\n" +
                            "Explain what changes you recommend or are being made (e.g. compression, cropping to passport ratio, removing background, rotating, or enhancing document contrast).\n" +
                            "Write 2-3 concise, intelligent sentences directly answering the user in the language they used (Hindi or English)."
                        )
                    }
                )
                response.text.orEmpty()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch dynamic Gemini explanation: ${e.message}")
            ""
        }
    }

    private fun scaleBitmapForAi(src: Bitmap): Bitmap {
        val maxDim = 1024
        if (src.width <= maxDim && src.height <= maxDim) return src
        val ratio = src.width.toFloat() / src.height.toFloat()
        val newW: Int
        val newH: Int
        if (src.width > src.height) {
            newW = maxDim
            newH = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            newH = maxDim
            newW = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }
}
