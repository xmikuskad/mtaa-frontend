package com.mtaa.techtalk.activities

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.roundToInt

class EditReviewActivity : ComponentActivity() {

    private lateinit var viewModel: EditReviewViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(EditReviewViewModel::class.java)
        var reviewID = intent.getIntExtra("reviewID",-1)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {
                    EditReviewScreen(viewModel,this,prefs,reviewID)
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

class EditReviewViewModel: ViewModel() {
    val livePositive= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveNegative= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveImage= MutableLiveData<List<Uri>>()

    var positives = mutableListOf<ReviewAttributePostPutInfo>()
    var negatives = mutableListOf<ReviewAttributePostPutInfo>()

    val liveReview= MutableLiveData<ReviewInfo>()

    fun loadReviewData(review:ReviewInfo) {
        liveReview.value = review

        for (item in review.attributes) {
            if (item.is_positive) {
                addPositive(item.text)

            } else {
                addNegative(item.text)
            }
        }
    }

    fun addPositive(text:String){
        positives.add(ReviewAttributePostPutInfo(text,true))
        livePositive.value = positives
    }

    fun editPositive(text:String, position:Int) {
        positives[position] = ReviewAttributePostPutInfo(text,true)
        livePositive.value = positives
    }

    fun deletePositive(text: String){
        positives.remove(ReviewAttributePostPutInfo(text,true))
        livePositive.value = positives
    }

    fun addNegative(text:String){
        negatives.add(ReviewAttributePostPutInfo(text,false))
        liveNegative.value = negatives
    }

    fun editNegative(text:String, position:Int) {
        negatives[position] = ReviewAttributePostPutInfo(text,false)
        liveNegative.value = negatives
    }

    fun deleteNegative(text:String){
        negatives.remove(ReviewAttributePostPutInfo(text,false))
        liveNegative.value = negatives
    }

    fun addPhoto(uri: Uri?){
        liveImage.value = (liveImage.value?.plus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }

    fun removePhoto(){

    }
}

// TODO find out why Spacers cause bugs
// How to replicate:
// 1. When deleting all positive attributes and then adding one
// or 2. Adding negative reviews
// There need to be at least 1 positive and 1 negative review
// Probably just fix with padding
@Composable
fun EditReviewScreen(viewModel: EditReviewViewModel, activity: EditReviewActivity, prefs: SharedPreferences, reviewID: Int) {
    val positives by viewModel.livePositive.observeAsState(initial = mutableListOf())
    val negatives by viewModel.liveNegative.observeAsState(initial = mutableListOf())
    val images by viewModel.liveImage.observeAsState(initial = mutableListOf())
    val review by viewModel.liveReview.observeAsState(initial = null)

    var positiveText by remember { mutableStateOf(TextFieldValue("")) }
    var negativeText by remember { mutableStateOf(TextFieldValue("")) }

    val context = LocalContext.current

    var negativesCount by remember { mutableStateOf(1) }
    var positivesCount by remember { mutableStateOf(1) }

    //Tracking if we should use textField or just text
    val positiveStatus by remember { mutableStateOf(mutableListOf(false)) }
    val negativeStatus by remember { mutableStateOf(mutableListOf(false)) }

    //Tracking changes in texts
    val positiveStrings by remember { mutableStateOf(mutableListOf<String>()) }
    val negativeStrings by remember { mutableStateOf(mutableListOf<String>()) }

    //Loading
    if (review == null) {
        //TODO do better loading anim
        Text(text = "Loading")
        return
    }
    var reviewText by remember { mutableStateOf(TextFieldValue(review!!.text)) }
    var sliderPosition by remember { mutableStateOf(review!!.score/100f) }

    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .verticalScroll(enabled = true, state = rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add review to product",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4
        )
        //Spacer(modifier = Modifier.height(20.dp))

        //Positive attributes
        Text(text = "Positive attributes")
        //Spacer(modifier = Modifier.height(10.dp))

        for ((count, item) in positives.withIndex()) {
            while (positiveStatus.size <= count) {
                positiveStatus.add(false) //Set default to text not textField
            }
            if (positiveStrings.size <= count) {
                positiveStrings.add(item.text)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!positiveStatus[count]) {
                    Text(text = item.text)
                } else {
                    OutlinedTextField(
                        label = { Text("Edit positive") },
                        value = positiveStrings[count],
                        onValueChange = {
                            positiveStrings[count] = it
                            //Force UI update
                            sliderPosition += 0.01f
                            sliderPosition -= 0.01f
                        },
                        singleLine = true
                    )
                }


                ////Spacer(modifier = Modifier.width(10.dp))
                Button(onClick = {
                    if (positiveStatus[count]) {
                        viewModel.editPositive(positiveStrings[count], count)
                    }
                    positiveStatus[count] = !positiveStatus[count]
                    //Force UI update
                    sliderPosition += 0.01f
                    sliderPosition -= 0.01f
                }) {
                    Text(text = "Edit")
                }

                IconButton(
                    onClick = {
                        viewModel.deletePositive(item.text)
                        positiveStatus.removeAt(count)
                        positiveStrings.removeAt(count)
                        positivesCount--
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
            //Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = { Text("Positive $positivesCount") },
                value = positiveText,
                onValueChange = {
                    positiveText = it
                },
                singleLine = true
            )
            //Spacer(modifier = Modifier.width(20.dp))
            IconButton(
                onClick = {
                    if (positiveText.text.isEmpty())
                        return@IconButton
                    viewModel.addPositive(positiveText.text)
                    positiveText = TextFieldValue("")
                    positivesCount++
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.AddCircle),
                    contentDescription = null
                )
            }
        }

        ////Spacer(modifier = Modifier.width(20.dp)) - THIS WILL CRASH APP !

        //Negative attributes
        Text(text = "Negative attributes", modifier = Modifier.padding(20.dp))

        ////Spacer(modifier = Modifier.height(10.dp)) - THIS WILL CRASH APP !

        for ((count, item) in negatives.withIndex()) {

            while (negativeStatus.size <= count) {
                negativeStatus.add(false) //Set default to text not textField
            }
            if (negativeStrings.size <= count) {
                negativeStrings.add(item.text)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (!negativeStatus[count]) {
                    Text(text = item.text)
                } else {
                    OutlinedTextField(
                        label = { Text("Edit positive") },
                        value = negativeStrings[count],
                        onValueChange = {
                            negativeStrings[count] = it
                            //Force UI update
                            sliderPosition += 0.01f
                            sliderPosition -= 0.01f
                        },
                        singleLine = true
                    )
                }

                //Spacer(modifier = Modifier.width(10.dp))
                Button(onClick = {
                    if (negativeStatus[count]) {
                        viewModel.editNegative(negativeStrings[count], count)
                    }
                    negativeStatus[count] = !negativeStatus[count]
                    //Force UI update
                    sliderPosition += 0.01f
                    sliderPosition -= 0.01f
                }) {
                    Text(text = "Edit")
                }

                IconButton(
                    onClick = {
                        viewModel.deleteNegative(item.text)
                        negativeStatus.removeAt(count)
                        negativeStrings.removeAt(count)
                        negativesCount--
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
            //Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = { Text("Negative $negativesCount") },
                value = negativeText,
                onValueChange = {
                    negativeText = it
                },
                singleLine = true,
            )
            //Spacer(modifier = Modifier.width(20.dp))
            IconButton(
                onClick = {
                    if (negativeText.text.isEmpty())
                        return@IconButton
                    viewModel.addNegative(negativeText.text)
                    negativeText = TextFieldValue("")
                    negativesCount++
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.AddCircle),
                    contentDescription = null
                )
            }
        }

        //Review text
        //Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Review text")
        //Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            label = {
                Text("Review Text")
            },
            value = reviewText,
            onValueChange = {
                reviewText = it
            },
            singleLine = false,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        )

        //Score
        //Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Score")
        //Spacer(modifier = Modifier.height(10.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            modifier = Modifier.width(280.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Gray,
                activeTrackColor = Color.LightGray,
                inactiveTrackColor = Color.DarkGray
            )
        )
        Text(text = ((sliderPosition * 100).roundToInt() / 10.0).toString())

