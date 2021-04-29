package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.CategoriesInfo
import com.mtaa.techtalk.DataGetter.getCategories
import com.mtaa.techtalk.DataGetter.getRecentReviews
import com.mtaa.techtalk.ReviewsInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkGray
import java.net.ConnectException

const val NO_ERROR = 0
const val NO_INTERNET = -1
const val SERVER_OFFLINE = -2
const val OTHER_ERROR = -3
const val WAITING_FOR_CONFIRMATION = -4

class SplashActivity : ComponentActivity() {
    companion object InitialData {
        var categories : CategoriesInfo? = null
        var reviews : ReviewsInfo? = null
    }

    private lateinit var viewModel: OfflineDialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        var isFirstRun = false
        if (prefs.getBoolean("firstrun", true)) {
            isFirstRun = true
            prefs.edit().putBoolean("firstrun", false).apply()
            prefs.edit().putString("color-scheme", "Dark Mode").apply()
            prefs.edit().putString("language", "English").apply()
        }

        setLanguage(prefs.getString("language", "English"), this)

        viewModel = ViewModelProvider(this).get(OfflineDialogViewModel::class.java)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    SplashScreen(viewModel,isFirstRun,this)

                    //Download main menu data and load next activity
                    initApplication(this, isFirstRun)
                }
            }
        }
    }

    //Load initial data
    fun initApplication(context:Context, isFirstRun: Boolean) {
        val scope = MainScope()
        scope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    reviews = getRecentReviews()
                    categories = getCategories()
                }
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
            finally {
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
            }
        }
    }
}

class OfflineDialogViewModel: ViewModel() {
    val loadingResult = MutableLiveData<Int>(0)

    fun changeResult(value:Int) {
        loadingResult.value = value
    }
}

@Composable
fun SplashScreen(
    viewModel: OfflineDialogViewModel,
    isFirstRun: Boolean,
    activity: SplashActivity
) {
    val result by viewModel.loadingResult.observeAsState(initial = NO_ERROR)
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {

        //App logo
        Box(
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_transparent_banner),
                contentDescription = null
            )
        }
        CircularProgressIndicator(color = TechTalkGray)

        //If we have connection problem
        if(result != NO_ERROR) {
            OfflineDialog(
                callback = {
                    viewModel.changeResult(NO_ERROR)
                    activity.initApplication(context, isFirstRun)
                },
                result = result
            )
        }
    }
}

//Dialog which is shown when network error happens
@Composable
fun OfflineDialog(callback: () -> Unit, result: Int) {
    val context = LocalContext.current

    Dialog(onDismissRequest = callback) {
        Card(
            border = BorderStroke(1.dp, Color.Black)
        )
        {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (result) {
                    SERVER_OFFLINE -> {
                        Text(
                            context.getString(R.string.err_server_offline),
                            style = MaterialTheme.typography.h6
                        )
                    }
                    NO_INTERNET -> {
                        Text(
                            context.getString(R.string.err_no_internet),
                            style = MaterialTheme.typography.h6
                        )
                    }
                    OTHER_ERROR -> {
                        Text(
                            context.getString(R.string.err_other),
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = callback) {
                    Text(context.getString(R.string.retry),)
                }
            }
        }
    }
}

//This is basic loading animation
@Composable
fun LoadingScreen(label: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = TechTalkGray)
        Text(text = label)
    }
}

//This is shown when there are no results
@Composable
fun NotFoundScreen(label: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = label)
    }
}