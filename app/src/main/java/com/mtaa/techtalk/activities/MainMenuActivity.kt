package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.CategoryInfo
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ReviewInfoItem
import com.mtaa.techtalk.ReviewsInfo
import com.mtaa.techtalk.ui.theme.TechTalkBlue
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.*
import java.lang.Exception

const val MAX_CATEGORIES_COUNT = 6
const val MAX_REVIEW_TEXT = 80

class MainMenuActivity : ComponentActivity() {
    private lateinit var viewModel: MainMenuViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        window.exitTransition = null

        viewModel = ViewModelProvider(this).get(MainMenuViewModel::class.java)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer() }
                ) {
                    MenuScreen(
                        liveCategories = viewModel.liveCategories,
                        liveRecentReviews = viewModel.liveRecentReviews
                    )
                    initMainMenu()
                }
            }
        }
    }


    private fun initMainMenu() {
        viewModel.loadCategoriesMenu() //They are the same, no need to reload

        val prevScreen = intent.getStringExtra("activity")
        //If we came from splash screen load preloaded data
        if (prevScreen.equals("splash") || prevScreen.equals("first-launch")) {
            viewModel.loadRecentReviews(SplashActivity.reviews.reviews)
        } else { //Update data and download again
            MainScope().launch(Dispatchers.Main) {
                try {
                    val recentReviews: ReviewsInfo
                    withContext(Dispatchers.IO) {
                        // do blocking networking on IO thread
                        recentReviews = DataGetter.getRecentReviews()
                    }
                    //Need to be called here to prevent blocking UI
                    viewModel.loadRecentReviews(recentReviews.reviews)
                } catch (e: Exception) {
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
        liveCategories.value = SplashActivity.categories.categories
    }

    fun loadRecentReviews(reviews: List<ReviewInfoItem>) {
        liveRecentReviews.value = reviews
    }
}

@Composable
fun Drawer() {
    // TODO Smaller Width
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .padding(top = 30.dp)
            .fillMaxWidth()
            .padding(start = 30.dp)
    ) {
        Row(
            modifier = Modifier
            .clickable(
                onClick = {
                    println("Open user info")
                }
            )
        ) {
            Icon(
                modifier = Modifier.size(48.dp, 48.dp),
                painter = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "Guest",
                fontSize = 28.sp
            )
        }
    }
    Column(
        modifier = Modifier
            .padding(top = 30.dp)
            .fillMaxWidth()
            .padding(start = 30.dp)
    ) {
        Text(
            text = "Main Menu",
            modifier = Modifier.clickable(
                onClick = { print("Main Menu") }
            ),
            fontSize = 32.sp
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Categories",
            modifier = Modifier.clickable(
                onClick = {
                    // TODO Don't open if already on category screen
                    openCategories(context)
                }
            ),
            fontSize = 32.sp
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "Settings",
            modifier = Modifier.clickable(
                onClick = { print("Settings") }
            ),
            fontSize = 32.sp
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = "About App",
            modifier = Modifier.clickable(
                onClick = { print("About App") }
            ),
            fontSize = 32.sp
        )
    }
}

@Composable
fun TopBar(scaffoldState: ScaffoldState, scope: CoroutineScope) {
    TopAppBar(
        title = {
            Image(
                painter = painterResource(R.drawable.logo_transparent_banner_text),
                contentDescription = null
            )
        },
        navigationIcon = {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Menu),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp, 24.dp)
                    .offset(16.dp)
                    .clickable(onClick = { scope.launch { scaffoldState.drawerState.open() } })
            )
        },
        backgroundColor = TechTalkBlue
    )
}

@Composable
fun MenuScreen(liveCategories: LiveData<List<CategoryInfo>>, liveRecentReviews:LiveData<List<ReviewInfoItem>>) {
    val categories by liveCategories.observeAsState(initial = emptyList())
    val reviews by liveRecentReviews.observeAsState(initial = emptyList())
    val context = LocalContext.current

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
                        CategoryMainMenu(item = item, context = context)
                    }
                }

            }
            Spacer(Modifier.size(10.dp))
            LazyColumn(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.End) {
                itemsIndexed(categories) { index, item ->
                    if (index % 2 == 1 && index < MAX_CATEGORIES_COUNT) {
                        CategoryMainMenu(item = item, context = context)
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
                ReviewBox(item)
            }
        }
    }
}

@Composable
fun CategoryMainMenu(item:CategoryInfo,context: Context){
    Card(
        modifier = Modifier
            .padding(top = 10.dp)
            .size(150.dp, 30.dp)
            .clickable(onClick = { openProductsMenu(item, context) }),
        backgroundColor = Color.DarkGray,
    ) {
        Text(
            item.name,
            modifier = Modifier.padding(5.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReviewBox(reviewInfo: ReviewInfoItem) {
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
            .clickable(onClick = { println("Open review") }),
        backgroundColor = Color.DarkGray
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
            Row {
                Text(text = "${reviewInfo.likes} likes", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(10.dp))
                Text(text = "${reviewInfo.dislikes} dislikes", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(50.dp))
                Text(text = "Score ${reviewInfo.score.div(10.0)}/10", modifier = Modifier.padding(5.dp))
            }
        }
    }
}

//TODO remake into navigation drawer
fun openCategories(context:Context){
    val intent = Intent(context, CategoriesActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    context.startActivity(intent)
}

fun showMessage(context: Context, message:String, length: Int){
    Toast.makeText(context, message, length).show()
}