package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.*
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkGray
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException

const val CREATED_AT = "created_at"

class ReviewsActivity : ComponentActivity() {

    private lateinit var viewModel: ReviewScreenViewModel
    private lateinit var offlineViewModel: OfflineDialogViewModel
    private lateinit var productName: String
    private var productId: Int = 0

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        window.exitTransition = null

        viewModel = ViewModelProvider(this).get(ReviewScreenViewModel::class.java)
        offlineViewModel = ViewModelProvider(this).get(OfflineDialogViewModel::class.java)
        productId = intent.getIntExtra("productId", 0)
        productName = intent.getStringExtra("productName") ?: "Unknown name"
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
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
                    drawerContent = { Drawer(prefs) },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                if ((prefs.getString("token","") ?: "").isNotEmpty()) {
                                    openAddReview(this, productId, productName)
                                } else {
                                    showMessage(this, getString(R.string.not_logged_in), Toast.LENGTH_SHORT)
                                }
                            },
                            text = {Text(text=getString(R.string.add_review))},
                            icon = {
                                Icon(
                                modifier = Modifier.size(40.dp, 40.dp),
                                painter = rememberVectorPainter(Icons.Filled.Add),
                                contentDescription = null
                            )},
                            backgroundColor = TechTalkGray
                        )
                    }
                ) {
                    ReviewsScreen(productId, productName, viewModel,queryParams,offlineViewModel)
                    viewModel.loadReviews(productId,queryParams)
                }
            }
        }
    }
}

//This class is responsible for updating list data
class ReviewScreenViewModel: ViewModel() {

    val liveReviews = MutableLiveData<List<ReviewInfoItem>>()
    lateinit var offlineViewModel:OfflineDialogViewModel

    private var page = 1

    fun loadViewModel(viewModel:OfflineDialogViewModel) {
        offlineViewModel = viewModel
    }

    fun loadReviews(productId: Int,obj: OrderAttributes) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val reviews: ReviewsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    reviews = DataGetter.getReviews(productId,page,obj)
                    page++
                }
                offlineViewModel.changeResult(NO_ERROR)
                liveReviews.value = liveReviews.value?.plus(reviews.reviews) ?: reviews.reviews
            } catch (e: Exception) {
                println(e.stackTraceToString())
                when (e) {
                    is ConnectTimeoutException -> {
                        offlineViewModel.changeResult(SERVER_OFFLINE)
                    }
                    is ConnectException -> {
                        offlineViewModel.changeResult(NO_INTERNET)
                    }
                    else -> offlineViewModel.changeResult(OTHER_ERROR)
                }
            }
        }
    }

    //Delete all reviews and load them again
    fun reloadReviews(productId: Int, obj:OrderAttributes) {
        liveReviews.value = mutableListOf()
        page = 1
        loadReviews(productId,obj)
    }
}

@Composable
fun ReviewsScreen(
    productId: Int,
    productName: String,
    viewModel: ReviewScreenViewModel,
    obj: OrderAttributes,
    offlineViewModel: OfflineDialogViewModel
) {

    val reviews by viewModel.liveReviews.observeAsState(initial = null)
    val context = LocalContext.current
    val orderState = remember { mutableStateOf(DrawerValue.Closed) }
    val result by offlineViewModel.loadingResult.observeAsState(initial = NO_ERROR)
    val isFirst = remember { mutableStateOf(true) }

    //If we have connection problem
    if (result != NO_ERROR && result != WAITING_FOR_CONFIRMATION) {
        OfflineDialog(
            callback = {
                offlineViewModel.changeResult(WAITING_FOR_CONFIRMATION)
                viewModel.loadReviews(productId, obj)
            },
            result = result
        )
        return
    }

    //Order screen
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

                        viewModel.reloadReviews(productId, obj)
                        orderState.value = DrawerValue.Closed
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
        }
    }

    if (reviews == null) {
        LoadingScreen(context.getString(R.string.loading_reviews))
    } else {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                //Order btn
                IconButton(
                    onClick = {
                        if (result == NO_ERROR)
                            orderState.value = DrawerValue.Open
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = rememberVectorPainter(Icons.Filled.Sort),
                        contentDescription = null
                    )
                }

                //Title
                Text(
                    "${context.getString(R.string.reviews_of)} $productName",
                    style = TextStyle(fontSize = 22.sp),
                    textAlign = TextAlign.Center
                )
            }

            if (reviews!!.isEmpty() && isFirst.value) {
                NotFoundScreen(context.getString(R.string.no_reviews_found))
            } else if (reviews!!.isEmpty() && !isFirst.value) {
                LoadingScreen(context.getString(R.string.loading_reviews))
            } else {
                isFirst.value = false
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 10.dp)
                ) {
                    itemsIndexed(reviews!!) { index, item ->
                        ReviewBox(reviewInfo = item, canEdit = false,false)
                        if (index == reviews!!.lastIndex && result == NO_ERROR) {
                            viewModel.loadReviews(productId, obj)
                        }
                    }
                }
            }
        }
    }
}

//Open add screen activity
fun openAddReview(context: Context, productId: Int, productName: String) {
    val intent = Intent(context, AddReviewActivity::class.java)
    intent.putExtra("productID",productId)
    intent.putExtra("productName",productName)
    context.startActivity(intent)
}