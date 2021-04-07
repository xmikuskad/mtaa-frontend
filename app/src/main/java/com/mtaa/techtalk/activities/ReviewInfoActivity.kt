package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.glide.GlideImage
import com.mtaa.techtalk.*
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class ReviewInfoActivity: ComponentActivity() {
    lateinit var viewModel: ReviewInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reviewID = intent.getIntExtra("reviewID",-1)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(ReviewInfoViewModel::class.java)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ){
                    if (reviewID > 0) {
                        ReviewInfoScreen(viewModel.liveReview, reviewID)
                    }
                    else {
                        //TODO better error handling
                        Text(text="Review not found!")
                    }
                }
            }
        }

        loadReviewData(reviewID)
    }

    private fun loadReviewData(reviewID:Int){
        MainScope().launch(Dispatchers.Main) {
            try {
                val review: ReviewInfoItem
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    review = DataGetter.getReviewInfo(reviewID)
                }
                //Need to be called here to prevent blocking UI
                viewModel.loadReviewData(review)
            } catch (e: Exception) {
                //Review wasnt found
                println(e.stackTraceToString())
            }
        }
    }
}

class ReviewInfoViewModel: ViewModel() {
    val liveReview= MutableLiveData<ReviewInfoItem>()

    fun loadReviewData(review:ReviewInfoItem) {
        liveReview.value = review
    }
}

@Composable
fun ReviewInfoScreen(liveReview: LiveData<ReviewInfoItem>, reviewID: Int) {
    val reviewData by liveReview.observeAsState(initial = null)
    val scrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if(reviewData!=null) {
                ReviewDetails(review = reviewData!!, scrollState = scrollState,id = reviewID)
            }
            else{
                //TODO add loading animation or something
                Text(text="Loading details...")
            }
        }
    }
}

@Composable
fun ReviewDetails(review: ReviewInfoItem, scrollState:ScrollState, id:Int){
    println(review)
    //First line with name, likes, dislikes
    Row() {
        Text(text = "L: " + review.likes)
        Spacer(Modifier.width(80.dp))
        Text(text = "Review details")
        Spacer(Modifier.width(80.dp))
        Text(text = "D: " + review.dislikes)
    }
    Spacer(Modifier.size(20.dp))

    //Photos loading
    Row(
        modifier = Modifier
            .height(300.dp)
            .horizontalScroll(enabled = true, state = scrollState)
    ) {
        for(item in review.images) {
            GlideImage(
                data = "$ADDRESS/reviews/" + id + "/photo/" + item.image_id,
                contentDescription = "My content description", fadeIn = true
            )
            Spacer(modifier = Modifier.width(20.dp))
        }
    }

    //Positive attributes
    Spacer(Modifier.size(20.dp))
    Text(text = "Positive:")
    Spacer(Modifier.size(10.dp))
    for (item in review.attributes) {
        if (item.is_positive) {
            Text(text = item.text)
            Spacer(Modifier.size(10.dp))
        }
    }

    //Negative attributs
    Spacer(Modifier.size(10.dp))
    Text(text = "Negative:")
    Spacer(Modifier.size(10.dp))
    for (item in review.attributes) {
        if (!item.is_positive) {
            Text(text = item.text)
            Spacer(Modifier.size(10.dp))
        }
    }

    //Text of review
    Spacer(Modifier.size(10.dp))
    Text(text = "Review text")
    Spacer(Modifier.size(10.dp))
    Text(text = review.text)
}
