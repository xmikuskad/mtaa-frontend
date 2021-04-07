package com.mtaa.techtalk.activities

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.mtaa.techtalk.activities.DropdownList
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
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp
        )
        Spacer(
            modifier = Modifier.size(20.dp)
        )
        DropdownList(
            items = listOf("English", "Slovak"),
            label = "Select Language"
        )
        Spacer(
            modifier = Modifier.size(10.dp)
        )
        DropdownList(
            items = listOf("Dark Mode", "Light Mode"),
            label = "Select Color Scheme"
        )
    }
}

@Composable
fun DropdownList(items: List<String>, label: String? = null) {
    val expanded = remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf("") }

    OutlinedTextField(
        value = selected.value,
        onValueChange = { selected.value = it },
        label = {
            if (label != null) {
                Text(text = label)
            }
        },
        readOnly = true,
        enabled = false,
        trailingIcon = {
            IconButton(onClick = { expanded.value = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null
                )
            }
        }
    )

    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = {
            expanded.value = false
        }
    ) {
        items.forEach {
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    selected.value = it
                }
            ) {
                Text(it)
            }
        }
    }
}