package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.ProductInfo
import com.mtaa.techtalk.ProductsInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class ProductsActivity : ComponentActivity() {

    private lateinit var viewModel: ProductScreenViewModel
    private lateinit var categoryName: String
    private var categoryId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.enterTransition = null
        window.exitTransition = null

        viewModel = ViewModelProvider(this).get(ProductScreenViewModel::class.java)
        categoryId = intent.getIntExtra("categoryId", 0)
        categoryName = intent.getStringExtra("categoryName") ?: "Unknown name"
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {
                    ProductsScreen(categoryId, categoryName, viewModel)
                    viewModel.loadProducts(categoryId)
                }
            }
        }
    }
}

//This class is responsible for updating list data
//NOTE: we cant actually download data in this class because downloading is blocking and UI will lag !!
class ProductScreenViewModel: ViewModel() {

    val liveProducts = MutableLiveData<List<ProductInfo>>()
    private var page = 1

    fun loadProducts(categoryId: Int) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val products: ProductsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    products = DataGetter.getProducts(categoryId,page)
                    page++
                }
                liveProducts.value = liveProducts.value?.plus(products.products) ?: products.products
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

}

@Composable
fun ProductsScreen(categoryId:Int,categoryName:String,viewModel: ProductScreenViewModel) {
    val products by viewModel.liveProducts.observeAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Category $categoryName",
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            itemsIndexed(products) { index,item ->
                ProductBox(product = item)
                if(index == products.lastIndex) {
                    viewModel.loadProducts(categoryId)
                }
            }
        }
    }
}

@Composable
fun ProductBox(product: ProductInfo) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .clickable(onClick = { openReviewsMenu(product,context) }),
        backgroundColor = Color.DarkGray
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = product.name ,
                modifier = Modifier.padding(5.dp),
                style = TextStyle(fontSize = 18.sp,fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.size(10.dp))
            Row {
                Text(text = "${product.price.div(10.0)} Euro", modifier = Modifier.padding(5.dp))
                Spacer(Modifier.size(50.dp))
                Text(text = "Score ${product.score.div(10.0)}/10", modifier = Modifier.padding(5.dp))
            }
        }
    }
}

fun openReviewsMenu(product : ProductInfo, context: Context){
    val intent = Intent(context, ReviewsActivity::class.java)
    intent.putExtra("productId",product.product_id)
    intent.putExtra("productName",product.name)
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    context.startActivity(intent)
}