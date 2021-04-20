package com.mtaa.techtalk.activities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.glide.GlideImage
import com.mtaa.techtalk.*
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException

class ReviewInfoActivity: ComponentActivity() {
    lateinit var viewModel: ReviewInfoViewModel
    private lateinit var offlineViewModel: OfflineDialogViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reviewID = intent.getIntExtra("reviewID",-1)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(ReviewInfoViewModel::class.java)
        offlineViewModel = ViewModelProvider(this).get(OfflineDialogViewModel::class.java)

        setLanguage(prefs.getString("language", "English"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ){
                    if (reviewID > 0) {
                        ReviewInfoScreen(viewModel, reviewID,prefs,offlineViewModel,this)
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

    fun loadReviewData(reviewID:Int){
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

    //Called after liking/disliking
    fun loadVotes(info:ReviewVotesInfo) {
        liveLikes.value = info.likes
        liveDislikes.value = info.dislikes
    }
}

@Composable
fun ReviewInfoScreen(
    viewModel: ReviewInfoViewModel,
    reviewID: Int,
    prefs: SharedPreferences,
    offlineViewModel: OfflineDialogViewModel,
    activity: ReviewInfoActivity
) {

    val reviewData by viewModel.liveReview.observeAsState(initial = null)
    val likes by viewModel.liveLikes.observeAsState(initial = 0)
    val dislikes by viewModel.liveDislikes.observeAsState(initial = 0)
    val scrollState = rememberScrollState()
    val result by offlineViewModel.loadingResult.observeAsState(initial = NO_ERROR)

    //If we have a connection problem
    if (result != NO_ERROR) {
        OfflineDialog(
            callback = {
                offlineViewModel.changeResult(NO_ERROR)
                activity.loadReviewData(reviewID)
            },
            result = result
        )
        return
    }

    if (reviewData == null) {
        LoadingScreen(LocalContext.current.getString(R.string.loading_details))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            item {
                ReviewDetails(
                    viewModel,
                    ReviewVotesInfo(likes, dislikes),
                    reviewData!!,
                    scrollState,
                    reviewID,
                    prefs
                )
            }
        }
    }
}

//This is design for one review detail
@Composable
fun ReviewDetails(
    viewModel: ReviewInfoViewModel,
    votes: ReviewVotesInfo,
    review: ReviewInfo,
    scrollState: ScrollState,
    id: Int,
    prefs: SharedPreferences)
{

    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row {

            //Likes
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        addVote(context, viewModel, true, id, prefs)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(25.dp),
                        painter = rememberVectorPainter(Icons.Filled.ThumbUp),
                        contentDescription = null
                    )
                }
                Text(
                    text = "${votes.likes}",
                    textAlign = TextAlign.Center,
                )
            }

            //Title
            Spacer(Modifier.width(40.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.review_details),
                    textAlign = TextAlign.Center,
                    style = typography.h5,
                    modifier = Modifier.padding(top=10.dp,bottom = 5.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(
                        text = "${review.score.div(10.0)} / 10",
                        textAlign = TextAlign.Center,
                        style = typography.h6,
                    )
                    Spacer(Modifier.size(5.dp))
                    Icon(
                        modifier = Modifier.size(15.dp),
                        painter = rememberVectorPainter(Icons.Filled.Star),
                        contentDescription = null
                    )
                }
            }

            //Dislikes
            Spacer(Modifier.width(40.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        addVote(context, viewModel, false, id, prefs)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(25.dp),
                        painter = rememberVectorPainter(Icons.Filled.ThumbDown),
                        contentDescription = null
                    )
                }
                Text(
                    text = "${votes.dislikes}",
                    textAlign = TextAlign.Center,
                )
            }
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
    Row(
        modifier = Modifier.padding(
            start = 20.dp,
            top = 10.dp,
            end = 10.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = rememberVectorPainter(Icons.Filled.AddCircle),
            contentDescription = null
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = context.getString(R.string.positive_attributes),
            textAlign = TextAlign.Center,
            style = typography.h5
        )
    }

    Column(modifier = Modifier.fillMaxWidth())
    {
        for (item in review.attributes) {
            if (item.is_positive) {
                Text(
                    text = "- " + item.text,
                    modifier = Modifier.padding(
                        start = 30.dp,
                        top = 5.dp,
                        end = 15.dp
                    )
                )
            }
        }
    }

    Spacer(Modifier.size(20.dp))
    //Negative attributes
    Row(
        modifier = Modifier.padding(
            start = 20.dp,
            top = 10.dp,
            end = 10.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
            contentDescription = null
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = context.getString(R.string.negative_attributes),
            textAlign = TextAlign.Center,
            style = typography.h5
        )
    }

    Column(modifier = Modifier.fillMaxWidth())
    {
        for (item in review.attributes) {
            if (!item.is_positive) {
                Text(
                    text = "- " + item.text,
                    modifier = Modifier.padding(
                        start = 30.dp,
                        top = 5.dp,
                        end = 15.dp
                    )
                )
            }
        }
    }

    Spacer(Modifier.size(20.dp))
    //Text of review
    Column(
        modifier = Modifier.padding(
            start=20.dp,
            top=10.dp,
            end = 10.dp,
            bottom = 20.dp
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(Icons.Filled.Article),
                contentDescription = null
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text = context.getString(R.string.review_text),
                textAlign = TextAlign.Center,
                style = typography.h5
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(text = review.text)
    }
}

//Add vote to review
fun addVote(
    context: Context,
    viewModel: ReviewInfoViewModel,
    shouldLike: Boolean,
    reviewID: Int,
    prefs: SharedPreferences
) {
    val auth = prefs.getString("token","")?:""

    if(auth.isEmpty())
    {
        showMessage(context, context.getString(R.string.not_logged_in), Toast.LENGTH_SHORT)
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
            //Review wasn't found
            println(e.stackTraceToString())
        }
    }
}
