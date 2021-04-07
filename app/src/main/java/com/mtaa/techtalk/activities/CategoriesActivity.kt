package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.CategoryInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class CategoriesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    CategoryScreen()
                }
            }
        }
    }

}

@Composable
fun CategoryScreen() {
    val context = LocalContext.current
    val categories = SplashActivity.categories.categories

    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Categories",
            style = TextStyle(fontSize = 25.sp),
            textAlign = TextAlign.Center
        )
        LazyColumn(
            modifier = Modifier
                .padding(top = 10.dp)
        ) {
            items(categories) { item ->
                Card(
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .fillMaxWidth()
                        .clickable(onClick = { openProductsMenu(item, context) }),
                    backgroundColor = Color.DarkGray,
                ) {
                    Text(
                        item.name,
                        modifier = Modifier.padding(15.dp),
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 20.sp)
                    )
                }
            }
        }
    }
}

fun openProductsMenu(category : CategoryInfo, context: Context){
    val intent = Intent(context, ProductsActivity::class.java)
    intent.putExtra("categoryId", category.category_id)
    intent.putExtra("categoryName", category.name)
    context.startActivity(intent)
}