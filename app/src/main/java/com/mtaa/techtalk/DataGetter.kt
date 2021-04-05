package com.mtaa.techtalk

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

const val ADDRESS = "http://10.0.2.2:8080"

object DataGetter {
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }

    suspend fun getRecentReviews(): ReviewsInfo {
        return client.get("$ADDRESS/reviews/recent")
    }
    suspend fun getCategories(): CategoriesInfo {
        return client.get("$ADDRESS/categories")
    }
    suspend fun getProducts(categoryID:Int, page:Int): ProductsInfo {
        return client.get("$ADDRESS/categories/$categoryID/$page")
    }
    suspend fun getReviews(productID:Int, page:Int): ReviewsInfo {
        return client.get("$ADDRESS/products/$productID/$page")
    }
    suspend fun login(email: String, password: String): HttpResponseData {
        return client.post("$ADDRESS/login") {
            header("Content-Type", "application/json")
            body = LoginInfo(password, email)
        }
    }
}