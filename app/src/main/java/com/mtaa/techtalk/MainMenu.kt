package com.mtaa.techtalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class MainMenu : ComponentActivity() {

    lateinit var recentReviews : ReviewsInfo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TechTalkTheme(true) {
                Scaffold() {
                    MenuScreen()
                    initMainMenu()

                }
            }
        }
    }

    private fun initMainMenu() {
        println(MainActivity.categories) //They are the same, no need to reload
        //If we came from splash screen load preloaded data
        if(intent.getStringExtra("activity").equals("splash")) {
            println(MainActivity.reviews)
            recentReviews = MainActivity.reviews
        }
        else { //Update data and download again
            MainScope().launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) {
                        // do blocking networking on IO thread
                        recentReviews = DataGetter.getRecentReviews()
                    }
                }
                catch (e:Exception) {
                    println(e.stackTraceToString())
                }
            }
        }
    }
}


@Composable
fun MenuScreen() {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Text("Main menu screen")
    }
}