package com.mtaa.techtalk.activities

import com.mtaa.techtalk.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.*
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.roundToInt

const val PRICE_MULTIPLIER = 10000
const val ASCENDING = "asc"
const val DESSCENDING = "des"
const val PRICE = "price"
const val SCORE = "score"

class ProductsActivity : ComponentActivity() {

    private lateinit var viewModel: ProductScreenViewModel
    private lateinit var categoryName: String
    private var categoryId: Int = 0
    private var queryAttributes = QueryAttributes("","","",0f,1f,0f)

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(ProductScreenViewModel::class.java)
        categoryId = intent.getIntExtra("categoryId", 0)
        categoryName = intent.getStringExtra("categoryName") ?: "Unknown name"
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setLanguage(prefs.getString("language", "en"), this)

        setContent {
            TechTalkTheme(true) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()

                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {
                    ProductsScreen(categoryId, categoryName, viewModel, queryAttributes)
                }
            }
        }

        viewModel.loadProducts(categoryId,queryAttributes)
        viewModel.loadBrands(categoryId)
    }
}


//This class is responsible for updating list data
class ProductScreenViewModel: ViewModel() {

    val liveProducts = MutableLiveData<List<ProductInfo>>()
    val liveBrands = MutableLiveData<List<BrandInfo>>()

    private var page = 1

    fun loadProducts(categoryId: Int, obj:QueryAttributes) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val products: ProductsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    products = DataGetter.getProducts(categoryId,page,obj)
                    page++
                }
                liveProducts.value = liveProducts.value?.plus(products.products) ?: products.products
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

    fun reloadProducts(categoryId: Int, obj:QueryAttributes)
    {
        liveProducts.value = mutableListOf()
        page = 1
        loadProducts(categoryId,obj)
    }

    fun loadBrands(categoryID: Int) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val brands: BrandsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    brands = DataGetter.getCategoryBrands(categoryID)
                }
                liveBrands.value = liveBrands.value?.plus(brands.brands) ?: brands.brands
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

}

