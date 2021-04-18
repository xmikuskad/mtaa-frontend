package com.mtaa.techtalk

import android.content.Context
import android.net.Uri
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import com.mtaa.techtalk.activities.PRICE_MULTIPLIER
import kotlin.math.roundToInt

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
    suspend fun getProducts(categoryID:Int, page:Int, obj: QueryAttributes): ProductsInfo {
        return client.get("$ADDRESS/categories/$categoryID/$page") {
            if (obj.min_price > 0.0001f)
                parameter(
                    "min_price",
                    (obj.min_price * PRICE_MULTIPLIER * 100).roundToInt()
                )  //*100 converts to cents
            if (obj.max_price < 0.9999f)
                parameter(
                    "max_price",
                    (obj.max_price * PRICE_MULTIPLIER * 100).roundToInt()
                )  //*100 converts to cents
            if (obj.min_score > 0.0001f)
                parameter("min_score", (obj.min_score * 100).roundToInt())
            if (obj.brands.isNotEmpty())
                parameter("brands", obj.brands)
            if (obj.order_by.isNotEmpty()) {
                parameter("order_by", obj.order_by)
            }
            if (obj.order_type.isNotEmpty()) {
                parameter("order_type", obj.order_type)
            }
        }
    }
    suspend fun getReviews(productID:Int, page:Int, obj:OrderAttributes): ReviewsInfo {
        return client.get("$ADDRESS/products/$productID/$page") {
            if (obj.order_by.isNotEmpty()) {
                parameter("order_by", obj.order_by)
            }
            if (obj.order_type.isNotEmpty()) {
                parameter("order_type", obj.order_type)
            }
        }
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
    suspend fun getReviewInfo(reviewID:Int) :ReviewInfo {
        return client.get("$ADDRESS/reviews/$reviewID")
    }
    suspend fun createReview(review:ReviewPostInfo, auth:String):ReviewIdInfo {
        return client.post("$ADDRESS/reviews") {
            contentType(ContentType.Application.Json)
            header("auth",auth)
            body = review
        }
    }
    suspend fun updateReview(review: ReviewPutInfo, reviewID: Int, auth:String) {
        return client.put("$ADDRESS/reviews/${reviewID}") {
            contentType(ContentType.Application.Json)
            header("auth",auth)
            body = review
        }
    }
    suspend fun addVoteToReview(shouldLike:Boolean, reviewID: Int, auth:String):ReviewVotesInfo {
        val link = if(shouldLike) "like" else "dislike"
        return client.put("$ADDRESS/reviews/${reviewID}/$link") {
            header("auth",auth)
        }
    }
    suspend fun deleteReview(reviewID: Int, auth:String) {
        return client.delete("$ADDRESS/reviews/${reviewID}") {
            header("auth",auth)
        }
    }
    suspend fun uploadPhoto(reviewID: Int, photoURI: Uri,auth:String,context: Context) {
        return client.post("$ADDRESS/reviews/$reviewID/photo") {
            header("auth",auth)
            body = context.contentResolver.openInputStream(photoURI)?.let { ByteArrayContent(it.readBytes(), ContentType.Image.Any) }!!
        }
    }
    suspend fun deletePhoto(reviewID: Int,photoID: Int, auth:String) {
        return client.delete("$ADDRESS/reviews/${reviewID}/photo/$photoID") {
            header("auth",auth)
        }
    }
    suspend fun editAccount(registerInfo: RegisterInfo,auth:String) {
        return client.put("$ADDRESS/users") {
            contentType(ContentType.Application.Json)
            header("auth",auth)
            body = registerInfo
        }
    }
    suspend fun getCategoryBrands(categoryID: Int) :BrandsInfo {
        return client.get("$ADDRESS/categories/$categoryID/brands")
    }
    suspend fun getUserInfo(authKey: String, page: Int, obj:OrderAttributes): UserInfo {
        return client.get("$ADDRESS/users/$page") {
            header("auth", authKey)
            if (obj.order_by.isNotEmpty()) {
                parameter("order_by", obj.order_by)
            }
            if (obj.order_type.isNotEmpty()) {
                parameter("order_type", obj.order_type)
            }
        }
    }
    suspend fun search(searchInput: String, page: Int): ProductsInfo {
        return client.post("$ADDRESS/products/search/$page") {
            contentType(ContentType.Application.Json)
            body = NameInfo(searchInput)
        }
    }
}
