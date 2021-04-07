package com.mtaa.techtalk

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
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
    suspend fun login(email: String, password: String): AuthInfo {
        return client.post("$ADDRESS/login") {
            contentType(ContentType.Application.Json)
            body = LoginInfo(password, email)
        }
    }
    suspend fun createAccount(name: String, email: String, password: String): HttpStatusCode {
        return client.post("$ADDRESS/users") {
            contentType(ContentType.Application.Json)
            body = RegisterInfo(name, password, email)
        }
    }
    suspend fun getReviewInfo(reviewID:Int) :ReviewInfoItem {
        return client.get("$ADDRESS/reviews/$reviewID")
    }

}