package com.mtaa.techtalk.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns.EMAIL_ADDRESS
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Password
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.AuthInfo
import com.mtaa.techtalk.DataGetter.login
import com.mtaa.techtalk.R
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.client.features.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException

class LoginActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setLanguage(prefs.getString("language", "English"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
                Surface(color = MaterialTheme.colors.background) {
                    LoginScreen(prefs)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val emailState = remember { mutableStateOf(TextFieldValue()) }
        val isValidEmail = EMAIL_ADDRESS.matcher(emailState.value.text).matches()

        //Email field
        OutlinedTextField(
            label = {
                val label = if (isValidEmail) {
                    context.getString(R.string.enter_email)
                } else {
                    context.getString(R.string.enter_email_fail)
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

        //Password field
        Spacer(modifier = Modifier.size(10.dp))
        val passwordState = remember { mutableStateOf(TextFieldValue()) }
        val isValidPassword = passwordState.value.text != ""
        OutlinedTextField(
            label = {
                val label = if (isValidPassword) {
                    context.getString(R.string.enter_password)
                } else {
                    context.getString(R.string.enter_password_fail)
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        //Login button
        Button(
            modifier = Modifier
                .padding(30.dp)
                .size(250.dp, 55.dp),
            onClick = {

                //Input validation
                if (!isValidEmail && !isValidPassword) {
                    showMessage(
                        context,
                        context.getString(R.string.invalid_pass_mail),
                        Toast.LENGTH_SHORT
                    )
                } else if (!isValidEmail) {
                    showMessage(
                        context,
                        context.getString(R.string.invalid_mail),
                        Toast.LENGTH_SHORT
                    )
                } else if (!isValidPassword) {
                    showMessage(
                        context,
                        context.getString(R.string.invalid_pass),
                        Toast.LENGTH_SHORT
                    )
                } else {
                    MainScope().launch(Dispatchers.Main) {
                        try {
                            val loginResponse: AuthInfo
                            withContext(Dispatchers.IO) {
                                // do blocking networking on IO thread
                                loginResponse =
                                    login(emailState.value.text, passwordState.value.text)
                            }
                            //Need to be called here to prevent blocking UI

                            prefs.edit().putString("token", loginResponse.key).apply()
                            prefs.edit().putString("username", loginResponse.name).apply()

                            val intent = Intent(context, MainMenuActivity::class.java)
                            intent.putExtra("activity", "log-in")
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)

                        } catch (e: Exception) {
                            println(e.stackTraceToString())
                            when (e) {
                                is ConnectTimeoutException -> {
                                    showMessage(
                                        context,
                                        context.getString(R.string.err_server_offline),
                                        Toast.LENGTH_LONG
                                    )
                                }
                                is ConnectException -> {
                                    showMessage(
                                        context,
                                        context.getString(R.string.err_no_internet),
                                        Toast.LENGTH_LONG
                                    )
                                }
                                is ClientRequestException ->
                                    showMessage(
                                        context,
                                        context.getString(R.string.account_not_found),
                                        Toast.LENGTH_LONG
                                    )
                            }
                        }
                    }
                }
            }
        ) {
            Text(
                text = context.getString(R.string.log_in),
                fontSize = 16.sp
            )
        }
    }
}