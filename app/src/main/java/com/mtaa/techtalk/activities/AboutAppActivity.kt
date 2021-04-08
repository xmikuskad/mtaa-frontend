package com.mtaa.techtalk.activities

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import androidx.core.content.ContextCompat.startActivity

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.mtaa.techtalk.R


class AboutAppActivity : ComponentActivity() {
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
                    AboutAppScreen()
                }
            }
        }
    }
}

@Composable
fun AboutAppScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    )
    {
        Spacer(modifier = Modifier.height(15.dp))
        Text(text = "About app",modifier = Modifier.fillMaxWidth(),textAlign = TextAlign.Center,style = typography.h4)
        Spacer(modifier = Modifier.height(40.dp))
        Row() {
            Text(text = "App version")
            Spacer(modifier = Modifier.width(50.dp))
            Text(text = "v1.0")
        }

        Spacer(modifier = Modifier.height(30.dp))
        Row() {
            Text(text = "Authors")
            Spacer(modifier = Modifier.width(80.dp))
            Column() {
                Text(text = "Dominik Mikuška")
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Viktor Modroczký")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(text = "Bug reports")
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "https://github.com/xmikuskad/mtaa-frontend",
            modifier = Modifier.clickable(onClick = {
                val uri = Uri.parse("https://github.com/xmikuskad/mtaa-frontend")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            })
        )
        Image(
            modifier = Modifier.fillMaxWidth(),
            alignment = Alignment.Center,
            painter = painterResource(R.drawable.logo_transparent_banner),
            contentDescription = null
        )
    }
}