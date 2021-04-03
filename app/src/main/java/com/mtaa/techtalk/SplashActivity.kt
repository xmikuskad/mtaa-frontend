package com.mtaa.techtalk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.mtaa.techtalk.DataGetter.getCategories
import com.mtaa.techtalk.DataGetter.getRecentReviews
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.*

class SplashActivity : ComponentActivity() {
    companion object initialData {
        lateinit var categories :CategoriesInfo
        lateinit var reviews : ReviewsInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TechTalkTheme(true) {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SplashScreen()

                    //Download main menu data and load next activity
                    initApplication(this)
                }
            }
        }
    }

    private fun initApplication(context:Context) {
        val scope = MainScope()
        scope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    reviews = getRecentReviews()
                    categories = getCategories()
                    //TODO log in user
                }

                //Load new screen
                val intent = Intent(context, MainMenuActivity::class.java)
                intent.putExtra("activity","splash")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent);
            } catch (e: Exception) {
                println(e.stackTraceToString())
                when (e) {
                    is ConnectTimeoutException -> println("server or user offline") //User or server is offline TODO handle - show warning
                }
            }
        }
    }
}


@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_transparent),
            contentDescription = null
        )
    }
}