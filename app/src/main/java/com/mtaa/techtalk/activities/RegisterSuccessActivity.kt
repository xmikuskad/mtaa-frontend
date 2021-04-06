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
import androidx.compose.ui.graphics.Color
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

        setContent {
            TechTalkTheme(true) {
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
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Continue to TechTalk",
                color = Color.Black,
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
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        )
        {
            Icon(
                modifier = Modifier.size(28.dp, 28.dp),
                painter = rememberVectorPainter(image = Icons.Filled.Login),
                contentDescription = null,
                tint = Color.Black
            )
            Text(
                text = "Log-In",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}