package com.mtaa.techtalk.activities

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.LiveData
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

class AddReviewActivity: ComponentActivity() {
    lateinit var viewModel: AddReviewViewModel
    val PICK_IMAGES_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        viewModel = ViewModelProvider(this).get(AddReviewViewModel::class.java)
        val productID = intent.getIntExtra("productID",-1)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {
                    AddReviewScreen(this,livePositives = viewModel.livePositive, liveNegatives = viewModel.liveNegative,viewModel,productID,prefs)
                }
            }
        }
    }

    fun loadImagesFromGallery(){
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent,"Select images"),PICK_IMAGES_CODE)
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

class AddReviewViewModel: ViewModel() {
    val livePositive= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveNegative= MutableLiveData<List<ReviewAttributePostPutInfo>>()
    val liveImage= MutableLiveData<List<Uri>>()

    fun addPositive(text:String){
        livePositive.value = livePositive.value?.plus(ReviewAttributePostPutInfo(text,true)) ?: mutableListOf(ReviewAttributePostPutInfo(text,true))
    }

    fun deletePositive(text: String){
        livePositive.value = livePositive.value?.minus(ReviewAttributePostPutInfo(text,true))
    }

    fun addNegative(text:String){
        liveNegative.value = liveNegative.value?.plus(ReviewAttributePostPutInfo(text,false)) ?: mutableListOf(ReviewAttributePostPutInfo(text,false))
    }

    fun deleteNegative(text:String){
        liveNegative.value = liveNegative.value?.minus(ReviewAttributePostPutInfo(text,false))
    }

    fun addPhoto(uri:Uri?){
        liveImage.value = (liveImage.value?.plus(uri) ?: mutableListOf(uri)) as List<Uri>?
    }
}

@Composable
fun AddReviewScreen(addReviewActivity: AddReviewActivity,livePositives: LiveData<List<ReviewAttributePostPutInfo>>,
                    liveNegatives: LiveData<List<ReviewAttributePostPutInfo>>,viewModel: AddReviewViewModel, productID:Int,prefs:SharedPreferences) {
    val positives by livePositives.observeAsState(initial = mutableListOf())
    val negatives by liveNegatives.observeAsState(initial = mutableListOf())
    val images by viewModel.liveImage.observeAsState(initial = mutableListOf())

    var positiveText by remember { mutableStateOf(TextFieldValue("Text123")) }
    var negativeText by remember { mutableStateOf(TextFieldValue("Text123")) }
    var reviewText by remember { mutableStateOf(TextFieldValue("Text123")) }
    var sliderPosition by remember { mutableStateOf(0f) }

    var context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .verticalScroll(enabled = true, state = rememberScrollState())
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add review to product")
        Spacer(Modifier.size(20.dp))

        //Positive attributes
        Text(text = "Positive attributes")
        Spacer(Modifier.size(10.dp))

        for (item in positives) {
            Row() {
                Text(text = item.text)
                Spacer(Modifier.size(10.dp))
                Button(onClick = {
                    viewModel.deletePositive(item.text)
                }) {
                    Text(text = "Remove")
                }
            }
            Spacer(Modifier.size(10.dp))
        }

        Row() {
            TextField(
                value = positiveText,
                onValueChange = {
                    positiveText = it
                },
                singleLine = true,
            )
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    if (positiveText.text.isEmpty())
                        return@Button
                    viewModel.addPositive(positiveText.text)
                    positiveText = TextFieldValue("")
                }
            ) {
                Text(text = "Add")

            }
        }

        //Negative attributes

        Text(text = "Negative attributes")
        Spacer(Modifier.size(10.dp))

        for (item in negatives) {
            Row() {
                Text(text = item.text)
                Spacer(Modifier.size(10.dp))
                Button(onClick = {
                    viewModel.deleteNegative(item.text)
                }) {
                    Text(text = "Remove")
                }
            }
            Spacer(Modifier.size(10.dp))
        }

        Row() {
            TextField(
                value = negativeText,
                onValueChange = {
                    negativeText = it
                },
                singleLine = true,
            )
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    if (negativeText.text.isEmpty())
                        return@Button
                    viewModel.addNegative(negativeText.text)
                    negativeText = TextFieldValue("")
                }
            ) {
                Text(text = "Add")

            }
        }

        //Review text
        Spacer(Modifier.size(20.dp))
        Text(text = "Review text")
        Spacer(Modifier.size(10.dp))
        TextField(
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
        Spacer(Modifier.size(20.dp))
        Text(text = "Score")
        Spacer(Modifier.size(10.dp))
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            modifier = Modifier.width(280.dp)
        )
        Text(text = (Math.round(sliderPosition * 100) / 10.0).toString())

        //Load images
        Spacer(Modifier.size(20.dp))
        Button(onClick = {
            addReviewActivity.loadImagesFromGallery()
        }) {
            Text(text = "Add photos")
        }

        Spacer(Modifier.size(20.dp))
        for (image in images) {
            GlideImage(
                data = image,
                contentDescription = "My content description", fadeIn = true,
                modifier = Modifier.clickable(onClick = {
                    println(image)  //TODO delete photo
                })
            )
            Spacer(Modifier.size(10.dp))
        }

        //Add review
        Button(onClick = {
            MainScope().launch(Dispatchers.Main) {
                try {
                    val auth = prefs.getString("token","") ?: ""
                    val recentReviews: ReviewsInfo
                    withContext(Dispatchers.IO) {
                        // do blocking networking on IO thread
                        //recentReviews = DataGetter.getRecentReviews()
                        var info = DataGetter.createReview(
                            ReviewPostInfo(
                                reviewText.text,
                                (positives + negatives) as MutableList<ReviewAttributePostPutInfo>,
                                productID,Math.round(sliderPosition*100)
                            ),auth
                        )
                        //println("NEW REVIEW ID IS "+info.id)
                        for(image in images) {
                            //DataGetter.uploadPhoto(info.id,image,auth)
                            DataGetter.uploadPhoto(info.id,image,auth,context)
                        }
                    }
                    //Need to be called here to prevent blocking UI
                    openScreen(context, MainMenuActivity())
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                }
            }
        }) {
            Text(text = "Add review")

        }
    }

}


