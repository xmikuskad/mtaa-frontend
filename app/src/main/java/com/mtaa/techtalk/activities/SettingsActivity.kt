package com.mtaa.techtalk.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class SettingsActivity : ComponentActivity() {
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
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp
        )
        Row {
            Text(
                text = "Language: ",
                fontSize = 24.sp
            )
            val expanded = remember { mutableStateOf(false)}
            val language = remember { mutableStateOf("English") }

            Text(
                text = language.value,
                fontSize = 24.sp
            )
            IconButton(onClick = { expanded.value = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = {
                    expanded.value = false
                }
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        language.value = "English"
                    }
                ) {
                    Text("English")
                }
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        language.value = "Slovak"
                    }
                ) {
                    Text("Slovak")
                }
            }
        }
        Row {
            Text(
                text = "Color Scheme: ",
                fontSize = 24.sp
            )
            val expanded = remember { mutableStateOf(false) }
            val colorScheme = remember { mutableStateOf("Dark Mode") }

            Text(
                text = colorScheme.value,
                fontSize = 24.sp
            )
            IconButton(onClick = { expanded.value = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = {
                    expanded.value = false
                }
            ) {
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        colorScheme.value = "Dark Mode"
                    },
                ) {
                    Text("Dark Mode")
                }
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        colorScheme.value = "Light Mode"
                    }
                ) {
                    Text("Light Mode")
                }
            }
        }
    }
}
