package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
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
import com.mtaa.techtalk.ui.theme.TechTalkGray
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.*
import java.lang.Exception

const val MAX_CATEGORIES_COUNT = 6
const val MAX_REVIEW_TEXT = 80

class MainMenuActivity : ComponentActivity() {
    private lateinit var viewModel: MainMenuViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(MainMenuViewModel::class.java)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

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
fun Drawer(prefs: SharedPreferences) {
    // TODO Smaller Width
    val context = LocalContext.current

    val authToken = prefs.getString("token", "")
    val name = prefs.getString("username", "")

    Column(
        modifier = Modifier
            .padding(top = 30.dp, start = 30.dp, bottom = 30.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
            .clickable(
                onClick = {
                    if (authToken != "" && name != "") {
                        val intent = Intent(context, AccountActivity::class.java)
                        context.startActivity(intent)
                    } else {
                        showMessage(context, context.getString(R.string.not_logged_in), Toast.LENGTH_SHORT)
                    }
                }
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(48.dp, 48.dp),
                painter = rememberVectorPainter(image = Icons.Filled.AccountCircle),
                contentDescription = null
            )
            Spacer(Modifier.size(10.dp))
            if (name != "") {
                name
            } else {
                context.getString(R.string.guest)
            }?.let {
                Text(
                    text = it,
                    fontSize = 22.sp
                )
            }
        }
    }
    Divider(
        color = Color.Black,
        thickness = 1.dp
    )
    Column(
        modifier = Modifier
            .padding(top = 30.dp, start = 30.dp, bottom = 30.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Home),
                contentDescription = null
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = context.getString(R.string.main_menu),
                modifier = Modifier.clickable(
                    onClick = { openScreen(context, MainMenuActivity()) }
                ),
                fontSize = 28.sp
            )
        }
        Spacer(Modifier.size(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.TableRows),
                contentDescription = null
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = context.getString(R.string.categories),
                modifier = Modifier.clickable(
                    onClick = {
                        openCategories(context)
                    }
                ),
                fontSize = 28.sp
            )
        }
        Spacer(Modifier.size(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Settings),
                contentDescription = null
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = context.getString(R.string.settings),
                modifier = Modifier.clickable(
                    onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        intent.putExtra("activity", "drawer")
                        context.startActivity(intent)
                    }
                ),
                fontSize = 28.sp
            )
        }
        Spacer(Modifier.size(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Info),
                contentDescription = null
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = context.getString(R.string.about),
                modifier = Modifier.clickable(
                    onClick = { openScreen(context, AboutAppActivity()) }
                ),
                fontSize = 28.sp
            )
        }
    }
    Divider(
        color = Color.Black,
        thickness = 1.dp
    )
    Column(
        modifier = Modifier
            .padding(top = 30.dp, start = 30.dp, bottom = 30.dp)
            .fillMaxWidth()
    ) {
        if (name != "" && authToken != "") {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.ManageAccounts),
                    contentDescription = null
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = context.getString(R.string.edit_account),
                    modifier = Modifier.clickable(
                        onClick = { openScreen(context, EditAccountActivity()) }
                    ),
                    fontSize = 28.sp
                )
            }
            Spacer(Modifier.size(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Logout),
                    contentDescription = null
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = context.getString(R.string.log_out),
                    modifier = Modifier.clickable(
                        onClick = {
                            prefs.edit().remove("token").apply()
                            prefs.edit().remove("username").apply()

                            val intent = Intent(context, FirstLaunchActivity::class.java)
                            intent.putExtra("activity", "menu-log-out")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        }
                    ),
                    fontSize = 28.sp
                )
            }
        } else if (name == "" && authToken == "") {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Login),
                    contentDescription = null
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = context.getString(R.string.log_in),
                    modifier = Modifier.clickable(
                        onClick = {
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.putExtra("activity", "first-launch")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    ),
                    fontSize = 28.sp
                )
            }
            Spacer(Modifier.size(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.PersonAdd),
                    contentDescription = null
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = context.getString(R.string.create_account),
                    modifier = Modifier.clickable(
                        onClick = {
                            val intent = Intent(context, CreateAccountActivity::class.java)
                            intent.putExtra("activity", "first-launch")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            context.startActivity(intent)
                        }
                    ),
                    fontSize = 28.sp
                )
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
fun TopBar(scaffoldState: ScaffoldState, scope: CoroutineScope) {
    val searchBarOpen = remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Image(
                painter = painterResource(R.drawable.logo_transparent_banner_text),
                contentDescription = null
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { scope.launch { scaffoldState.drawerState.open() } },
                modifier = Modifier
                    .size(24.dp, 24.dp)
                    .offset(16.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Menu),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        },
        backgroundColor = TechTalkGray,
        actions = {
            AnimatedVisibility (searchBarOpen.value) {
                SearchBar(searchBarOpen)
            }
            if (!searchBarOpen.value) {
                IconButton(
                    onClick = { searchBarOpen.value = true },
                    modifier = Modifier
                        .size(24.dp, 24.dp)
                        .offset((-16).dp)
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Filled.Search),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    )
}

@Composable
fun MenuScreen(liveCategories: LiveData<List<CategoryInfo>>, liveRecentReviews:LiveData<List<ReviewInfoItem>>) {
    val categories by liveCategories.observeAsState(initial = emptyList())
    val reviews by liveRecentReviews.observeAsState(initial = emptyList())
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getString(R.string.popular_categories),
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
            text = context.getString(R.string.recent_reviews),
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        LazyColumn(modifier = Modifier.padding(top = 5.dp)) {
            items(reviews) { item ->
                ReviewBox(item,false)
            }
        }
    }
}

