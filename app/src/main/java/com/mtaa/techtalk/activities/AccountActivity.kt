package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.*
import com.mtaa.techtalk.R
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
        val queryParams = OrderAttributes("","")

        setLanguage(prefs.getString("language", "English"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {

                    AccountScreen(viewModel, authKey, name,queryParams)
                    if (authKey.isNotEmpty()) {
                        viewModel.loadUserReviews(authKey,queryParams)
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

    fun loadUserReviews(authKey: String,obj:OrderAttributes) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val userInfo: UserInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    userInfo = DataGetter.getUserInfo(authKey, page,obj)
                    page++
                }
                liveUserReviews.value = liveUserReviews.value?.plus(userInfo.reviews) ?: userInfo.reviews
                trustScore.value = userInfo.trust_score
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

    fun reloadUserReviews(authKey: String,obj: OrderAttributes) {
        liveUserReviews.value = mutableListOf()
        page = 1
        loadUserReviews(authKey,obj)
    }
}

@Composable
fun AccountScreen(viewModel: UserReviewsViewModel, authKey: String?, name: String?, obj: OrderAttributes) {
    val userReviews by viewModel.liveUserReviews.observeAsState(initial = emptyList())
    val trustScore by viewModel.trustScore.observeAsState(initial = 0)
    val context = LocalContext.current

    val orderState = remember { mutableStateOf(DrawerValue.Closed) }

    if (orderState.value == DrawerValue.Open) {
        Dialog(onDismissRequest = { orderState.value = DrawerValue.Closed }) {

            Card(
                border = BorderStroke(1.dp, Color.Black)
            )
            {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(context.getString(R.string.order_by), style = MaterialTheme.typography.h5)
                    Spacer(modifier = Modifier.height(20.dp))
                    val selectedOrder = remember { mutableStateOf(context.getString(R.string.newest)) }
                    DropdownList(
                        items = listOf(
                            context.getString(R.string.newest),
                            context.getString(R.string.oldest),
                            context.getString(R.string.score_asc),
                            context.getString(R.string.score_desc)
                        ),
                        label = context.getString(R.string.order_by),
                        selected = selectedOrder
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        when (selectedOrder.value) {
                            context.getString(R.string.newest) -> {
                                obj.order_by = ""
                                obj.order_type = ""
                            }
                            context.getString(R.string.oldest) -> {
                                obj.order_by = CREATED_AT
                                obj.order_type = ASCENDING
                            }
                            context.getString(R.string.score_asc) -> {
                                obj.order_by = SCORE
                                obj.order_type = ASCENDING
                            }
                            context.getString(R.string.score_desc) -> {
                                obj.order_by = SCORE
                                obj.order_type = DESCENDING
                            }
                        }

                        viewModel.reloadUserReviews(authKey?:"", obj)
                        orderState.value = DrawerValue.Closed
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
        }
    }


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
            text = "${context.getString(R.string.fan_score)} $trustScore",
            modifier = Modifier.padding(top = 15.dp, start = 25.dp),
            fontSize = 24.sp
        )
        Row(
            modifier = Modifier.padding(top = 15.dp, start = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    orderState.value = DrawerValue.Open
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.Sort),
                    contentDescription = null
                )
            }
            Text(
                text = context.getString(R.string.reviews),
                fontSize = 24.sp
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            itemsIndexed(userReviews) { index, item ->
                ReviewBox(reviewInfo = item,canEdit = true)
                if(index == userReviews.lastIndex) {
                    if (authKey != null) {
                        viewModel.loadUserReviews(authKey,obj)
                    }
                }
            }
        }
    }
}