package com.mtaa.techtalk

import android.content.Context
import android.net.Uri
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.nio.*
import java.io.File

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
    suspend fun createReview(review:ReviewPostInfo, auth:String):ReviewIdInfo {
        return client.post("$ADDRESS/reviews") {
            contentType(ContentType.Application.Json)
            header("auth",auth)
            body = review
        }
    }
    suspend fun uploadPhoto(reviewID: Int, photoURI: Uri,auth:String,context: Context) {
        return client.post("$ADDRESS/reviews/$reviewID/photo") {
            header("auth",auth)
            body = context.contentResolver.openInputStream(photoURI)?.let { ByteArrayContent(it.readBytes(), ContentType.Image.Any) }!!
        }
    }
    suspend fun editAccount(registerInfo: RegisterInfo,auth:String) {
        return client.put("$ADDRESS/users") {
            contentType(ContentType.Application.Json)
            header("auth",auth)
            body = registerInfo
        }
    }
    suspend fun getUserInfo(authKey: String, page: Int): UserInfo {
        return client.get("$ADDRESS/users/$page") {
            header("auth", authKey)
        }
    }
}

class StreamContent(private val image:File): OutgoingContent.WriteChannelContent() {
    override suspend fun writeTo(channel: ByteWriteChannel) {
        val readChannel = image.inputStream().channel
        var copiedBytes: Long
        do {
            copiedBytes = readChannel.copyTo(channel, 1024)
        } while (copiedBytes > 0)
    }
    override val contentType = ContentType.Image.Any
    override val contentLength: Long = image.length()

}

