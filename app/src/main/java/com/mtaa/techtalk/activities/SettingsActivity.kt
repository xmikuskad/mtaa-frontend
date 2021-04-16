package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import java.util.*

class SettingsActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setLanguage(prefs.getString("language", "en"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
                val scope = rememberCoroutineScope()
                Scaffold(
                    scaffoldState = scaffoldState,
                    topBar = { TopBar(scaffoldState, scope) },
                    drawerContent = { Drawer(prefs) }
                ) {
                    SettingsScreen(prefs)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getString(R.string.settings),
            fontSize = 28.sp
        )
        Spacer(
            modifier = Modifier.size(20.dp)
        )

        val selectedLanguage = remember { mutableStateOf(prefs.getString("language", "English")?:"English") }
        DropdownList(
            items = listOf("English", "Slovenčina"),
            label = context.getString(R.string.select_language),
            selected = selectedLanguage
        )
        Spacer(
            modifier = Modifier.size(10.dp)
        )
        val selectedScheme = remember { mutableStateOf(prefs.getString("color-scheme", "Dark Mode")?:"Dark Mode") }
        DropdownList(
            items = listOf("Dark Mode", "Light Mode"),
            label = context.getString(R.string.select_color),
            selected = selectedScheme
        )
        Button(
            onClick = {
                prefs.edit().putString("language", selectedLanguage.value).apply()
                prefs.edit().putString("color-scheme", selectedScheme.value).apply()
                showMessage(context, context.getString(R.string.saved), Toast.LENGTH_LONG)

                val intent = Intent(context, MainMenuActivity::class.java)
                intent.putExtra("activity","settings")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            },
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = context.getString(R.string.save_changes),
            )
        }
    }
}

@Composable
fun DropdownList(items: List<String>, label: String? = null, selected: MutableState<String>) {
    val expanded = remember { mutableStateOf(false) }

    Box {
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
            },
            modifier = Modifier.fillMaxWidth()
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
}

fun setLanguage(language: String?, context: Context) {
    if (language == null) {
        return
    }
    val myLocale = Locale(
        if (language == "English") {
            "en"
        } else {
            "sk"
        }
    )
    Locale.setDefault(myLocale)
    val resources: Resources = context.resources
    val dm: DisplayMetrics = resources.displayMetrics
    val config: Configuration = resources.configuration
    config.setLocale(myLocale)
    resources.updateConfiguration(config, dm)
}

fun setColorScheme(prefs: SharedPreferences): Boolean {
    if (prefs.getString("color-scheme", "Dark Mode") == "Light Mode")
        return false
    return true
}