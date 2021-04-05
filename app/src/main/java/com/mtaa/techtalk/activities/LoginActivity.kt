package com.mtaa.techtalk.activities

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.DataGetter.login
import com.mtaa.techtalk.ReviewsInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.regex.Pattern
import kotlin.text.Regex.Companion.fromLiteral

class LoginActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TechTalkTheme(true) {
                Surface(color = MaterialTheme.colors.background) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter your e-mail:")
        val emailState = remember { mutableStateOf(TextFieldValue()) }
        val isValidEmail = emailState.value.text.count() > 5 && '@' in emailState.value.text
        TextField(
            label = {
                val label = if (isValidEmail) {
                    "Enter e-mail"
                } else {
                    "Enter e-mail*"
                }
                Text(label)
            },
            value = emailState.value,
            onValueChange = { emailState.value = it },
            singleLine = true,
            modifier = Modifier.size(250.dp, 55.dp),
            isError = !isValidEmail,
            leadingIcon = {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Filled.Email),
                    contentDescription = null
                )
            }
        )
        Text("Enter your password:")
        val passwordState = remember { mutableStateOf(TextFieldValue()) }

        val isValidPassword = passwordState.value.text != ""
        TextField(
            label = {
                val label = if (isValidPassword) {
                    "Enter password"
                } else {
                    "Enter password*"
                }
                Text(label)
            },
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.size(250.dp, 55.dp),
            isError = !isValidPassword,
            leadingIcon = {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Filled.Password),
                    contentDescription = null
                )
            }
        )
        Button(
            modifier = Modifier
                .padding(30.dp)
                .size(250.dp, 50.dp),
            onClick = {
                if (!isValidEmail && !isValidPassword) {
                    showMessage(context, "Invalid e-mail or password!")
                }
                else if (!isValidEmail) {
                    showMessage(context, "Invalid e-mail!")
                }
                else if (!isValidPassword) {
                    showMessage(context, "Invalid password!")
                }
                else {
                    MainScope().launch(Dispatchers.Main) {
                        try {
                            val loginResponse: HttpResponseData
                            withContext(Dispatchers.IO) {
                                // do blocking networking on IO thread
                                loginResponse = login(emailState.value.text, passwordState.value.text)
                            }
                            //Need to be called here to prevent blocking UI

                            val intent = Intent(context, MainMenuActivity::class.java)
                            intent.putExtra("activity", "log-in")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)

                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                            when (e) {
                                //User or server is offline TODO handle - show warning
                                is ConnectTimeoutException -> println("server or user offline")
                                // TODO
                                is ClientRequestException -> showMessage(context, "This account does not exist!")
                            }
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Log-In",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}

fun showMessage(context: Context, message:String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}