package com.mtaa.techtalk.activities

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mtaa.techtalk.R
import io.ktor.client.features.*
import io.ktor.network.sockets.*
import java.net.ConnectException

class EditReviewActivity : ComponentActivity() {

    private lateinit var viewModel: EditReviewViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(EditReviewViewModel::class.java)
        val reviewID = intent.getIntExtra("reviewID",-1)

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
                    EditReviewScreen(viewModel,this,prefs,reviewID)
                }
            }
        }
        loadReviewData(reviewID)
    }

    //Load review data
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
                //Review wasn't found
                println(e.stackTraceToString())
            }
        }
    }

    //Open android gallery
    fun loadImagesFromGallery(){
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select images"), PICK_IMAGES_CODE)
    }

    //Called after the gallery is closed
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

    //Get review data from server
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

    //Add positive attribute
    fun addPositive(text:String){
        positives.add(ReviewAttributePostPutInfo(text,true))
        livePositive.value = positives
    }

    //Edit positive attribute
    fun editPositive(text:String, position:Int) {
        positives[position] = ReviewAttributePostPutInfo(text,true)
        livePositive.value = positives
    }

    //Delete positive attribute
    fun deletePositive(text: String){
        positives.remove(ReviewAttributePostPutInfo(text,true))
        livePositive.value = positives
    }

    //Add negative attribute
    fun addNegative(text:String){
        negatives.add(ReviewAttributePostPutInfo(text,false))
        liveNegative.value = negatives
    }

    //Edit negative attribute
    fun editNegative(text:String, position:Int) {
        negatives[position] = ReviewAttributePostPutInfo(text,false)
        liveNegative.value = negatives
    }

    //Delete negative attribute
    fun deleteNegative(text:String){
        negatives.remove(ReviewAttributePostPutInfo(text,false))
        liveNegative.value = negatives
    }

    //Add photo uri to list
    fun addPhoto(uri: Uri?){
        liveImage.value = (liveImage.value?.plus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }

    //Delete photo uri
    fun deletePhoto(uri: Uri?){
        liveImage.value = (liveImage.value?.minus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }
}

//For some reason spacers cant be used in this screen. It crashes the app.
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

    //Loading review
    if (review == null) {
        LoadingScreen(context.getString(R.string.loading_review))
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

        //Title
        Text(
            text = context.getString(R.string.edit_review),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4
        )

        //Positive attributes section
        Text(
            text = context.getString(R.string.positive_attributes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

        //Show added attributes
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
                        label = { Text(context.getString(R.string.edit_positive)) },
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
                            modifier = Modifier
                                .size(36.dp)
                                .padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Edit),
                            contentDescription = null
                        )
                    }
                    else {
                        Icon(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(start = 10.dp),
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
                        modifier = Modifier
                            .size(36.dp)
                            .padding(start = 10.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
        }

        //Here we can add new positive attribute
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = { Text(context.getString(R.string.positive_attribute)) },
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

        //Negative attributes section
        Text(
            text = context.getString(R.string.negative_attributes),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 30.dp, bottom = 10.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )

        //Show already added negative attributes
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
                        label = { Text(context.getString(R.string.edit_negative)) },
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
                            modifier = Modifier
                                .size(36.dp)
                                .padding(start = 10.dp),
                            painter = rememberVectorPainter(Icons.Filled.Edit),
                            contentDescription = null
                        )
                    }
                    else {
                        Icon(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(start = 10.dp),
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
                        modifier = Modifier
                            .size(36.dp)
                            .padding(start = 10.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
        }

        //Here we can add new negative attributes
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = { Text(context.getString(R.string.negative_attribute)) },
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
                text = context.getString(R.string.review_text),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.h6
            )
        }

        OutlinedTextField(
            label = {
                Text(context.getString(R.string.review_text))
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
            text = context.getString(R.string.score),
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
                modifier = Modifier
                    .size(25.dp)
                    .padding(start = 5.dp),
                painter = rememberVectorPainter(Icons.Filled.Star),
                contentDescription = null
            )
        }

        //Photo buttons
        Row(
            modifier = Modifier.padding(top=20.dp,bottom = 20.dp)
        ) {
            Button(
                onClick = {
                    activity.loadImagesFromGallery()
                },
                modifier = Modifier.padding(end=10.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.AddPhotoAlternate),
                    contentDescription = null
                )
                Text(
                    text = context.getString(R.string.add_photos)
                )
            }
            Button(
                onClick = {
                    showPhotos = !showPhotos
                },
                modifier = Modifier.padding(start=10.dp)
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Collections),
                    contentDescription = null
                )
                Text(
                    text = if (showPhotos) {
                        context.getString(R.string.hide_photos)
                    } else context.getString(R.string.show_photos)
                )
            }
        }

        //Show images
        if (showPhotos) {
            //Images from gallery
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
                            Text(context.getString(R.string.delete))
                        }
                        //Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            //Already added images
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
                                Text(context.getString(R.string.delete))
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
                        when (e) {
                            is ConnectTimeoutException -> {
                                showMessage(
                                    context,
                                    context.getString(R.string.err_server_offline),
                                    Toast.LENGTH_LONG
                                )
                            }
                            is ConnectException -> {
                                showMessage(
                                    context,
                                    context.getString(R.string.err_no_internet),
                                    Toast.LENGTH_LONG
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Save),
                contentDescription = null
            )
            Text(
                text = context.getString(R.string.save_changes)
            )
        }

        //Discard changes
        Button(
            onClick = {
                //Close activity
                activity.finish()
            },
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.EditOff),
                contentDescription = null
            )
            Text(
                text = context.getString(R.string.discard_changes)
            )
        }

        //Delete review
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
                        when (e) {
                            is ConnectTimeoutException -> {
                                showMessage(
                                    context,
                                    context.getString(R.string.err_server_offline),
                                    Toast.LENGTH_LONG
                                )
                            }
                            is ConnectException -> {
                                showMessage(
                                    context,
                                    context.getString(R.string.err_no_internet),
                                    Toast.LENGTH_LONG
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.padding(top=15.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Delete),
                contentDescription = null
            )
            Text(
                text = context.getString(R.string.delete_review)
            )
        }
    }
}

