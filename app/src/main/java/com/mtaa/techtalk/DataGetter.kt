package com.mtaa.techtalk

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*

object DataGetter {
    val client = HttpClient() {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    suspend fun getRecentReviews(): ReviewsInfo {
        return client.get("http://10.0.2.2:8080/reviews/recent")
    }
    suspend fun getCategories(): CategoriesInfo {
        return client.get("http://10.0.2.2:8080/categories")
    }
}