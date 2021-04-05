package com.mtaa.techtalk.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class FirstLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TechTalkTheme(true) {
                FirstLaunchScreen()
            }
        }
    }
}

@Composable
fun FirstLaunchScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            modifier = Modifier.padding(10.dp),
            onClick = { println("Log-In Screen") },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.LightGray
            )
        )
        {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_login_36),
                contentDescription = null
            )
            Text(
                text = "Log-In",
                color = Color.Black
            )
        }
        Button(
            modifier = Modifier.padding(10.dp),
            onClick = { println("Create Account Screen") },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.LightGray
            )
        )
        {
            Text(
                text = "Create Account",
                color = Color.Black
            )
        }
        Button(
            modifier = Modifier.padding(10.dp),
            onClick = {
                val intent = Intent(context, MainMenuActivity::class.java)
                intent.putExtra("activity", "first-launch")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.LightGray
            )
        ) {
            Text(
                text = "Continue without Account",
                color = Color.Black
            )
        }
    }
}