        //Load images
        //Spacer(modifier = Modifier.height(20.dp))
        Row() {
            Button(
                onClick = {
                    //addReviewActivity.loadImagesFromGallery()
                    // TODO add images from gallery
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray
                )
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.AddPhotoAlternate),
                    tint = Color.Black,
                    contentDescription = null
                )
                Text(
                    text = "Add photos",
                    color = Color.Black
                )
            }
            //Spacer(modifier = Modifier.width(20.dp))
            Button(
                onClick = {
                    //addReviewActivity.loadImagesFromGallery() /
                    // TODO remove images (on server + just added)
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray
                )
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.AddPhotoAlternate),
                    tint = Color.Black,
                    contentDescription = null
                )
                Text(
                    text = "Remove photos",
                    color = Color.Black
                )
            }
        }

        //Spacer(modifier = Modifier.height(20.dp))
        for (image in images) {
            GlideImage(
                data = image,
                contentDescription = "My content description",
                fadeIn = true,
                modifier = Modifier.clickable(onClick = {
                    println(image)  //TODO delete photo
                })
            )
            //Spacer(modifier = Modifier.height(10.dp))
        }

        //Add review
        Button(
            onClick = {
                MainScope().launch(Dispatchers.Main) {
                    try {
                        val auth = prefs.getString("token", "") ?: ""
                        withContext(Dispatchers.IO) {
                            // do blocking networking on IO thread
                            val info = DataGetter.updateReview(
                                ReviewPutInfo(
                                    reviewText.text,
                                    (positives + negatives) as MutableList<ReviewAttributePostPutInfo>,
                                    (sliderPosition * 100).roundToInt()
                                ), reviewID, auth
                            )
                            /*for(image in images) {
                                DataGetter.uploadPhoto(info.id,image,auth,context)
                            }*/
                        }
                        //Need to be called here to prevent blocking UI
                        openScreen(context, AccountActivity())
                    } catch (e: Exception) {
                        println(e.stackTraceToString())
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Save changes",
                color = Color.Black
            )
        }

        //Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                //Close activity
                activity.finish()
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Discard changes",
                color = Color.Black
            )
        }
        //Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = {
                //Delete review
                MainScope().launch(Dispatchers.Main) {
                    try {
                        val auth = prefs.getString("token", "") ?: ""
                        withContext(Dispatchers.IO) {
                            // do blocking networking on IO thread
                            DataGetter.deleteReview(reviewID, auth)
                            /*for(image in images) {
                                DataGetter.uploadPhoto(info.id,image,auth,context)
                            }*/
                        }
                        //Need to be called here to prevent blocking UI
                        openScreen(context, AccountActivity())
                    } catch (e: Exception) {
                        println(e.stackTraceToString())
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Delete review",
                color = Color.Black
            )
        }
    }
}

