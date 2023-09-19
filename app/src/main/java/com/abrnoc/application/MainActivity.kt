package com.abrnoc.application

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abrnoc.application.presentation.mainConnection.MainConnectionScreen
import com.abrnoc.application.presentation.navigation.Navigation
import com.abrnoc.application.presentation.screens.landing.Landing
import com.abrnoc.application.presentation.screens.landing.Welcome
import com.abrnoc.application.presentation.screens.signUp.EmailSignIn
import com.abrnoc.application.presentation.screens.signUp.EmailSignUp
import com.abrnoc.application.presentation.screens.signUp.PasswordSignUp
import com.abrnoc.application.presentation.screens.signUp.VerificationSignUp
import com.abrnoc.application.presentation.ui.theme.AbrnocApplicationTheme
import com.abrnoc.application.presentation.viewModel.StartService
import com.abrnoc.domain.auth.CheckSignedInUseCase
import com.abrnoc.domain.common.Result
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var checkSignedInUseCase: CheckSignedInUseCase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val connect = registerForActivityResult(StartService()) {}
        lifecycleScope.launch(Dispatchers.Main) {
            val result = checkSignedInUseCase(Unit)
            when (result) {
                is Result.Error -> {
                    Log.i("show", "shwo error")
                }

                Result.Loading -> {
                    Log.i("show", "show Loading")
                }

                is Result.Success -> {
//                    if (result.data) {
//                        LoginApplication(Navigation.LandingScreen.route)
////                        val intent = Intent(this@MainActivity, ConnActivity::class.java)
////                        this@MainActivity.startActivity(intent)
//                    } else {
                    setContent {
                        AbrnocApplicationTheme {
                            if (result.data) {
                                LoginApplication(defaultRoute = Navigation.MainConnectionScreen.route, connect)
                            } else {
                                LoginApplication(Navigation.LandingScreen.route, connect)
                            }
                        }
                    }
//                            }
                }
            }
        }
    }

    @Composable
    fun LoginApplication(defaultRoute: String, connect: ActivityResultLauncher<Void?>) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = defaultRoute,
            builder = {
                composable(
                    Navigation.LandingScreen.route,
                    content = { Landing(navController = navController) })
                composable(Navigation.WelcomeScreen.route, content = { Welcome(navController) })
                composable(
                    Navigation.EmailSignUpScreen.route,
                    content = { EmailSignUp(navController = navController) })
                composable(
                    Navigation.PasswordScreen.route + "/{email}",
                    content = { PasswordSignUp(navController = navController) })
                composable(
                    Navigation.VerificationCodeScreen.route + "/{email}" + "/{password}",
                    content = { VerificationSignUp(navController) })
                composable(
                    Navigation.MainConnectionScreen.route,
                    content = {
                        MainConnectionScreen(
                            navControle = navController,
                            connect = connect
                        )
                    })
                composable(
                    Navigation.EmailSignInScreen.route,
                    content = { EmailSignIn(navController = navController) })
            })
    }
}

