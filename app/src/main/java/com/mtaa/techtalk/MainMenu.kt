package com.mtaa.techtalk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

const val MAX_CATEGORIES_COUNT = 6
const val MAX_REVIEW_TEXT = 80

class MainMenu : ComponentActivity() {
    lateinit var viewModel: MainMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MainMenuViewModel::class.java)

        setContent {
            TechTalkTheme(true) {
                Scaffold(topBar = {
                    TopAppBar(title = { Text("Main menu") })
                }) {
                    MenuScreen(liveCategories = viewModel.liveCategories,liveRecentReviews = viewModel.liveRecentReviews)
                    initMainMenu()

                }
            }
        }
    }


    private fun initMainMenu() {
        viewModel.loadCategoriesMenu() //They are the same, no need to reload

        //If we came from splash screen load preloaded data
        if(intent.getStringExtra("activity").equals("splash")) {
            viewModel.loadRecentReviews(MainActivity.reviews.reviews)
        }
        else { //Update data and download again
            MainScope().launch(Dispatchers.Main) {
                try {
                    val recentReviews:ReviewsInfo
                    withContext(Dispatchers.IO) {
                        // do blocking networking on IO thread
                        recentReviews = DataGetter.getRecentReviews()
                    }
                    //Need to be called here to prevent blocking UI
                    viewModel.loadRecentReviews(recentReviews.reviews)
                }
                catch (e:Exception) {
                    println(e.stackTraceToString())
                }
            }
        }
    }
}

//This class is responsible for updating list data
//NOTE: we cant actually download data in this class because downloading is blocking and UI will lag !!
class MainMenuViewModel: ViewModel() {

    val liveCategories = MutableLiveData<List<CategoryInfo>>()
    val liveRecentReviews = MutableLiveData<List<ReviewInfoItem>>()

    fun loadCategoriesMenu() {
        liveCategories.value = MainActivity.categories.categories
    }

    fun loadRecentReviews(reviews:List<ReviewInfoItem>){
        liveRecentReviews.value = reviews
    }
}


@Composable
fun MenuScreen(liveCategories: LiveData<List<CategoryInfo>>, liveRecentReviews:LiveData<List<ReviewInfoItem>>) {
    val categories by liveCategories.observeAsState(initial = emptyList())
    val reviews by liveRecentReviews.observeAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Most popular categories",
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        Row(modifier = Modifier.padding(bottom = 10.dp)) {
            LazyColumn(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.Start) {
                itemsIndexed(categories) { index, item ->
                    if (index % 2 == 0 && index < MAX_CATEGORIES_COUNT) {
                        Card(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clickable(onClick = { printHello() }),
                            border = BorderStroke(
                                1.dp,
                                Color.Red
                            )
                        ) { Text(item.name, modifier = Modifier.padding(5.dp)) }
                    }
                }

            }
            Spacer(Modifier.size(10.dp))
            LazyColumn(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.End) {
                itemsIndexed(categories) { index, item ->
                    if (index % 2 == 1 && index < MAX_CATEGORIES_COUNT) {
                        Card(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .clickable(onClick = { printHello() }),
                            border = BorderStroke(
                                1.dp,
                                Color.Red
                            )
                        ) { Text(item.name, modifier = Modifier.padding(5.dp)) }
                    }
                }

            }
        }
        Text(
            "Most recent reviews",
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        LazyColumn(modifier = Modifier.padding(top = 15.dp)) {
            items(reviews) { item ->
                reviewBox(item)
            }
        }
    }
}

@Composable
fun reviewBox(reviewInfo: ReviewInfoItem) {
    //Calculate before showing
    var positives = 0
    var negatives = 0
    for(attr in reviewInfo.attributes){
        if(attr.is_positive)
            positives++
        else
            negatives++
    }

    Card(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .clickable(onClick = { printHello() }),
        border = BorderStroke(
            2.dp,
            Color.Red
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = reviewInfo.text.take(MAX_REVIEW_TEXT)+"..." ,
                modifier = Modifier.padding(5.dp)
            )
            Spacer(Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$positives positives", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(10.dp))
                Text(text = "$negatives negatives", modifier = Modifier.padding(5.dp))
            }
            Spacer(Modifier.size(10.dp))
            Row() {
                Text(text = "${reviewInfo.likes} likes", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(10.dp))
                Text(text = "${reviewInfo.dislikes} dislikes", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(50.dp))
                Text(text = "Score ${reviewInfo.score.div(10.0)}/10", modifier = Modifier.padding(5.dp))
            }
        }
    }
}

fun printHello() {
    println("HELLO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
}