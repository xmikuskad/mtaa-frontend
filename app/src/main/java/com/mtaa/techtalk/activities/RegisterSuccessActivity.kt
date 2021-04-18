package com.mtaa.techtalk.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class RegisterSuccessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)
        setLanguage(prefs.getString("language", "English"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                Surface(color = MaterialTheme.colors.background) {
                    RegisterSuccessScreen()
                }
            }
        }
    }
}

@Composable
fun RegisterSuccessScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.success_logo_transparent),
            contentDescription = null
        )
        Button(
            modifier = Modifier
                .padding(10.dp)
                .size(250.dp, 50.dp),
            onClick = {
                val intent = Intent(context, MainMenuActivity::class.java)
                intent.putExtra("activity", "create-account")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
        ) {
            Text(
                text = context.getString(R.string.continue_to_techtalk),
                fontSize = 16.sp
            )
        }
        Button(
            modifier = Modifier
                .padding(10.dp)
                .size(250.dp, 50.dp),
            onClick = {
                val intent = Intent(context, LoginActivity::class.java)
                intent.putExtra("activity", "first-launch")
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
        )
        {
            Icon(
                modifier = Modifier.size(28.dp, 28.dp),
                painter = rememberVectorPainter(image = Icons.Filled.Login),
                contentDescription = null
            )
            Text(
                text = context.getString(R.string.log_in),
                fontSize = 16.sp
            )
        }
    }
}