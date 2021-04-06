package com.mtaa.techtalk.activities

import android.os.Bundle
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.ui.theme.TechTalkTheme
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
            //visualTransformation = PasswordVisualTransformation(),
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
            //visualTransformation = PasswordVisualTransformation(),
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
                println("create acc")
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