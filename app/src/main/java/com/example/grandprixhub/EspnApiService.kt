package com.example.grandprixhub

import retrofit2.http.GET

interface EspnApiService {
    @GET("apis/site/v2/sports/racing/f1/news")
    suspend fun getLatestF1News(): EspnNewsResponse
}

// --- ESPN RESPONSE MODELS ---
data class EspnNewsResponse(
    val articles: List<EspnArticle>
)

data class EspnArticle(
    val headline: String,
    val description: String?,
    val images: List<EspnImage>?,
    val links: EspnLinks?
)

data class EspnImage(
    val url: String
)

data class EspnLinks(
    val web: EspnWebLink?
)

data class EspnWebLink(
    val href: String
)