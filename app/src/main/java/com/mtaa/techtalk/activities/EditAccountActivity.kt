package com.mtaa.techtalk.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mtaa.techtalk.DataGetter
import com.mtaa.techtalk.R
import com.mtaa.techtalk.RegisterInfo
import com.mtaa.techtalk.ui.theme.TechTalkTheme
import io.ktor.client.features.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.ConnectException
import java.util.regex.Pattern

class EditAccountActivity : ComponentActivity() {
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("com.mtaa.techtalk", MODE_PRIVATE)

        setLanguage(prefs.getString("language", "English"), this)

        setContent {
            TechTalkTheme(setColorScheme(prefs)) {
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
    val passwordRules = context.getString(R.string.password_rules)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = context.getString(R.string.edit_account_info),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h4
        )
        Spacer(modifier = Modifier.height(40.dp))

        val nameState =
            remember { mutableStateOf(TextFieldValue(prefs.getString("username", "") ?: "")) }

        /**
        ^               // start of line
        [a-zA-Z]{2,}    // will except a name with at least two characters
        \s              // will look for white space between name and surname
        [a-zA-Z]+    // needs at least 1 Character
        \'?-?           // possibility of ' or - for double barreled and hyphenated surnames - John D'Largy
        [a-zA-Z]{2,}    // will except a name with at least two characters
        \s?             // possibility of another whitespace
        ([a-zA-Z]+)? // possibility of a second surname
         */
        val usernameRegex = Pattern.compile(
            "^([\\p{L}]{2,}\\s[\\p{L}]+'?-?[\\p{L}]{2,}\\s?([\\p{L}]+)?)"
        )
        val isValidName = usernameRegex.matcher(nameState.value.text).matches()
        OutlinedTextField(
            label = {
                val label = if (isValidName) {
                    context.getString(R.string.new_name)
                } else {
                    context.getString(R.string.new_name_fail)
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
                    context.getString(R.string.new_or_old_email)
                } else {
                    context.getString(R.string.new_or_old_email_fail)
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
                    context.getString(R.string.new_or_old_password)
                } else {
                    context.getString(R.string.new_or_old_password_fail)
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
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.size(10.dp))
        val secondPasswordState = remember { mutableStateOf(TextFieldValue()) }
        val isValidSecondPassword = secondPasswordState.value.text == passwordState.value.text
        OutlinedTextField(
            label = {
                val label = if (isValidSecondPassword) {
                    context.getString(R.string.confirm_password)
                } else {
                    context.getString(R.string.confirm_password_fail)
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
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Button(
            modifier = Modifier
                .padding(top = 30.dp)
                .size(250.dp, 55.dp),
            onClick = {
                if (!isValidName || !isValidEmail || !isValidPassword || !isValidSecondPassword) {
                    var message = context.getString(R.string.invalid)
                    var num = 1
                    if (!isValidName) {
                        message += "\n $num. ${context.getString(R.string.name)}"
                        num++
                    }
                    if (!isValidEmail) {
                        message += "\n $num. E-mail"
                        num++
                    }
                    if (!isValidPassword) {
                        message += "\n $num. ${context.getString(R.string.password)}"
                        num++
                    }
                    if (!isValidSecondPassword) {
                        message += "\n $num. ${context.getString(R.string.confirmation_password)}"
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
                                        context.getString(R.string.duplicate_account),
                                        Toast.LENGTH_LONG
                                    )
                            }
                        }
                    }
                }
            }
        )
        {
            Text(
                text = context.getString(R.string.save_changes),
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Button(
            onClick = { activity.finish() },
            modifier = Modifier
                .size(250.dp, 55.dp),
        ) {
            Text(
                text = context.getString(R.string.discard_changes),
                fontSize = 16.sp
            )
        }
    }
}