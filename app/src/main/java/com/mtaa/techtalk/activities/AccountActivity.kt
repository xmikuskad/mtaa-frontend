package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.*
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class AccountActivity : ComponentActivity() {
    private lateinit var viewModel: UserReviewsViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        val authKey = prefs.getString("token", "") ?: ""
        val name = prefs.getString("username", "") ?: ""
        viewModel = ViewModelProvider(this).get(UserReviewsViewModel::class.java)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {

                    AccountScreen(viewModel, authKey, name)
                    if (authKey.isNotEmpty()) {
                        viewModel.loadUserReviews(authKey)
                    }
                }
            }
        }
    }
}

class UserReviewsViewModel: ViewModel() {
    val liveUserReviews = MutableLiveData<List<ReviewInfoItem>>()
    val trustScore = MutableLiveData<Int>()
    private var page = 1

    fun loadUserReviews(authKey: String) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val userInfo: UserInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    userInfo = DataGetter.getUserInfo(authKey, page)
                    page++
                }
                liveUserReviews.value = liveUserReviews.value?.plus(userInfo.reviews) ?: userInfo.reviews
                trustScore.value = userInfo.trust_score
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }
}

@Composable
fun AccountScreen(viewModel: UserReviewsViewModel, authKey: String?, name: String?) {
    val userReviews by viewModel.liveUserReviews.observeAsState(initial = emptyList())
    val trustScore by viewModel.trustScore.observeAsState(initial = 0)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (name != null) {
            Text(
                text = name,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 15.dp),
                fontSize = 24.sp
            )
        }
        Text(
            text = "Trust Score: $trustScore",
            modifier = Modifier.padding(top = 15.dp, start = 25.dp),
            fontSize = 24.sp
        )
        Text(
            text = "Reviews:",
            modifier = Modifier.padding(top = 15.dp, start = 25.dp),
            fontSize = 24.sp
        )

        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            itemsIndexed(userReviews) { index, item ->
                ReviewBox(reviewInfo = item,canEdit = true)
                if(index == userReviews.lastIndex) {
                    if (authKey != null) {
                        viewModel.loadUserReviews(authKey)
                    }
                }
            }
        }
    }
}