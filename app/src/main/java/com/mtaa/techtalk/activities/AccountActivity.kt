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
import io.ktor.client.features.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException

class AccountActivity : ComponentActivity() {
    private lateinit var viewModel: UserReviewsViewModel
    private lateinit var offlineViewModel: OfflineDialogViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        val authKey = prefs.getString("token", "") ?: ""
        val name = prefs.getString("username", "") ?: ""
        viewModel = ViewModelProvider(this).get(UserReviewsViewModel::class.java)
        offlineViewModel = ViewModelProvider(this).get(OfflineDialogViewModel::class.java)
        val queryParams = OrderAttributes("","")

        setLanguage(prefs.getString("language", "English"), this)
        viewModel.loadViewModel(offlineViewModel)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {

                    AccountScreen(viewModel, authKey, name,queryParams,offlineViewModel)
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
    lateinit var offlineViewModel:OfflineDialogViewModel

    private var page = 1

    //Set offline model
    fun loadViewModel(viewModel:OfflineDialogViewModel) {
        offlineViewModel = viewModel
    }

    fun loadUserReviews(authKey: String,obj:OrderAttributes) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val userInfo: UserInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    userInfo = DataGetter.getUserInfo(authKey, page,obj)
                    page++
                }
                offlineViewModel.changeResult(NO_ERROR)
                liveUserReviews.value = liveUserReviews.value?.plus(userInfo.reviews) ?: userInfo.reviews
                trustScore.value = userInfo.trust_score
            } catch (e: Exception) {
                println(e.stackTraceToString())
                when (e) {
                    is ConnectTimeoutException -> {
                        offlineViewModel.changeResult(SERVER_OFFLINE)
                    }
                    is ConnectException -> {
                        offlineViewModel.changeResult(NO_INTERNET)
                    }
                    is ClientRequestException -> {
                        //This is OK, the list it empty
                    }
                    else -> offlineViewModel.changeResult(OTHER_ERROR)
                }
            }
        }
    }

    //Delete all reviews and load them again
    fun reloadUserReviews(authKey: String,obj: OrderAttributes) {
        liveUserReviews.value = mutableListOf()
        page = 1
        loadUserReviews(authKey,obj)
    }
}

@Composable
fun AccountScreen(viewModel: UserReviewsViewModel, authKey: String?, name: String?, obj: OrderAttributes, offlineViewModel: OfflineDialogViewModel) {
    val userReviews by viewModel.liveUserReviews.observeAsState(initial = null)
    val trustScore by viewModel.trustScore.observeAsState(initial = 0)
    val context = LocalContext.current
    val result = offlineViewModel.loadingResult.observeAsState(initial = NO_ERROR)
    val isFirst = remember { mutableStateOf(true) }

    //Show warning that we are offline
    if (result.value != NO_ERROR && result.value != WAITING_FOR_CONFIRMATION) {
        OfflineDialog(
            callback = {
                offlineViewModel.changeResult(WAITING_FOR_CONFIRMATION)
                if (authKey != null) {
                    viewModel.loadUserReviews(authKey, obj)
                }
            },
            result = result.value
        )
    }

    val orderState = remember { mutableStateOf(DrawerValue.Closed) }

    //Open order window
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
                    val selectedOrder =
                        remember { mutableStateOf(context.getString(R.string.newest)) }
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

                        viewModel.reloadUserReviews(authKey ?: "", obj)
                        orderState.value = DrawerValue.Closed
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
        }
    }

    //Account screen
    if (userReviews == null) {
        LoadingScreen(context.getString(R.string.loading_user_info))
    } else {
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

            //User score
            Text(
                text = "${context.getString(R.string.fan_score)} $trustScore",
                modifier = Modifier.padding(top = 15.dp, start = 25.dp),
                fontSize = 24.sp
            )

            //Reviews + ordering
            Row(
                modifier = Modifier.padding(top = 15.dp, start = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (result.value == NO_ERROR)
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

            if (userReviews!!.isEmpty() && isFirst.value) {
                NotFoundScreen(context.getString(R.string.no_reviews_found))
            } else if (userReviews!!.isEmpty() && !isFirst.value) {
                LoadingScreen(context.getString(R.string.loading_reviews))
            } else {
                isFirst.value = false
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 10.dp)
                ) {
                    itemsIndexed(userReviews!!) { index, item ->
                        ReviewBox(reviewInfo = item, canEdit = true)
                        if (index == userReviews!!.lastIndex && result.value == NO_ERROR) {
                            if (authKey != null) {
                                viewModel.loadUserReviews(authKey, obj)
                            }
                        }
                    }
                }
            }
        }
    }
}