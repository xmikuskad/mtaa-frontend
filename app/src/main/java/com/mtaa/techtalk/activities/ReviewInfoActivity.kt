package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.glide.GlideImage
import com.mtaa.techtalk.ADDRESS
import com.mtaa.techtalk.ReviewInfoItem
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class ReviewInfoActivity: ComponentActivity() {
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val review = intent.getParcelableExtra<ReviewInfoItem>("review")
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ){
                    if (review != null) {
                        ReviewInfoScreen(review)
                    }
                    else {
                        //TODO better error handling
                        Text(text="Review not found!")
                    }
                }
            }
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun ReviewInfoScreen(review: ReviewInfoItem) {
    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxSize()
            .verticalScroll(enabled = true, state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        LazyRow(
            modifier = Modifier.height(300.dp)
        ) {
            items(review.images) {item->
                GlideImage(
                    data = "$ADDRESS/reviews/"+review.review_id+"/photo/"+item.image_id,
                    contentDescription = "My content description",
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
}
