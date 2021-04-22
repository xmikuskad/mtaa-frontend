package com.mtaa.techtalk

import android.content.Context
import android.net.Uri
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import com.mtaa.techtalk.activities.PRICE_MULTIPLIER
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import kotlin.math.roundToInt


//This is supposed to be localhost address
const val ADDRESS = "http://10.0.2.2:8080"
const val WEBSOCKET = "10.0.2.2:8080"

//Websocket commands
const val VOTES_COMMAND = "votes"
const val MENU_COMMAND = "menu"
const val LOAD_COMMAND = "load"

object DataGetter {
    private val client = HttpClient {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
    private var ws: WebSocket? = null

    suspend fun getRecentReviews(): ReviewsInfo {
        return client.get("$ADDRESS/reviews/recent")
    }

    suspend fun getCategories(): CategoriesInfo {
        return client.get("$ADDRESS/categories")
    }

    suspend fun getProducts(categoryID: Int, page: Int, obj: QueryAttributes): ProductsInfo {
        return client.get("$ADDRESS/categories/$categoryID/$page") {

            //Check which filter and order attributes we should use
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

    suspend fun getReviews(productID: Int, page: Int, obj: OrderAttributes): ReviewsInfo {
        return client.get("$ADDRESS/products/$productID/$page") {

            //Check if we should use order by
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

    suspend fun getReviewInfo(reviewID: Int): ReviewInfo {
        return client.get("$ADDRESS/reviews/$reviewID")
    }

    suspend fun createReview(review: ReviewPostInfo, auth: String): ReviewIdInfo {
        return client.post("$ADDRESS/reviews") {
            contentType(ContentType.Application.Json)
            header("auth", auth)
            body = review
        }
    }

    suspend fun updateReview(review: ReviewPutInfo, reviewID: Int, auth: String) {
        return client.put("$ADDRESS/reviews/${reviewID}") {
            contentType(ContentType.Application.Json)
            header("auth", auth)
            body = review
        }
    }

    suspend fun addVoteToReview(shouldLike: Boolean, reviewID: Int, auth: String): ReviewVotesInfo {
        val link = if (shouldLike) "like" else "dislike"
        return client.put("$ADDRESS/reviews/${reviewID}/$link") {
            header("auth", auth)
        }
    }

    suspend fun deleteReview(reviewID: Int, auth: String) {
        return client.delete("$ADDRESS/reviews/${reviewID}") {
            header("auth", auth)
        }
    }

    suspend fun uploadPhoto(reviewID: Int, photoURI: Uri, auth: String, context: Context) {
        return client.post("$ADDRESS/reviews/$reviewID/photo") {
            header("auth", auth)
            body = context.contentResolver.openInputStream(photoURI)?.let {
                ByteArrayContent(it.readBytes(), ContentType.Image.Any)
            }!!
        }
    }

    suspend fun deletePhoto(reviewID: Int, photoID: Int, auth: String) {
        return client.delete("$ADDRESS/reviews/${reviewID}/photo/$photoID") {
            header("auth", auth)
        }
    }

    suspend fun editAccount(registerInfo: RegisterInfo, auth: String) {
        return client.put("$ADDRESS/users") {
            contentType(ContentType.Application.Json)
            header("auth", auth)
            body = registerInfo
        }
    }

    suspend fun getCategoryBrands(categoryID: Int): BrandsInfo {
        return client.get("$ADDRESS/categories/$categoryID/brands")
    }

    suspend fun getUserInfo(authKey: String, page: Int, obj: OrderAttributes): UserInfo {
        return client.get("$ADDRESS/users/$page") {
            header("auth", authKey)

            //Check if we should use order by
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

    fun votesUpdateListener(id:Int, callback: (info:ReviewVotesInfo) -> Unit) {
        //Disconnect previous connection
        if (ws != null && ws!!.isOpen) {
            ws!!.disconnect()
            ws = null
        }
        // Create a WebSocket with a socket connection
        ws = WebSocketFactory().setVerifyHostname(false).createSocket("ws://$WEBSOCKET/votes")

        // Register a listener to receive WebSocket events.
        ws!!.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                super.onTextMessage(websocket, text)

                //Messages format "likes dislikes"
                val votes = text?.split(" ")
                votes?.get(1)?.let {
                    callback(ReviewVotesInfo(votes[0].toInt(), it.toInt()))
                }
            }

            override fun onCloseFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onCloseFrame(websocket, frame)
            }
        })

        ws!!.connect()
        ws!!.sendText("$VOTES_COMMAND $id")
    }

    fun recentReviewsUpdateListener(callback: () -> Unit) {
        //Disconnect previous connection
        if (ws != null && ws!!.isOpen) {
            ws!!.disconnect()
            ws = null
        }
        // Create a WebSocket with a socket connection
        ws = WebSocketFactory().setVerifyHostname(false).createSocket("ws://$WEBSOCKET/reviews")

        // Register a listener to receive WebSocket events.
        ws!!.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                super.onTextMessage(websocket, text)
                if(text == LOAD_COMMAND)
                    callback()
            }

            override fun onCloseFrame(websocket: WebSocket?, frame: WebSocketFrame?) {
                super.onCloseFrame(websocket, frame)
            }
        })

        ws!!.connect()
        ws!!.sendText(MENU_COMMAND)
    }

}

