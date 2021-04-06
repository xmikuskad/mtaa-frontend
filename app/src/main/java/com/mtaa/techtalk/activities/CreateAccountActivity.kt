package com.mtaa.techtalk.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns.EMAIL_ADDRESS
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.mtaa.techtalk.AuthInfo
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.client.features.*
import io.ktor.http.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.regex.Pattern

class CreateAccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TechTalkTheme(true) {
                Surface(color = MaterialTheme.colors.background) {
                    CreateAccountScreen()
                }
            }
        }
    }
}

@Composable
fun CreateAccountScreen() {
    val context = LocalContext.current
    val passwordRules = "Your password must include:\n\n" +
            "- minimum eight characters,\n" +
            "- at least one uppercase letter,\n" +
            "- one lowercase letter,\n" +
            "- one digit,\n" +
            "- one special character (#?!@\$%^&*-)"
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val nameState = remember { mutableStateOf(TextFieldValue()) }
        val isValidName = nameState.value.text != ""
        TextField(
            label = {
                val label = if (isValidName) {
                    "Enter your name"
                } else {
                    "Enter your name*"
                }
                Text(label)
            },
            value = nameState.value,
            onValueChange = { nameState.value = it },
            singleLine = true,
            modifier = Modifier.size(250.dp, 55.dp),
            isError = !isValidName,
            leadingIcon = {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Filled.Face),
                    contentDescription = null
                )
            }
        )

        Spacer(modifier = Modifier.size(10.dp))
        val emailState = remember { mutableStateOf(TextFieldValue()) }
        val isValidEmail = EMAIL_ADDRESS.matcher(emailState.value.text).matches()
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

        Spacer(modifier = Modifier.size(10.dp))
        val passwordState = remember { mutableStateOf(TextFieldValue()) }
        // Minimum eight characters, at least one uppercase letter, one lowercase letter, one digit and one special character
        val passwordRegex = Pattern.compile(
            "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@\$%^&*-]).{8,}\$"
        )
        val isValidPassword = passwordRegex.matcher(passwordState.value.text).matches()
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
            },
            trailingIcon = {
                if (!isValidPassword) {
                    Icon(
                        modifier = Modifier.clickable(
                            onClick = {
                                showMessage(context, passwordRules, Toast.LENGTH_LONG)
                            }
                        ),
                        painter = rememberVectorPainter(image = Icons.Filled.Info),
                        contentDescription = null
                    )
                } else {
                    Icon(
                        painter = rememberVectorPainter(image = Icons.Filled.CheckCircle),
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(modifier = Modifier.size(10.dp))
        val secondPasswordState = remember { mutableStateOf(TextFieldValue()) }
        val isValidSecondPassword = secondPasswordState.value.text == passwordState.value.text
        TextField(
            label = {
                val label = if (isValidSecondPassword) {
                    "Confirm password"
                } else {
                    "Confirm password*"
                }
                Text(label)
            },
            value = secondPasswordState.value,
            onValueChange = { secondPasswordState.value = it },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.size(250.dp, 55.dp),
            isError = !isValidSecondPassword,
            leadingIcon = {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Filled.Password),
                    contentDescription = null
                )
            }
        )
        Button(
            modifier = Modifier.padding(30.dp).size(250.dp, 50.dp),
            onClick = {
                if (!isValidName || !isValidEmail || !isValidPassword || !isValidSecondPassword) {
                    var message = "Invalid:"
                    var num = 1
                    if (!isValidName) {
                        message += "\n $num. Name"
                        num++
                    }
                    if (!isValidEmail) {
                        message += "\n $num. E-mail"
                        num++
                    }
                    if (!isValidPassword) {
                        message += "\n $num. Password"
                        num++
                    }
                    if (!isValidSecondPassword) {
                        message += "\n $num. Confirmation Password"
                    }
                    showMessage(context, message, Toast.LENGTH_SHORT)
                } else {
                    MainScope().launch(Dispatchers.Main) {
                        try {
                            val createAccountResponse: HttpStatusCode
                            withContext(Dispatchers.IO) {
                                // do blocking networking on IO thread
                                createAccountResponse = DataGetter.createAccount(
                                    nameState.value.text,
                                    emailState.value.text,
                                    passwordState.value.text
                                )
                            }
                            //Need to be called here to prevent blocking UI

                            val intent = Intent(context, RegisterSuccessActivity::class.java)
                            intent.putExtra("activity", "create-account")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)

                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                            when (e) {
                                //User or server is offline TODO handle - show warning
                                is ConnectTimeoutException -> println("server or user offline")
                                // TODO
                                is ClientRequestException ->
                                    showMessage(
                                        context,
                                        "There already is an account with this e-mail!",
                                        Toast.LENGTH_SHORT
                                    )
                            }
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        )
        {
            Text(
                text = "Create Account",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}