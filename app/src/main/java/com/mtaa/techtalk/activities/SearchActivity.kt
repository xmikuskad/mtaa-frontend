package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.TextFieldValue

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

        }
    }
}

@Composable
fun SearchScreen(searchInput: String) {

}

@Composable
fun SearchBar() {
    val searchText = remember { mutableStateOf(TextFieldValue()) }
    OutlinedTextField(
        value = searchText.value,
        onValueChange = {
            searchText.value = it
        },
        trailingIcon = {
            IconButton(
                onClick = {

                }
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Search),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    )
}