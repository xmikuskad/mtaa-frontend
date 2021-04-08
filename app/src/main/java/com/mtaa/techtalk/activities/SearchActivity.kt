package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun SearchBar(open: MutableState<Boolean>) {
    val searchText = remember { mutableStateOf(TextFieldValue()) }
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().offset(y = (-4).dp),
        textStyle = TextStyle(fontSize = 12.sp),
        value = searchText.value,
        onValueChange = {
            searchText.value = it
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    open.value = false
                }
            ) {
                Icon(
                    painter = rememberVectorPainter(Icons.Filled.Close),
                    contentDescription = null,
                    tint = Color.White
                )
            }
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