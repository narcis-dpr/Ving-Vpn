package com.abrnoc.application

import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceDataStore
import com.abrnoc.application.connection.database.GroupManager
import com.abrnoc.application.connection.database.ProfileManager
import com.abrnoc.application.presentation.connection.BaseService
import com.abrnoc.application.presentation.connection.DataStore
import com.abrnoc.application.presentation.connection.Key
import com.abrnoc.application.presentation.connection.OnPreferenceDataStoreChangeListener
import com.abrnoc.application.presentation.connection.SagerConnection
import com.abrnoc.application.presentation.connection.VpnRequestActivity
import com.abrnoc.application.presentation.connection.runOnDefaultDispatcher
import com.abrnoc.application.presentation.mainConnection.MainConnectionScreen
import com.abrnoc.application.presentation.navigation.Navigation
import com.abrnoc.application.presentation.screens.landing.Landing
import com.abrnoc.application.presentation.screens.landing.Welcome
import com.abrnoc.application.presentation.screens.signUp.EmailSignIn
import com.abrnoc.application.presentation.screens.signUp.EmailSignUp
import com.abrnoc.application.presentation.screens.signUp.PasswordSignUp
import com.abrnoc.application.presentation.screens.signUp.VerificationSignUp
import com.abrnoc.application.presentation.ui.theme.AbrnocApplicationTheme
import com.abrnoc.domain.auth.CheckSignedInUseCase
import com.abrnoc.domain.common.Result
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.nekohasekai.sagernet.aidl.ISagerNetService
import io.nekohasekai.sagernet.group.GroupInterfaceAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity :
    ComponentActivity(),
    SagerConnection.Callback,
    OnPreferenceDataStoreChangeListener {

    @Inject
    lateinit var checkSignedInUseCase: CheckSignedInUseCase

    val connection = SagerConnection(true)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val connect = registerForActivityResult(VpnRequestActivity.StartService()) {
            if (it) Toast.makeText(this, R.string.vpn_permission_denied, Toast.LENGTH_LONG).show()
        }
        changeState(BaseService.State.Idle)
        connection.connect(this, this)
        DataStore.configurationStore.registerChangeListener(this)
        GroupManager.userInterface = GroupInterfaceAdapter(this)

        if (intent?.action == Intent.ACTION_VIEW) {
            onNewIntent(intent)
        }
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
// //                        val intent = Intent(this@MainActivity, ConnActivity::class.java)
// //                        this@MainActivity.startActivity(intent)
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

        NavHost(
            navController = navController,
            startDestination = defaultRoute,
            builder = {
                composable(
                    Navigation.LandingScreen.route,
                    content = { Landing(navController = navController) }
                )
                composable(Navigation.WelcomeScreen.route, content = { Welcome(navController) })
                composable(
                    Navigation.EmailSignUpScreen.route,
                    content = { EmailSignUp(navController = navController) }
                )
                composable(
                    Navigation.PasswordScreen.route + "/{email}",
                    content = { PasswordSignUp(navController = navController) }
                )
                composable(
                    Navigation.VerificationCodeScreen.route + "/{email}" + "/{password}",
                    content = { VerificationSignUp(navController) }
                )
                composable(
                    Navigation.MainConnectionScreen.route,
                    content = {
                        MainConnectionScreen(
                            navControle = navController,
                            connect = connect
                        )
                    }
                )
                composable(
                    Navigation.EmailSignInScreen.route,
                    content = { EmailSignIn(navController = navController) }
                )
            }
        )
    }
    private fun changeState(
        state: BaseService.State,
        msg: String? = null,
        animate: Boolean = false,
    ) {
        DataStore.serviceState = state

        if (!DataStore.serviceState.connected) {
            statsUpdated(emptyList())
        }

//        binding.fab.changeState(state, DataStore.serviceState, animate)
        println("change state : $state and ${DataStore.serviceMode}")
//        binding.stats.changeState(state)
        if (msg != null) {
            // snackbar(getString(R.string.vpn_error, msg)).show()
            println(" %%% MainActivity $msg")
        }
        when (state) {
            BaseService.State.Stopped -> {
                runOnDefaultDispatcher {
                    // refresh view
                    ProfileManager.postUpdate(DataStore.currentProfile)
                }
            }

            else -> {}
        }
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state, msg, true)
    }

    override fun onServiceConnected(service: ISagerNetService) = changeState(
        try {
            BaseService.State.values()[service.state]
        } catch (_: RemoteException) {
            BaseService.State.Idle
        }
    )

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.SERVICE_MODE -> onBinderDied()
            Key.PROXY_APPS, Key.BYPASS_MODE, Key.INDIVIDUAL -> {
                if (DataStore.serviceState.canStop) {
//                    snackbar(getString(R.string.restart)).setAction(R.string.apply) {
//                        SagerNet.reloadService()
//                    }.show()
                }
            }
        }
    }
    fun snackbar(@StringRes resId: Int): Snackbar = snackbar("").setText(resId)
    fun snackbar(text: CharSequence): Snackbar = snackbarInternal(text).apply {
        view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
            maxLines = 10
        }
    }
    internal open fun snackbarInternal(text: CharSequence): Snackbar = throw NotImplementedError()
}
