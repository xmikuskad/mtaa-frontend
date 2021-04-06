package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
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

class ReviewsActivity : ComponentActivity() {

    private lateinit var viewModel: ReviewScreenViewModel
    private lateinit var productName: String
    private var productId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        window.exitTransition = null

        viewModel = ViewModelProvider(this).get(ReviewScreenViewModel::class.java)
        productId = intent.getIntExtra("productId", 0)
        productName = intent.getStringExtra("productName") ?: "Unknown name"
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) },
                    floatingActionButton = {
                        // TODO Change button color
                        FloatingActionButton(
                            onClick = { println("Add review") }
                        )
                        {
                            Icon(
                                modifier = Modifier.size(40.dp, 40.dp),
                                painter = rememberVectorPainter(Icons.Filled.Add),
                                tint = Color.White,
                                contentDescription = null
                            )
                        }
                    }
                ) {
                    ReviewsScreen(productId, productName, viewModel)
                    viewModel.loadReviews(productId)
                }
            }
        }
    }
}

//This class is responsible for updating list data
//NOTE: we cant actually download data in this class because downloading is blocking and UI will lag !!
class ReviewScreenViewModel: ViewModel() {

    val liveReviews = MutableLiveData<List<ReviewInfoItem>>()
    private var page = 1

    fun loadReviews(productId: Int) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val reviews: ReviewsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    reviews = DataGetter.getReviews(productId,page)
                    page++
                }
                liveReviews.value = liveReviews.value?.plus(reviews.reviews) ?: reviews.reviews
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

}

@Composable
fun ReviewsScreen(productId:Int,productName:String,viewModel: ReviewScreenViewModel) {
    //val context = LocalContext.current
    val reviews by viewModel.liveReviews.observeAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            productName,
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            itemsIndexed(reviews) { index,item ->
                ReviewBox(reviewInfo = item)
                if(index == reviews.lastIndex) {
                    viewModel.loadReviews(productId)
                }
            }
        }
    }
}