@Composable
fun CategoryMainMenu(item:CategoryInfo,context: Context){
    Card(
        modifier = Modifier
            .padding(top = 10.dp)
            .size(150.dp, 35.dp)
            .clickable(onClick = { openProductsMenu(item, context) }),
        backgroundColor = Color.DarkGray,
    ) {
        Text(
            text = item.name,
            modifier = Modifier.padding(5.dp),
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 18.sp),
            color = Color.White
        )
    }
}

@Composable
fun ReviewBox(reviewInfo: ReviewInfoItem, canEdit:Boolean) {
    val context = LocalContext.current

    //Calculate before showing
    var positives = 0
    var negatives = 0
    for (attr in reviewInfo.attributes) {
        if (attr.is_positive)
            positives++
        else
            negatives++
    }

    Card(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .clickable(onClick = {
                if (canEdit) openReviewEdit(context, reviewInfo.review_id) else {
                    openReviewInfo(context, reviewInfo.review_id)
                }
            }),
        backgroundColor = Color.DarkGray
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = reviewInfo.text.take(MAX_REVIEW_TEXT) + "...",
                modifier = Modifier.padding(15.dp),
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = rememberVectorPainter(Icons.Filled.AddCircle),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    text = "$positives ${context.getString(R.string.positives_num)}",
                    color = Color.White
                )
                Spacer(Modifier.size(20.dp))
                Icon(
                    modifier = Modifier.size(25.dp),
                    painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    text = "$negatives ${context.getString(R.string.negatives_num)}",
                    color = Color.White
                )
            }
            Spacer(Modifier.size(25.dp))
            Row {
                Text(
                    text = "${reviewInfo.likes}",
                    color = Color.White
                )
                Spacer(Modifier.size(5.dp))
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(Icons.Filled.ThumbUp),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.size(20.dp))
                Text(
                    text = "${reviewInfo.dislikes}",
                    color = Color.White
                )
                Spacer(Modifier.size(5.dp))
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(Icons.Filled.ThumbDown),
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.size(50.dp))
                Text(
                    text = "${reviewInfo.score.div(10.0)} / 10",
                    color = Color.White
                )
                Spacer(Modifier.size(5.dp))
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(Icons.Filled.Star),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

//TODO remake into navigation drawer
fun openCategories(context:Context){
    val intent = Intent(context, CategoriesActivity::class.java)
    context.startActivity(intent)
}

fun openReviewInfo(context: Context,reviewID: Int) {
    val intent = Intent(context, ReviewInfoActivity::class.java)
    intent.putExtra("reviewID",reviewID)
    context.startActivity(intent)
}

fun openReviewEdit(context: Context,reviewID: Int) {
    val intent = Intent(context, EditReviewActivity::class.java)
    intent.putExtra("reviewID",reviewID)
    context.startActivity(intent)
}

//General openScreen function TODO refactor to use this!
fun openScreen(context: Context, activityClass:ComponentActivity){
    val intent = Intent(context, activityClass::class.java)
    context.startActivity(intent)
}

fun showMessage(context: Context, message:String, length: Int){
    Toast.makeText(context, message, length).show()
}