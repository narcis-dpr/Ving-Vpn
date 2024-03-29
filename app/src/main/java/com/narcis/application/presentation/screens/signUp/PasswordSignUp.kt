package com.narcis.application.presentation.screens.signUp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.nekohasekai.sagernet.R
import com.narcis.application.presentation.components.ButtonGradient
import com.narcis.application.presentation.navigation.Navigation
import com.narcis.application.presentation.screens.landing.AnimatedLogo
import com.narcis.application.presentation.ui.theme.ApplicationTheme
import com.narcis.application.presentation.ui.theme.Blue0
import com.narcis.application.presentation.ui.theme.Blue1
import com.narcis.application.presentation.ui.theme.Neutral2
import com.narcis.application.presentation.ui.theme.Neutral3
import com.narcis.application.presentation.ui.theme.Sky0
import com.narcis.application.presentation.ui.theme.Sky1
import com.narcis.application.presentation.utiles.Visibility
import com.narcis.application.presentation.utiles.VisibilityOff
import com.narcis.application.presentation.viewModel.PasswordViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PasswordSignUp(
    navController: NavController?,
    passwordViewModel: PasswordViewModel = hiltViewModel()
) {
    BackHandler() {
        // Handle the back press here
        navController?.popBackStack()
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    var passwordOne by rememberSaveable {
        mutableStateOf("")
    }
    var passwordTwo by rememberSaveable {
        mutableStateOf("")
    }
    var passwordHiddenOne by rememberSaveable {
        mutableStateOf(true)
    }
    var passwordHiddenTwo by rememberSaveable {
        mutableStateOf(true)
    }
    val snackBarHostState = remember {
        SnackbarHostState()
    }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ApplicationTheme.colors.uiBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState)
        }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(color = ApplicationTheme.colors.uiBackground)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedLogo(
                modifier = Modifier
                    .fillMaxWidth(.3f)
                    .padding(bottom = 8.dp),
                colors = listOf(Sky1, Sky0)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter Password",
                color = ApplicationTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                maxLines = 1,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "create your free account now and experience the true potential of VPN",
                color = ApplicationTheme.colors.textSecondry,
                modifier = Modifier.padding(8.dp),
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = passwordOne,
                    onValueChange = { passwordOne = it },
                    shape = RoundedCornerShape(30.dp),
                    label = {
                        Text(
                            "Password",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    visualTransformation =
                    if (passwordHiddenOne) PasswordVisualTransformation() else VisualTransformation.None,
                    //  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = ApplicationTheme.colors.textPrimary,
                        unfocusedLabelColor = Neutral3,
                        placeholderColor = Color.White,
                        focusedBorderColor = Neutral2,
                        unfocusedBorderColor = Neutral2
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordHiddenOne = !passwordHiddenOne }) {
                            val visibilityIcon =
                                if (passwordHiddenOne) Visibility else VisibilityOff
                            // Please provide localized description for accessibility services
                            val description =
                                if (passwordHiddenOne) "Show password" else "Hide password"
                            Icon(imageVector = visibilityIcon, contentDescription = description)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.key_icon),
                            contentDescription = "password icon",
                            tint = Neutral2
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.92f),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            // do something here
                        }
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordTwo,
                    onValueChange = { passwordTwo = it },
                    shape = RoundedCornerShape(30.dp),
                    label = {
                        Text(
                            "Confirm Password",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    visualTransformation =
                    if (passwordHiddenTwo) PasswordVisualTransformation() else VisualTransformation.None,
                    //  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = ApplicationTheme.colors.textPrimary,
                        unfocusedLabelColor = Neutral3,
                        placeholderColor = Color.White,
                        focusedBorderColor = Neutral2,
                        unfocusedBorderColor = Neutral2
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordHiddenTwo = !passwordHiddenTwo }) {
                            val visibilityIcon =
                                if (passwordHiddenTwo) Visibility else VisibilityOff
                            // Please provide localized description for accessibility services
                            val description =
                                if (passwordHiddenTwo) "Show password" else "Hide password"
                            Icon(imageVector = visibilityIcon, contentDescription = description)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.key_icon),
                            contentDescription = "password icon",
                            tint = Neutral2
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.92f),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            // do something here
                        }
                    )
                )
                Spacer(modifier = Modifier.padding(10.dp))
                ButtonGradient(
                    gradientColors = listOf(Blue0, Blue1),
                    cornerRadius = 30.dp,
                    nameButton = "Next",
                    roundedCornerShape = RoundedCornerShape(30.dp)
                ) {
                    if (passwordOne == passwordTwo) {
                        navController?.navigate(
                            Navigation.VerificationCodeScreen.route + "/${passwordViewModel.email.value}" + "/${passwordOne.toString()}"
                        )
                    } else {
                        scope.launch {
                            snackBarHostState.showSnackbar("oops! the passwords don't match!")
                        }
                    }
                }

            }
        }
    }
}

//@Preview
//@Composable
//private fun Preview() {
//    narcisApplicationTheme {
//        PasswordSignUp(null, VerificationCodeViewModel(), AuthenticationViewModel())
//    }
//}