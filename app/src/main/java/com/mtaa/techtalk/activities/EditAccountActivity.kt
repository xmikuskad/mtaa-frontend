package com.mtaa.techtalk.activities

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Space
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.RegisterInfo
import com.mtaa.techtalk.ReviewAttributePostPutInfo
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

class EditAccountActivity : ComponentActivity() {
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
                    EditAccountScreen(this,prefs)
                }
            }
        }
    }
}

@Composable
fun EditAccountScreen(activity: EditAccountActivity, prefs: SharedPreferences) {
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

        Text(text = "Edit account info",modifier = Modifier.fillMaxWidth(),textAlign = TextAlign.Center,style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(50.dp))

        val nameState =
            remember { mutableStateOf(TextFieldValue(prefs.getString("username", "") ?: "")) }

        /**
        ^               // start of line
        [a-zA-Z]{2,}    // will except a name with at least two characters
        \s              // will look for white space between name and surname
        [a-zA-Z]{1,}    // needs at least 1 Character
        \'?-?           // possibility of ' or - for double barreled and hyphenated surnames - John D'Largy
        [a-zA-Z]{2,}    // will except a name with at least two characters
        \s?             // possibility of another whitespace
        ([a-zA-Z]{1,})? // possibility of a second surname
         */
        val usernameRegex = Pattern.compile(
            "^([a-zA-Z]{2,}\\s[a-zA-Z]{1,}'?-?[a-zA-Z]{2,}\\s?([a-zA-Z]{1,})?)"
        )
        val isValidName = usernameRegex.matcher(nameState.value.text).matches()
        OutlinedTextField(
            label = {
                val label = if (isValidName) {
                    "New name"
                } else {
                    "New name*"
                }
                Text(label)
            },
            value = nameState.value,
            onValueChange = { nameState.value = it },
            singleLine = true,
            modifier = Modifier.size(250.dp, 64.dp),
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
        val isValidEmail = Patterns.EMAIL_ADDRESS.matcher(emailState.value.text).matches()
        OutlinedTextField(
            label = {
                val label = if (isValidEmail) {
                    "New or old e-mail"
                } else {
                    "New or old e-mail*"
                }
                Text(label)
            },
            value = emailState.value,
            onValueChange = { emailState.value = it },
            singleLine = true,
            modifier = Modifier.size(250.dp, 64.dp),
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
        OutlinedTextField(
            label = {
                val label = if (isValidPassword) {
                    "New or old password"
                } else {
                    "New or old password*"
                }
                Text(label)
            },
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.size(250.dp, 64.dp),
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
        OutlinedTextField(
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
            modifier = Modifier.size(250.dp, 64.dp),
            isError = !isValidSecondPassword,
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
                .size(250.dp, 55.dp),
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
                    showMessage(context, message, Toast.LENGTH_LONG)
                } else {
                    MainScope().launch(Dispatchers.Main) {
                        try {
                            val auth = prefs.getString("token", "") ?: ""
                            withContext(Dispatchers.IO) {
                                // do blocking networking on IO thread

                                DataGetter.editAccount(
                                    RegisterInfo(
                                        nameState.value.text,
                                        passwordState.value.text,
                                        emailState.value.text
                                    ), auth
                                )
                            }
                            //Need to be called here to prevent blocking UI
                            val intent = Intent(context, MainMenuActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
                            context.startActivity(intent)

                            prefs.edit().putString("username", nameState.value.text).apply()
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
                text = "Save changes",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { activity.finish() },
            modifier = Modifier
                .size(250.dp, 55.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Gray
            )
        ) {
            Text(
                text = "Discard changes",
                color = Color.Black,
                fontSize = 16.sp
            )
        }
    }
}