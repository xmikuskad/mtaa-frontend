package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.ProductInfo
import com.mtaa.techtalk.ProductsInfo
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.SearchBarDark
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class SearchActivity : ComponentActivity() {
    private lateinit var viewModel: SearchViewModel
    private lateinit var searchInput: String

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        searchInput = intent.getStringExtra("search-input") ?: ""
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
                    SearchScreen(searchInput, viewModel)
                    viewModel.loadSearchResultProducts(searchInput)
                }
            }
        }
    }
}

class SearchViewModel: ViewModel() {
    val liveSearchResultProducts = MutableLiveData<List<ProductInfo>>()
    private var page = 1

    fun loadSearchResultProducts(searchInput: String) {
        MainScope().launch(Dispatchers.Main) {
            try {
                val foundProducts: ProductsInfo
                withContext(Dispatchers.IO) {
                    // do blocking networking on IO thread
                    foundProducts = DataGetter.search(searchInput, page)
                    page++
                }
                liveSearchResultProducts.value = liveSearchResultProducts.value?.plus(foundProducts.products) ?: foundProducts.products
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }
}

@Composable
fun SearchScreen(searchInput: String, viewModel: SearchViewModel) {
    val searchResultProducts by viewModel.liveSearchResultProducts.observeAsState(initial = null)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${context.getString(R.string.search_results_for)} '$searchInput'",
            style = TextStyle(fontSize = 20.sp),
            textAlign = TextAlign.Center
        )
        when {
            searchResultProducts == null -> {
                LoadingScreen(context.getString(R.string.loading_products))
            }
            searchResultProducts!!.isEmpty() -> {
                NotFoundScreen(context.getString(R.string.no_products_found))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 10.dp)
                ) {
                    itemsIndexed(searchResultProducts!!) { index, item ->
                        ProductBox(product = item)
                        if (index == searchResultProducts!!.lastIndex) {
                            viewModel.loadSearchResultProducts(searchInput)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(open: MutableState<Boolean>) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

    val barColor = if (setColorScheme(prefs)) {
        SearchBarDark
    } else {
        Color.White
    }

    val textColor = if (setColorScheme(prefs)) {
        Color.White
    } else {
        Color.Black
    }

    val searchText = remember { mutableStateOf(TextFieldValue()) }
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .size(48.dp),
        textStyle = TextStyle(
            fontSize = 12.sp,
            color = textColor
        ),
        value = searchText.value,
        onValueChange = {
            searchText.value = it
        },
        singleLine = true,
        leadingIcon = {
            IconButton(
                onClick = {
                    open.value = false
                }
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Close),
                    contentDescription = null,
                    tint = textColor
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    if (searchText.value.text.isNotEmpty()) {
                        openSearchScreen(context, searchText.value.text)
                    } else {
                        showMessage(context, context.getString(R.string.no_search_input), Toast.LENGTH_SHORT)
                    }
                }
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Search),
                    contentDescription = null,
                    tint = textColor
                )
            }
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = barColor,
            focusedIndicatorColor = barColor,
            unfocusedIndicatorColor = barColor
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (searchText.value.text.isNotEmpty()) {
                    openSearchScreen(context, searchText.value.text)
                } else {
                    showMessage(context, context.getString(R.string.no_search_input), Toast.LENGTH_SHORT)
                }
            }
        )
    )
}

fun openSearchScreen(context: Context, searchInput: String) {
    val intent = Intent(context, SearchActivity::class.java)
    intent.putExtra("search-input", searchInput)
    context.startActivity(intent)
}