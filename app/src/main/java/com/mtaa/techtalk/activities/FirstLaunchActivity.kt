package com.mtaa.techtalk.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme

class FirstLaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            TechTalkTheme(true) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { println("Log-In Screen") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_login_36),
                            contentDescription = null
                        )
                        Text("Log-In")
                    }
                    Button(onClick = { println("Create Account Screen") }) {
                        Text("Create Account")
                    }
                    Button(onClick = {
                        val intent = Intent(context, MainMenuActivity::class.java)
                        intent.putExtra("activity", "first-launch")
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                        context.startActivity(intent)
                    }) {
                        Text("Continue without Account")
                    }
                }
            }
        }
    }
}