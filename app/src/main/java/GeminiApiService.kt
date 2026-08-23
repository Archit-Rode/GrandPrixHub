package com.example.grandprixhub

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- Request Models ---
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiSystemInstruction? = null,
    val tools: List<GeminiTool>? = null
)

data class GeminiTool(
    val google_search: Map<String, String> = emptyMap()
)

data class GeminiSystemInstruction(
    val parts: List<GeminiPart>
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// --- Response Models ---
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiCandidateContent?
)

data class GeminiCandidateContent(
    val parts: List<GeminiPart>?
)

// --- API Service ---
interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}