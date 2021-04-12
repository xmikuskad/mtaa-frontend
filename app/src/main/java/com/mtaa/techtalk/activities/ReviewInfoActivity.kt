package com.mtaa.techtalk.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
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

    @ExperimentalAnimationApi
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
                        ReviewInfoScreen(viewModel, reviewID,prefs)
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
                val review: ReviewInfo
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
    val liveReview= MutableLiveData<ReviewInfo>()
    val liveLikes = MutableLiveData<Int>()
    val liveDislikes = MutableLiveData<Int>()

    fun loadReviewData(review: ReviewInfo) {
        liveReview.value = review
        liveLikes.value = review.likes
        liveDislikes.value = review.dislikes
    }

    fun loadVotes(info:ReviewVotesInfo) {
        liveLikes.value = info.likes
        liveDislikes.value = info.dislikes
    }
}

@Composable
fun ReviewInfoScreen(viewModel:ReviewInfoViewModel, reviewID: Int, prefs:SharedPreferences) {
    val reviewData by viewModel.liveReview.observeAsState(initial = null)
    val likes by viewModel.liveLikes.observeAsState(initial = 0)
    val dislikes by viewModel.liveDislikes.observeAsState(initial = 0)
    val scrollState = rememberScrollState()

    LazyColumn(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            if(reviewData!=null) {
                ReviewDetails(viewModel,ReviewVotesInfo(likes,dislikes),reviewData!!,scrollState,reviewID,prefs)
            }
            else{
                //TODO add loading animation or something
                Text(text="Loading details...")
            }
        }
    }
}

@Composable
fun ReviewDetails(viewModel:ReviewInfoViewModel,votes:ReviewVotesInfo,review: ReviewInfo, scrollState:ScrollState, id:Int, prefs:SharedPreferences){
    //First line with name, likes, dislikes
    Row() {
        Column{
            Button(onClick = { addVote(viewModel,true,id,prefs) }) {
                Text(text = "L")
            }
            Text(text = "${votes.likes}")
        }
        Spacer(Modifier.width(80.dp))
        Text(text = "Review details")
        Spacer(Modifier.width(80.dp))
        Column{
            Button(onClick = { addVote(viewModel,false,id,prefs) }) {
                Text(text = "D")
            }
            Text(text = "${votes.dislikes}")
        }
    }
    Spacer(Modifier.size(20.dp))

    //Photos loading
    if(review.images.size > 0) {
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

fun addVote(viewModel: ReviewInfoViewModel,shouldLike:Boolean,reviewID: Int,prefs:SharedPreferences) {
    val auth = prefs.getString("token","")?:""

    if(auth.isEmpty())
    {
        //TODO show not logged in warning
        return
    }
    MainScope().launch(Dispatchers.Main) {
        try {
            val votes: ReviewVotesInfo
            withContext(Dispatchers.IO) {
                // do blocking networking on IO thread
                votes = DataGetter.addVoteToReview(shouldLike,reviewID,auth)
            }
            //Need to be called here to prevent blocking UI
            viewModel.loadVotes(votes)
        } catch (e: Exception) {
            //Review wasnt found
            println(e.stackTraceToString())
        }
    }
}