@Composable
fun ProductsScreen(categoryId:Int,categoryName:String,viewModel: ProductScreenViewModel, obj:QueryAttributes) {
    val products by viewModel.liveProducts.observeAsState(initial = emptyList())
    val filterState = remember { mutableStateOf(DrawerValue.Closed) }
    val orderState = remember { mutableStateOf(DrawerValue.Closed) }

    val context = LocalContext.current

    if (filterState.value == DrawerValue.Open) {
        val brands by viewModel.liveBrands.observeAsState(initial = emptyList())
        var minPrice by remember { mutableStateOf(obj.min_price) }
        var maxPrice by remember { mutableStateOf(obj.max_price) }
        var score by remember { mutableStateOf(obj.min_score) }

        //Storing status of checkboxes
        val map by remember { mutableStateOf(HashMap<Int, Boolean>()) }

        val brandsLoaded =
            if (obj.brands.isNotEmpty()) obj.brands.split(',').map { it.toInt() } else emptyList()

        if (map.size <= 0 && brands.isNotEmpty()) {
            for (brand in brands) {
                map[brand.brand_id] = brand.brand_id in brandsLoaded
            }
        }

        Column(
            modifier = Modifier
                .padding(30.dp)
                .verticalScroll(enabled = true, state = rememberScrollState())
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Filter",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = typography.h4
            )

            Spacer(modifier = Modifier.height(30.dp))
            Text(text = context.getString(R.string.min_price))
            Slider(
                value = minPrice,
                onValueChange = { minPrice = it },
                modifier = Modifier.width(280.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Gray,
                    activeTrackColor = Color.LightGray,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Text(text = ((minPrice * PRICE_MULTIPLIER).roundToInt()).toString())

            Spacer(modifier = Modifier.height(30.dp))
            Text(text = context.getString(R.string.max_price))
            Slider(
                value = maxPrice,
                onValueChange = { maxPrice = it },
                modifier = Modifier.width(280.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Gray,
                    activeTrackColor = Color.LightGray,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Text(text = ((maxPrice * PRICE_MULTIPLIER).roundToInt()).toString())

            Spacer(modifier = Modifier.height(30.dp))
            Text(text = context.getString(R.string.min_score))
            Slider(
                value = score,
                onValueChange = { score = it },
                modifier = Modifier.width(280.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Gray,
                    activeTrackColor = Color.LightGray,
                    inactiveTrackColor = Color.DarkGray
                )
            )
            Text(text = (((score) * 100).roundToInt() / 10.0).toString())

            Spacer(modifier = Modifier.height(30.dp))

            if (brands.isNotEmpty()) {
                Text(text = context.getString(R.string.brands))
                Spacer(modifier = Modifier.height(10.dp))
            }

            for (brand in brands) {
                if (map[brand.brand_id] == null)
                    map[brand.brand_id] = false
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = map[brand.brand_id] ?: false,
                        onCheckedChange = {
                            map[brand.brand_id] = it

                            //Updating to force UI recomposition
                            score += 0.01f
                            score -= 0.01f
                        }
                    )
                    Text(text = brand.name)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = {
                obj.min_price = minPrice
                obj.max_price = maxPrice
                obj.min_score = score
                var brandString = ""
                for (brand in brands) {
                    if (map[brand.brand_id] == true) {
                        brandString += "${brand.brand_id},"
                    }
                }
                if (brandString.isNotEmpty())
                    brandString = brandString.dropLast(1) //remove last ,
                obj.brands = brandString

                viewModel.reloadProducts(categoryId, obj)
                filterState.value = DrawerValue.Closed
            }) {
                Text(context.getString(R.string.apply_filter))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {

                for (brand in brands) {
                    map[brand.brand_id] = false
                }
                obj.brands = ""
                minPrice = 0f
                obj.min_price = 0f
                maxPrice = 1f
                obj.max_price = 1f
                score = 0f
                obj.min_score = 0f

                //Updating to force UI recomposition
                score += 0.01f
                score -= 0.01f

                viewModel.reloadProducts(categoryId, obj)
            }) {
                Text(context.getString(R.string.clear_filter))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                filterState.value = DrawerValue.Closed
            }) {
                Text(context.getString(R.string.close))
            }

        }
        return
    }

    if (orderState.value == DrawerValue.Open) {
        Dialog(onDismissRequest = { orderState.value = DrawerValue.Closed }) {

            Card(
                border = BorderStroke(1.dp, Color.Black)
            )
            {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(context.getString(R.string.order_by), style = typography.h5)
                    Spacer(modifier = Modifier.height(20.dp))
                    val selectedOrder = remember { mutableStateOf(context.getString(R.string.newest)) }
                    DropdownList(
                        items = listOf(
                            context.getString(R.string.newest),
                            context.getString(R.string.price_asc),
                            context.getString(R.string.price_desc),
                            context.getString(R.string.score_asc),
                            context.getString(R.string.score_desc)
                        ),
                        label = context.getString(R.string.order_by),
                        selected = selectedOrder
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        when (selectedOrder.value) {
                            context.getString(R.string.newest) -> {
                                obj.order_by = ""
                                obj.order_type = ""
                            }
                            context.getString(R.string.price_asc) -> {
                                obj.order_by = PRICE
                                obj.order_type = ASCENDING
                            }
                            context.getString(R.string.price_desc) -> {
                                obj.order_by = PRICE
                                obj.order_type = DESSCENDING
                            }
                            context.getString(R.string.score_asc) -> {
                                obj.order_by = SCORE
                                obj.order_type = ASCENDING
                            }
                            context.getString(R.string.score_desc) -> {
                                obj.order_by = SCORE
                                obj.order_type = DESSCENDING
                            }
                        }

                        viewModel.reloadProducts(categoryId, obj)
                        orderState.value = DrawerValue.Closed
                    }) {
                        Text(context.getString(R.string.save))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row() {
            IconButton(
                onClick = {
                    orderState.value = DrawerValue.Open
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.Sort),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = categoryName,
                textAlign = TextAlign.Center,
                style = typography.h4
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = {
                    filterState.value = DrawerValue.Open
                }
            ) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    painter = rememberVectorPainter(Icons.Filled.FilterAlt),
                    contentDescription = null
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            itemsIndexed(products) { index, item ->
                ProductBox(product = item)
                if (index == products.lastIndex) {
                    viewModel.loadProducts(categoryId, obj)
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
            .padding(15.dp)
            .fillMaxWidth()
            .clickable(onClick = { openReviewsMenu(product, context) }),
        backgroundColor = Color.DarkGray
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = product.name,
                modifier = Modifier.padding(top=10.dp),
                textAlign = TextAlign.Center,
                style = typography.h6
            )
            Spacer(Modifier.size(20.dp))
            Row {
                Text(text = "${product.price.div(100.0)}")
                Spacer(Modifier.size(5.dp))
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(Icons.Filled.Euro),
                    contentDescription = null
                )
                Spacer(Modifier.size(50.dp))
                Text(text = "${product.score.div(10.0)} / 10")
                Spacer(Modifier.size(5.dp))
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(Icons.Filled.Star),
                    contentDescription = null
                )
            }
        }
    }
}

fun openReviewsMenu(product : ProductInfo, context: Context){
    val intent = Intent(context, ReviewsActivity::class.java)
    intent.putExtra("productId",product.product_id)
    intent.putExtra("productName",product.name)
    context.startActivity(intent)
}