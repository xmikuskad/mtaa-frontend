package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.mtaa.techtalk.CategoriesInfo
import com.mtaa.techtalk.DataGetter.getCategories
import com.mtaa.techtalk.DataGetter.getRecentReviews
import com.mtaa.techtalk.ReviewsInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkGray

class SplashActivity : ComponentActivity() {
    companion object InitialData {
        lateinit var categories : CategoriesInfo
        lateinit var reviews : ReviewsInfo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        window.exitTransition = null

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        var isFirstRun = false
        if (prefs.getBoolean("firstrun", true)) {
            isFirstRun = true
            prefs.edit().putBoolean("firstrun", false).apply()
            prefs.edit().putString("color-scheme", "Light Mode").apply()
            prefs.edit().putString("language", "English").apply()
        }

        setLanguage(prefs.getString("language", "English"), this)
        // TODO Delete after testing
        //isFirstRun = true

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SplashScreen()

                    //Download main menu data and load next activity
                    initApplication(this, isFirstRun)
                }
            }
        }
    }

    private fun initApplication(context:Context, isFirstRun: Boolean) {
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
                val intent = if (isFirstRun) {
                    Intent(context, FirstLaunchActivity::class.java)
                } else {
                    Intent(context, MainMenuActivity::class.java)
                }
                intent.putExtra("activity","splash")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_transparent_banner),
                contentDescription = null
            )
        }
        CircularProgressIndicator(color = TechTalkGray)
    }
}