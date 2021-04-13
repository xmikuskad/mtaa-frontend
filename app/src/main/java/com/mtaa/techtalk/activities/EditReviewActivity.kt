package com.mtaa.techtalk.activities

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.res.painterResource

class EditReviewActivity : ComponentActivity() {

    private lateinit var viewModel: EditReviewViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(EditReviewViewModel::class.java)
        val reviewID = intent.getIntExtra("reviewID",-1)

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

    fun loadImagesFromGallery(){
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select images"), PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == PICK_IMAGES_CODE){
            if(resultCode == Activity.RESULT_OK){
                if(data!!.clipData != null) {
                    //Multiple images
                    val count = data.clipData?.itemCount ?: 0 //Number of images
                    for(i in 0 until count) {
                        val imageUri = data.clipData!!.getItemAt(i).uri
                        viewModel.addPhoto(imageUri)
                    }
                }
                else {
                    //One image
                    val imageUri = data.data
                    viewModel.addPhoto(imageUri)
                }
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

    fun deletePhoto(uri: Uri?){
        liveImage.value = (liveImage.value?.minus(uri) ?: mutableListOf(uri)) as List<Uri>?
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

    //Tracking if we should use textField or just text
    val positiveStatus by remember { mutableStateOf(mutableListOf(false)) }
    val negativeStatus by remember { mutableStateOf(mutableListOf(false)) }

    //Tracking changes in texts
    val positiveStrings by remember { mutableStateOf(mutableListOf<String>()) }
    val negativeStrings by remember { mutableStateOf(mutableListOf<String>()) }

    var showPhotos by remember { mutableStateOf(false) }
    val deletePhotos by remember { mutableStateOf(mutableListOf<Int>()) }

    //Loading
    if (review == null) {
        //TODO do better loading anim
        Text(text = "Loading")
        return
    }
    var reviewText by remember { mutableStateOf(TextFieldValue(review!!.text)) }
    var sliderPosition by remember { mutableStateOf(review!!.score / 100f) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(enabled = true, state = rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit review",
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4
        )

        //Positive attributes
        Text(
            text = "Positive attributes",
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

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
                    Text(text = item.text,modifier = Modifier.width(200.dp))
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
                        singleLine = true,
                        modifier = Modifier.width(250.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (positiveStatus[count]) {
                            viewModel.editPositive(positiveStrings[count], count)
                        }
                        positiveStatus[count] = !positiveStatus[count]
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    if (!positiveStatus[count]) {
                        Icon(
                            modifier = Modifier.size(36.dp).padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Edit),
                            contentDescription = null
                        )
                    }
                    else {
                        Icon(
                            modifier = Modifier.size(36.dp).padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Save),
                            contentDescription = null
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.deletePositive(item.text)
                        positiveStatus.removeAt(count)
                        positiveStrings.removeAt(count)
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp).padding(start=10.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = { Text("Positive attribute") },
                value = positiveText,
                onValueChange = {
                    positiveText = it
                },
                singleLine = true,
                modifier = Modifier.width(250.dp)
            )
            IconButton(
                onClick = {
                    if (positiveText.text.isEmpty())
                        return@IconButton
                    viewModel.addPositive(positiveText.text)
                    positiveText = TextFieldValue("")
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.AddCircle),
                    contentDescription = null
                )
            }
        }

        //Negative attributes
        Text(
            text = "Negative attributes",
            modifier = Modifier.fillMaxWidth().padding(top = 30.dp,bottom = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

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
                    Text(text = item.text,modifier = Modifier.width(200.dp))
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
                        singleLine = true,
                        modifier = Modifier.width(250.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (negativeStatus[count]) {
                            viewModel.editNegative(negativeStrings[count], count)
                        }
                        negativeStatus[count] = !negativeStatus[count]
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    if (!negativeStatus[count]) {
                        Icon(
                            modifier = Modifier.size(36.dp).padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Edit),
                            contentDescription = null
                        )
                    }
                    else {
                        Icon(
                            modifier = Modifier.size(36.dp).padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Save),
                            contentDescription = null
                        )
                    }
                }

                IconButton(
                    onClick = {
                        viewModel.deleteNegative(item.text)
                        negativeStatus.removeAt(count)
                        negativeStrings.removeAt(count)
                        //Force UI update
                        sliderPosition += 0.01f
                        sliderPosition -= 0.01f
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp).padding(start = 10.dp),
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
                label = { Text("Negative attribute") },
                value = negativeText,
                onValueChange = {
                    negativeText = it
                },
                singleLine = true,
                modifier = Modifier.width(250.dp)
            )
            //Spacer(modifier = Modifier.width(20.dp))
            IconButton(
                onClick = {
                    if (negativeText.text.isEmpty())
                        return@IconButton
                    viewModel.addNegative(negativeText.text)
                    negativeText = TextFieldValue("")
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 30.dp,bottom = 10.dp),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = rememberVectorPainter(Icons.Filled.Article),
                contentDescription = null
            )
            Text(
                text = "Review Text",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.h6
            )
        }

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
        Text(
            text = "Score",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(top = 20.dp),
        )
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

        Row(
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "${((sliderPosition * 100).roundToInt() / 10.0)} / 10",
                textAlign = TextAlign.Center
            )
            Icon(
                modifier = Modifier.size(25.dp).padding(start=5.dp),
                painter = painterResource(com.mtaa.techtalk.R.drawable.ic_star),
                contentDescription = null
            )
        }

        //Load images
        Row(
            modifier = Modifier.padding(top=20.dp,bottom = 20.dp)
        ) {
            Button(
                onClick = {
                    activity.loadImagesFromGallery()
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray
                ),
                modifier = Modifier.padding(end=10.dp)
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
            Button(
                onClick = {
                    showPhotos = !showPhotos
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray
                ),
                modifier = Modifier.padding(start=10.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Collections),
                    tint = Color.Black,
                    contentDescription = null
                )
                Text(
                    text = if (showPhotos) "Hide photos" else "Show photos",
                    color = Color.Black
                )
            }
        }

        if (showPhotos) {
            //Spacer(modifier = Modifier.height(20.dp))
            for (image in images) {
                Box(
                    modifier = Modifier.padding(top=20.dp)
                ){
                    GlideImage(
                        data = image,
                        contentDescription = "My content description",
                        fadeIn = true,
                        modifier = Modifier.clickable(onClick = {
                            println(image)
                        }),
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                        Button(onClick = { viewModel.deletePhoto(image) }) {
                            Text("Delete")
                        }
                        //Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            for (image in review!!.images) {
                if (image.image_id !in deletePhotos) {
                    Box(
                        modifier = Modifier.padding(top=20.dp)
                    ) {
                        GlideImage(
                            data = "$ADDRESS/reviews/$reviewID/photo/${image.image_id}",
                            contentDescription = "My content description",
                            fadeIn = true,
                            modifier = Modifier.clickable(onClick = {
                                println(image)
                            })
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Button(onClick = {
                                deletePhotos.add(image.image_id)
                                //Force UI update
                                sliderPosition += 0.01f
                                sliderPosition -= 0.01f
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        //Add review
        Button(
            onClick = {
                MainScope().launch(Dispatchers.Main) {
                    try {
                        val auth = prefs.getString("token", "") ?: ""
                        withContext(Dispatchers.IO) {
                            // do blocking networking on IO thread
                            DataGetter.updateReview(
                                ReviewPutInfo(
                                    reviewText.text,
                                    (positives + negatives) as MutableList<ReviewAttributePostPutInfo>,
                                    (sliderPosition * 100).roundToInt()
                                ), reviewID, auth
                            )
                            for (image in images) {
                                DataGetter.uploadPhoto(reviewID, image, auth, context)
                            }
                            for(image in deletePhotos) {
                                DataGetter.deletePhoto(reviewID,image,auth)
                            }
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
            ),
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Save),
                tint = Color.Black,
                contentDescription = null
            )
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
            ),
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.EditOff),
                tint = Color.Black,
                contentDescription = null
            )
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
            ),
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Delete),
                tint = Color.Black,
                contentDescription = null
            )
            Text(
                text = "Delete review",
                color = Color.Black
            )
        }
    }
}

