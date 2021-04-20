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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.glide.GlideImage
import com.mtaa.techtalk.*
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkGray
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException
import kotlin.math.roundToInt

const val PICK_IMAGES_CODE = 0

class AddReviewActivity: ComponentActivity() {
    private lateinit var viewModel: AddReviewViewModel

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(AddReviewViewModel::class.java)
        val productID = intent.getIntExtra("productID",-1)
        val productName = intent.getStringExtra("productName")?:""

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
                    AddReviewScreen(
                        addReviewActivity = this,
                        livePositives = viewModel.livePositive,
                        liveNegatives = viewModel.liveNegative,
                        viewModel = viewModel,
                        productID = productID,
                        prefs = prefs,
                        productName = productName
                    )
                }
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

    //This is called after closing the gallery
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

class AddReviewViewModel: ViewModel() {
    val livePositive= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveNegative= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveImage= MutableLiveData<List<Uri>>()

    //Add positive attribute to list
    fun addPositive(text:String){
        livePositive.value = livePositive.value?.plus(
            ReviewAttributePostPutInfo(text,true)
        ) ?: mutableListOf(ReviewAttributePostPutInfo(text,true))
    }

    //Delete positive attribute from list
    fun deletePositive(text: String){
        livePositive.value = livePositive.value?.minus(
            ReviewAttributePostPutInfo(text,true)
        )
    }

    //Add negative attribute to list
    fun addNegative(text:String){
        liveNegative.value = liveNegative.value?.plus(
            ReviewAttributePostPutInfo(text,false)
        ) ?: mutableListOf(ReviewAttributePostPutInfo(text,false))
    }

    //Delete negative attribute from list
    fun deleteNegative(text:String){
        liveNegative.value = liveNegative.value?.minus(
            ReviewAttributePostPutInfo(text,false)
        )
    }

    //Add photo uri to list
    fun addPhoto(uri:Uri?){
        liveImage.value = (liveImage.value?.plus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }

    //Remove photo uri from list
    fun deletePhoto(uri: Uri?){
        liveImage.value = (liveImage.value?.minus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }
}

@Composable
fun AddReviewScreen(
    addReviewActivity: AddReviewActivity,
    livePositives: LiveData<List<ReviewAttributePostPutInfo>>,
    liveNegatives: LiveData<List<ReviewAttributePostPutInfo>>,
    viewModel: AddReviewViewModel,
    productID:Int,
    prefs:SharedPreferences,
    productName:String
) {

    val positives by livePositives.observeAsState(initial = mutableListOf())
    val negatives by liveNegatives.observeAsState(initial = mutableListOf())
    val images by viewModel.liveImage.observeAsState(initial = mutableListOf())

    var positiveText by remember { mutableStateOf(TextFieldValue("")) }
    var negativeText by remember { mutableStateOf(TextFieldValue("")) }
    var reviewText by remember { mutableStateOf(TextFieldValue("")) }
    var sliderPosition by remember { mutableStateOf(0f) }

    val context = LocalContext.current

    var negativesCount by remember { mutableStateOf(1) }
    var positivesCount by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(enabled = true, state = rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        //Title
        Text(
            text = "${context.getString(R.string.add_review_to)} $productName",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h5
        )
        Spacer(Modifier.size(20.dp))

        //Positive attributes section
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(5.dp))
            Text(
                text = context.getString(R.string.positive_attributes),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )
        }

        Spacer(Modifier.size(10.dp))

        //Show added positives attributes
        for (item in positives) {
            Spacer(Modifier.size(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.text)
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = {
                    viewModel.deletePositive(item.text)
                    positivesCount--
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
        }

        //Here we can add positive attribute
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label =  { Text("${context.getString(R.string.positive)} $positivesCount") },
                value = positiveText,
                onValueChange = {
                    positiveText = it
                },
                singleLine = true
            )
            Spacer(Modifier.size(20.dp))
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
        Spacer(Modifier.size(20.dp))


        //Negative attributes section
        Row(
            modifier = Modifier.padding(start=20.dp,top=10.dp,end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(5.dp))
            Text(
                text = context.getString(R.string.negative_attributes),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6
            )
        }
        Spacer(Modifier.size(10.dp))

        //Show added negative attributes
        for (item in negatives) {
            Spacer(Modifier.size(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.text)
                Spacer(Modifier.width(10.dp))
                IconButton(
                    onClick = {
                    viewModel.deleteNegative(item.text)
                    negativesCount--
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(36.dp),
                        painter = rememberVectorPainter(Icons.Filled.RemoveCircle),
                        contentDescription = null
                    )
                }
            }
        }

        //Here we can add negative attribute
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label =  { Text("${context.getString(R.string.negative)} $negativesCount") },
                value = negativeText,
                onValueChange = {
                    negativeText = it
                },
                singleLine = true,
            )
            Spacer(Modifier.size(20.dp))
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
        Spacer(Modifier.size(30.dp))
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
                style = MaterialTheme.typography.h6
            )
        }

        //Review text
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
                .padding(10.dp)
                .fillMaxWidth()
        )

        //Score
        Spacer(Modifier.size(10.dp))
        Text(
            text = context.getString(R.string.score),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
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
            Spacer(Modifier.size(5.dp))
            Icon(
                modifier = Modifier.size(15.dp),
                painter = rememberVectorPainter(Icons.Filled.Star),
                contentDescription = null
            )
        }

        //Load images
        Spacer(Modifier.size(20.dp))
        Button(
            onClick = {
                addReviewActivity.loadImagesFromGallery()
            }
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.AddPhotoAlternate),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = context.getString(R.string.add_photos),
            )
        }

        //Show all added images
        Spacer(Modifier.size(20.dp))
        for (image in images) {
            Box {
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
                }
            }
            Spacer(Modifier.size(10.dp))
        }

        var isUploading by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var photoNum by remember { mutableStateOf(1) }

        //Upload progress indicator
        if (isUploading) {
            Dialog(
                onDismissRequest = { }
            ) {
                Card(
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = 50.dp,
                            bottom = 50.dp,
                            start = 20.dp,
                            end = 20.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("${context.getString(R.string.uploading_photo)} ${photoNum}...")
                        Spacer(modifier = Modifier.size(20.dp))
                        LinearProgressIndicator(
                            color = TechTalkGray,
                            progress = progress
                        )
                    }
                }
            }
        }

        //Add review
        Button(
            onClick = {
                if (reviewText.text.isEmpty()) {
                    showMessage(
                        context,
                        context.getString(R.string.review_text_empty),
                        Toast.LENGTH_SHORT
                    )
                    return@Button
                }

                if (negativesCount == 1 || positivesCount == 1) {
                    showMessage(
                        context,
                        context.getString(R.string.one_positive_negative),
                        Toast.LENGTH_SHORT
                    )
                    return@Button
                }

                MainScope().launch(Dispatchers.Main) {
                    try {
                        val auth = prefs.getString("token", "") ?: ""
                        withContext(Dispatchers.IO) {
                            // do blocking networking on IO thread
                            val info = DataGetter.createReview(
                                ReviewPostInfo(
                                    reviewText.text,
                                    (positives + negatives) as MutableList<ReviewAttributePostPutInfo>,
                                    productID, (sliderPosition * 100).roundToInt()
                                ), auth
                            )
                            isUploading = true
                            for ((i, image) in images.withIndex()) {
                                photoNum = i + 1
                                progress = (i + 1).toFloat() / images.size
                                DataGetter.uploadPhoto(info.id, image, auth, context)
                            }
                        }
                        //Need to be called here to prevent blocking UI
                        isUploading = false
                        openScreen(context, MainMenuActivity())
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
            }
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Filled.Add),
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(5.dp))
            Text(
                text = context.getString(R.string.add_review)
            )
        }
    }
}


